package com.window.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 安全的数学表达式求值器（递归下降解析器）
 * 支持: +, -, *, /, %, 括号, 小数, 一元负号
 * 不使用 ScriptEngine/eval，杜绝公式注入风险
 */
public final class SafeMathEvaluator {

    private final String expr;
    private int pos;

    private SafeMathEvaluator(String expr) {
        this.expr = expr;
        this.pos = 0;
    }

    /**
     * 解析并计算数学表达式
     * @param expression 只包含数字、运算符、括号、小数点、空格的表达式
     * @return 计算结果，保留4位小数
     * @throws IllegalArgumentException 表达式格式非法
     */
    public static BigDecimal evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("表达式不能为空");
        }
        // 中文输入法符号 → 英文（兼容历史公式里的中文括号/乘除号，避免误判为非法字符）
        expression = normalizeSymbols(expression);
        // 二次校验：只允许合法字符
        if (!expression.matches("[0-9+\\-*/%().\\s]+")) {
            throw new IllegalArgumentException("表达式包含非法字符: " + expression);
        }
        SafeMathEvaluator evaluator = new SafeMathEvaluator(expression.trim());
        BigDecimal result = evaluator.parseExpression();
        evaluator.skipWhitespace();
        if (evaluator.pos < evaluator.expr.length()) {
            throw new IllegalArgumentException("表达式末尾有多余字符: " + evaluator.expr.substring(evaluator.pos));
        }
        return result.setScale(4, RoundingMode.HALF_UP);
    }

    // 中文输入法符号 → 英文公式符号（供计算前统一转换）
    public static String normalizeSymbols(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '（' -> sb.append('(');
                case '）' -> sb.append(')');
                case '×' -> sb.append('*');
                case '÷' -> sb.append('/');
                case '＋' -> sb.append('+');
                case '－' -> sb.append('-');
                case '　' -> sb.append(' '); // 全角空格 → 半角
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    // expression = term (('+' | '-') term)*
    private BigDecimal parseExpression() {
        BigDecimal result = parseTerm();
        while (true) {
            skipWhitespace();
            if (peek() == '+') {
                advance();
                result = result.add(parseTerm());
            } else if (peek() == '-') {
                advance();
                result = result.subtract(parseTerm());
            } else {
                break;
            }
        }
        return result;
    }

    // term = factor (('*' | '/' | '%') factor)*
    private BigDecimal parseTerm() {
        BigDecimal result = parseFactor();
        while (true) {
            skipWhitespace();
            if (peek() == '*') {
                advance();
                result = result.multiply(parseFactor());
            } else if (peek() == '/') {
                advance();
                BigDecimal divisor = parseFactor();
                if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                    throw new IllegalArgumentException("除数不能为零");
                }
                result = result.divide(divisor, 10, RoundingMode.HALF_UP);
            } else if (peek() == '%') {
                advance();
                BigDecimal divisor = parseFactor();
                if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                    throw new IllegalArgumentException("模数不能为零");
                }
                result = result.remainder(divisor).setScale(10, RoundingMode.HALF_UP);
            } else {
                break;
            }
        }
        return result;
    }

    // factor = ('+' | '-')? atom
    private BigDecimal parseFactor() {
        skipWhitespace();
        if (peek() == '-') {
            advance();
            return parseAtom().negate();
        }
        if (peek() == '+') {
            advance();
        }
        return parseAtom();
    }

    // atom = number | '(' expression ')'
    private BigDecimal parseAtom() {
        skipWhitespace();
        char c = peek();
        if (c == '(') {
            advance(); // skip '('
            BigDecimal result = parseExpression();
            skipWhitespace();
            if (peek() != ')') {
                throw new IllegalArgumentException("缺少右括号");
            }
            advance(); // skip ')'
            return result;
        }
        if (Character.isDigit(c) || (c == '.' && pos + 1 < expr.length() && Character.isDigit(expr.charAt(pos + 1)))) {
            return parseNumber();
        }
        if (c == '\0') {
            throw new IllegalArgumentException("表达式意外结束");
        }
        throw new IllegalArgumentException("非法字符: " + c);
    }

    private BigDecimal parseNumber() {
        int start = pos;
        boolean hasDot = false;
        while (pos < expr.length()) {
            char c = expr.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' && !hasDot) {
                hasDot = true;
                pos++;
            } else {
                break;
            }
        }
        if (start == pos) {
            throw new IllegalArgumentException("期望数字");
        }
        return new BigDecimal(expr.substring(start, pos));
    }

    private void skipWhitespace() {
        while (pos < expr.length() && expr.charAt(pos) == ' ') {
            pos++;
        }
    }

    private char peek() {
        if (pos >= expr.length()) return '\0';
        return expr.charAt(pos);
    }

    private void advance() {
        pos++;
    }
}
