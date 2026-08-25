package com.window.common;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 敏感词/脏话/广告/诈骗内容过滤器
 * 多层检测：原始匹配 → 归一化匹配 → 谐音检测 → 模式匹配
 */
public class SensitiveWordFilter {

    // ==================== 第一层：URL/邮箱/电话检测 ====================
    private static final Pattern URL_PATTERN = Pattern.compile(
        "https?://|www\\d*\\.|\\.(com|cn|net|top|xyz|cc|vip|org|io|info|biz|pro|site|club|online|store|shop|xin|wang|ren|vip|link|click|help|love|icu|cyou|cfd|gdn|sbs)"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile("@\\w+\\.");
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern QQ_PATTERN = Pattern.compile("[qQ]{1,2}[^a-zA-Z]?\\d{5,11}");
    private static final Pattern WECHAT_PATTERN = Pattern.compile(
        "[vVwW]\\s*[xX]\\s*[:：]?\\s*[a-zA-Z0-9_\\-]{3,20}"
    );

    // ==================== 第二层：脏话攻击词库（分类） ====================

    // 性相关/下流词汇
    private static final String[] SEXUAL = {
        "操","操你","我操","操你妈","操蛋","操比","操逼","操尼玛","操尼妈","被操","求操","想操",
        "草泥马","草拟吗","草拟马","草你妈","草泥马戈壁","草尼玛",
        "肏","肏你","肏尼玛","肏蛋",
        "日你","日尼","日尼玛","日你妈","日你全家","狗日的","狗日",
        "艹","我艹","艹你","艹你妈","艹尼玛",
        "干你","干你妈","干你娘","干尼玛",
        "bitch","b1tch","b!tch","b.i.t.c.h","btch","biatch","biotch",
        "fuck","fuk","fck","fuuck","fucck","f.u.c.k","f u c k","fúck","fück",
        "shit","sh1t","sh!t","sh*t","sht","shite","shít",
        "damn","damm","dam","d4mn","d@mn",
        "dick","d1ck","d!ck","d*k","dck",
        "cock","c0ck","c*ck","cok",
        "pussy","p*ssy","puss","puzzy",
        "asshole","assh0le","arsehole",
        "bastard","b*stard","b4stard",
        "whore","wh0re","wh*re",
        "slut","sl*t","sl4t",
        "cunt","c*nt","c4nt",
        "motherfucker","mtherfcker","mofo","mf",
        "wanker","wnker",
        "douche","d0uche",
        "dildo","d1ldo",
        "nigger","n1gger","nigg","n1gg",
        "retard","ret*rd","r3tard",
    };

    // 辱骂/人格侮辱
    private static final String[] INSULTS = {
        "傻逼","傻b","傻B","傻逼玩意","傻逼东西","傻逼玩意儿",
        "煞笔","傻杯","傻叉","傻比","傻批","傻缺","傻屌","傻吊","傻狗","傻X","傻x",
        "弱智","智障","脑残","脑瘫","脑残片","脑子有病","脑子进水","脑子有坑",
        "白痴","蠢货","蠢猪","蠢驴","蠢材","愚笨",
        "废物","垃圾","垃圾人","垃圾玩意","垃圾东西",
        "贱人","贱货","贱B","贱b","贱逼","贱婢","贱种","贱骨头",
        "婊子","婊砸","婊子养的","臭婊子","死婊子",
        "狗逼","狗比","狗币","狗东西","狗日的","狗娘养的","狗杂种","狗屎",
        "猪猡","猪头","蠢猪","死猪",
        "畜生","禽兽","牲口","杂种","野种",
        "烂人","烂货","烂逼","烂比","烂B",
        "死全家","死一户口本","死妈","死爹","死娘","死爸",
        "龟孙","龟儿子","龟孙子","王八蛋","王八羔子",
        "cnm","cnmb","cnmdb","cnmlgb","cnmgb",
        "tmd","tmdb","tmlgb",
        "nmsl","nmb","nm$l","nmslwsnd",
        "wqnmlgb","wqnmb","wcnm","wcnmlgb","wqnmlgbd",
        "rnmb","rnm","rnmd","rnmlgb",
        "mlgb","mlgbd","mlgb的",
        "wsnd","wdnmd","nmd","nm$l",
        "csb","csnm","csndy",
        "sb","s b","s.b","s-b","s_b","$b","sB","Sb",
        "2b","2B","2 b","2-b",
        "nb","nmb",
    };

    // 下流/色情相关
    private static final String[] LEWD = {
        "约炮","约p","约P","约pao","打炮","打P","炮友","py",
        "嫖娼","嫖","嫖客","招嫖","卖淫","卖y","卖Y",
        "包养","求包养","被包养","求包","找金主",
        "大保健","全套","半套","莞式","水疗会所","丝足",
        "看片","簧片","a片","A片","av","AV",
        "裸聊","裸体","裸照","luo聊","luo照","luo体",
        "黄色","黄片","黄网","黄色网站","h网","H网",
        "色情","色q","seqing","seq",
        "激情视频","激情聊天","激情裸聊",
        "一夜情","一夜q","ons","O N S",
        "援交","援助交际","yuanjiao",
        "成人","成人电影","成人用品","成人网站",
        "偷拍","偷窥","偷pai",
        "性交","性爱","性虐","性虐待","性奴",
        "乱伦","乱lun","luanlun",
        "迷奸","迷j","迷jian",
        "做爱","做a","做A","zuoai","zuo爱",
        "射精","射j","shejing",
        "自慰","自w","ziwei","打飞机","打fj",
        "乳交","口交","kj","KJ",
        "巨乳","大奶","大波","巨ru",
        "强奸","强j","强jian",
        "轮奸","轮j","轮jian",
        "猥亵","猥xie","weixie",
        "小视频","小v","福利视频","福利姬",
        "萝莉","luoli","loli","幼女","幼齿",
        "SM","sm","S M","s m",
    };

    // 赌博相关
    private static final String[] GAMBLING = {
        "赌博","赌场","赌钱","赌球","赌马","赌狗",
        "博彩","菠菜","bc","B C","B.C",
        "彩票","时时彩","六合彩","时时c","11选5","快三","北京赛车",
        "百家乐","21点","二十一点","德州扑克","炸金花","牛牛","斗牛",
        "老虎机","水果机","捕鱼","打鱼","捕鱼游戏",
        "下注","投注","压注","加注","梭哈","all in",
        "庄家","闲家","赌注","赌资","赌金","赌本",
        "赌神","赌王","赌圣","千术","出千",
        "菲律宾","缅甸","澳门赌场","皇冠","永利","金沙","威尼斯人",
        "棋牌","电玩城","娱乐城","赌城",
        "现金网","信用网","代理线","推广线","佣j",
        "真人视讯","真人娱乐","真人荷官","真人发牌",
        "彩金","彩池","头奖","jackpot",
    };

    // 诈骗/代办/违法
    private static final String[] SCAM = {
        "代办","代办证件","代办学历","代办签证","代办贷款",
        "刻章","刻公章","办证","办假证","假证","做证",
        "发票","开发票","代开发票","增值税发票",
        "信用卡","办卡","套现","养卡","提额","花呗","白条",
        "贷款","网贷","借贷","放款","小额贷","无抵押","秒到账","黑户",
        "刷单","刷信誉","刷钻","刷信用","刷量","刷粉","刷流量","刷榜",
        "传销","拉人头","发展下线","金字塔","庞氏",
        "资金盘","互助盘","拆分盘","分红盘","原始股",
        "高利贷","高炮","砍头息","714高炮",
        "中奖","中大奖","恭喜中奖","幸运用户","幸运儿","特等奖",
        "免费领取","免费送","免费拿","免费获取","免费赠送","0元购",
        "加微信","加我微信","加v信","加V","加我V","加我v",
        "扫码","扫一扫","扫码加","扫码领取","扫码进群",
        "加群","进群","拉群","入群","qq群","Q群","Q Q群",
        "点击领取","点击查看","点击进入","点击打开","点击链接",
        "兼职","日结","日赚","在家赚钱","手机赚钱","学生兼职","宝妈兼职",
        "薅羊毛","撸羊毛","撸平台","漏洞单","薅毛",
        "洗钱","洗黑钱","跑分","跑分平台","代收代付",
        "网赌","网投","网上投注","在线投注",
    };

    // 政治敏感（仅做初始化，实际由各平台政策决定）
    private static final String[] POLITICAL = {
        "反党","反共","反华","反中","反政府",
        "台独","港独","藏独","疆独","东突",
        "法轮功","法轮","falun","Falun",
        "六四","天安门事件","坦克人",
        "颠覆国家","推翻政府","颜色革命",
    };

    // 广告营销骚扰
    private static final String[] ADS = {
        "加我qq","加我QQ","加扣扣","扣扣号","qq号",
        "加好友","添加好友","加我好友",
        "微商","代理","招代理","招加盟","诚招代理",
        "减肥","瘦身","丰胸","壮阳","增粗","延时",
        "股票","炒股","荐股","牛股","涨停","内幕消息","老师带单",
        "比特币","虚拟币","数字货币","挖矿","矿机","区块链","token","空投",
        "外汇","黄金","期货","原油","现货",
        "课程","培训","考证","保过","包过",
        "包治","根治","秘方","祖传","特效药","特效",
    };

    // ==================== 第三层：谐音/变体映射 ====================
    // 拼音谐音 → 原词
    private static final String[][] HOMOPHONES = {
        {"cao","操"},{"cào","操"},{"kao","靠"},{"kào","靠"},
        {"ri","日"},{"rì","日"},
        {"gan","干"},{"gàn","干"},{"gān","干"},
        {"diao","屌"},{"diǎo","屌"},{"diao","吊"},
        {"bi","逼"},{"bī","逼"},{"bi","比"},{"bi","B"},{"bi","b"},
        {"sha","傻"},{"shǎ","傻"},
        {"biao","婊"},{"biǎo","婊"},
        {"jian","贱"},{"jiàn","贱"},{"jian","奸"},
        {"ma","妈"},{"mā","妈"},
        {"ji","鸡"},{"jī","鸡"},
        {"gou","狗"},{"gǒu","狗"},
        {"zhu","猪"},{"zhū","猪"},
        {"si","死"},{"sǐ","死"},
        {"caobi","操逼"},{"caob","操逼"},
        {"shabi","傻逼"},{"shab","傻逼"},{"sabi","傻逼"},
        {"jianbi","贱逼"},{"jianb","贱逼"},
        {"gouri","狗日"},{"gour","狗日"},
        {"tama","他妈"},{"tam","他妈"},
        {"nima","尼玛"},{"nim","尼玛"},
        {"wocao","我操"},{"woc","我操"},
        {"nimabi","尼玛逼"},{"nimab","尼玛逼"},
        {"woqu","我去"},
        {"kaonima","靠尼玛"},
        {"fuckyou","fuck you"},
        {"matherfucker","motherfucker"},
        {"fking","fucking"},{"fcking","fucking"},{"f*cking","fucking"},
        {"sh1t","shit"},{"$hit","shit"},{"sh!t","shit"},
        {"d4mn","damn"},{"d@mn","damn"},
        {"b1tch","bitch"},{"b!tch","bitch"},
        {"@ss","ass"},{"a$$","ass"},{"@sshole","asshole"},
    };

    // Unicode 形近字（homoglyphs）→ 标准字符
    private static final String[][] HOMOGLYPHS = {
        {"а","a"},{"е","e"},{"о","o"},{"р","p"},{"с","c"},{"у","y"},{"х","x"},
        {"і","i"},{"ɑ","a"},{"ο","o"},{"е","e"},
        {"Α","A"},{"Β","B"},{"Ε","E"},{"Η","H"},{"Ι","I"},{"Κ","K"},
        {"Μ","M"},{"Ν","N"},{"Ο","O"},{"Ρ","P"},{"Τ","T"},{"Υ","Y"},{"Χ","X"},{"Ζ","Z"},
        {"А","A"},{"В","B"},{"Е","E"},{"Н","H"},{"К","K"},{"М","M"},{"О","O"},
        {"Р","P"},{"С","C"},{"Т","T"},{"Х","X"},{"У","Y"},
        {"₳","A"},{"€","E"},{"!","i"},{"¡","i"},{"$","S"},{"₴","S"},
        {"₵","C"},{"₲","G"},{"₭","K"},{"₦","N"},{"₱","P"},{"₮","T"},{"₩","W"},
        {"０","0"},{"１","1"},{"２","2"},{"３","3"},{"４","4"},
        {"５","5"},{"６","6"},{"７","7"},{"８","8"},{"９","9"},
        {"⓪","0"},{"①","1"},{"②","2"},{"③","3"},{"④","4"},{"⑤","5"},
        {"⑥","6"},{"⑦","7"},{"⑧","8"},{"⑨","9"},
        {"ⓐ","a"},{"ⓑ","b"},{"ⓒ","c"},{"ⓓ","d"},{"ⓔ","e"},{"ⓕ","f"},
        {"ⓖ","g"},{"ⓗ","h"},{"ⓘ","i"},{"ⓙ","j"},{"ⓚ","k"},{"ⓛ","l"},
        {"ⓜ","m"},{"ⓝ","n"},{"ⓞ","o"},{"ⓟ","p"},{"ⓠ","q"},{"ⓡ","r"},
        {"ⓢ","s"},{"ⓣ","t"},{"ⓤ","u"},{"ⓥ","v"},{"ⓦ","w"},{"ⓧ","x"},{"ⓨ","y"},{"ⓩ","z"},
        {"Ⓐ","A"},{"Ⓑ","B"},{"Ⓒ","C"},{"Ⓓ","D"},{"Ⓔ","E"},{"Ⓕ","F"},
        {"Ⓖ","G"},{"Ⓗ","H"},{"Ⓘ","I"},{"Ⓙ","J"},{"Ⓚ","K"},{"Ⓛ","L"},
        {"Ⓜ","M"},{"Ⓝ","N"},{"Ⓞ","O"},{"Ⓟ","P"},{"Ⓠ","Q"},{"Ⓡ","R"},
        {"Ⓢ","S"},{"Ⓣ","T"},{"Ⓤ","U"},{"Ⓥ","V"},{"Ⓦ","W"},{"Ⓧ","X"},{"Ⓨ","Y"},{"Ⓩ","Z"},
    };

    // ==================== 第四层：繁体→简体（扩展版） ====================
    private static final String[][] TRAD_TO_SIMP = {
        {"幹","干"},{"乾","干"},{"媽","妈"},{"嗎","吗"},{"們","们"},{"門","门"},
        {"麼","么"},{"說","说"},{"來","来"},{"對","对"},{"時","时"},{"會","会"},
        {"個","个"},{"為","为"},{"國","国"},{"這","这"},{"發","发"},{"後","后"},
        {"開","开"},{"關","关"},{"長","长"},{"兒","儿"},{"頭","头"},{"無","无"},
        {"體","体"},{"機","机"},{"氣","气"},{"愛","爱"},{"電","电"},{"學","学"},
        {"點","点"},{"當","当"},{"過","过"},{"實","实"},{"現","现"},{"書","书"},
        {"馬","马"},{"鳥","鸟"},{"魚","鱼"},{"龍","龙"},{"龜","龟"},
        {"見","见"},{"貝","贝"},{"車","车"},{"東","东"},{"樂","乐"},{"買","买"},
        {"賣","卖"},{"華","华"},{"萬","万"},{"與","与"},{"興","兴"},{"義","义"},
        {"親","亲"},{"話","话"},{"語","语"},{"讓","让"},{"講","讲"},{"認","认"},
        {"識","识"},{"請","请"},{"誰","谁"},{"謝","谢"},{"該","该"},{"談","谈"},
        {"論","论"},{"調","调"},{"變","变"},{"護","护"},{"處","处"},{"務","务"},
        {"業","业"},{"飛","飞"},{"風","风"},{"區","区"},{"醫","医"},{"嚴","严"},
        {"廠","厂"},{"廣","广"},{"歷","历"},{"參","参"},{"觀","观"},{"難","难"},
        {"證","证"},{"錢","钱"},{"銀","银"},{"鐵","铁"},{"陽","阳"},{"陰","阴"},
        {"雙","双"},{"從","从"},{"眾","众"},{"導","导"},{"經","经"},{"織","织"},
        {"統","统"},{"計","计"},{"設","设"},{"許","许"},{"報","报"},{"場","场"},
        {"塊","块"},{"歲","岁"},{"島","岛"},{"師","师"},{"帶","带"},{"幫","帮"},
        {"張","张"},{"強","强"},{"彈","弹"},{"復","复"},{"術","术"},{"號","号"},
        {"裝","装"},{"裡","里"},{"製","制"},{"複","复"},{"線","线"},{"組","组"},
        {"結","结"},{"給","给"},{"絕","绝"},{"統","统"},{"絲","丝"},{"維","维"},
    };

    /**
     * 主检测入口。返回错误提示字符串，返回 null 表示通过。
     */
    public static String check(String text) {
        if (text == null || text.isBlank()) return null;

        // 预处理：去空格/换行/全角空格
        String raw = text.toLowerCase()
            .replaceAll("[\\s　\\r\\n\\t]", "");

        if (raw.isEmpty()) return null;

        // ---------- 第1层：结构化检测 ----------
        String r1 = checkStructural(raw);
        if (r1 != null) return r1;

        // ---------- 第2层：原始词库精确匹配 ----------
        String[][] allWordLists = {SEXUAL, INSULTS, LEWD, GAMBLING, SCAM, POLITICAL, ADS};
        String[] blockMessages = {
            "请文明交流", "请文明交流", "请勿发布违规内容",
            "请勿发布赌博相关信息", "请勿发布违法广告信息",
            "请遵守法律法规", "请勿发布广告信息"
        };

        for (int i = 0; i < allWordLists.length; i++) {
            for (String w : allWordLists[i]) {
                if (raw.contains(w)) return blockMessages[i];
            }
        }

        // ---------- 第3层：归一化后匹配 ----------
        String norm = normalize(raw);
        for (int i = 0; i < allWordLists.length; i++) {
            for (String w : allWordLists[i]) {
                if (norm.contains(normalize(w))) return blockMessages[i];
            }
        }

        // ---------- 第4层：谐音/变体检测 ----------
        String letters = norm.replaceAll("[^a-z]", "");
        for (String[] pair : HOMOPHONES) {
            if (letters.contains(pair[0].toLowerCase()) && norm.contains(pair[1])) {
                return "请文明交流";
            }
        }

        // ---------- 第5层：变形攻击检测 ----------
        // 检测常见拆分/间隔写法
        String r5 = checkObfuscated(norm);
        if (r5 != null) return r5;

        return null;
    }

    // ==================== 结构化检测 ====================
    private static String checkStructural(String raw) {
        if (URL_PATTERN.matcher(raw).find()) return "请勿包含网址链接";
        if (EMAIL_PATTERN.matcher(raw).find()) return "请勿包含邮箱地址";
        if (PHONE_PATTERN.matcher(raw).find()) return "请勿包含手机号码";
        if (QQ_PATTERN.matcher(raw).find()) return "请勿包含QQ号";
        if (WECHAT_PATTERN.matcher(raw).find()) return "请勿包含微信号";

        // 长数字串 (如银行卡号、身份证等)
        if (raw.replaceAll("[^\\d]", "").length() >= 13) return "请勿包含长数字串";

        return null;
    }

    // ==================== 文本归一化 ====================
    private static String normalize(String text) {
        String result = text.toLowerCase();

        // 繁→简
        for (String[] pair : TRAD_TO_SIMP) {
            result = result.replace(pair[0], pair[1]);
        }

        // 全角→半角
        result = result.replace('！', '!').replace('＂', '"')
            .replace('＃', '#').replace('＄', '$')
            .replace('％', '%').replace('＆', '&')
            .replace('＇', '\'').replace('（', '(')
            .replace('）', ')').replace('＊', '*')
            .replace('＋', '+').replace('，', ',')
            .replace('－', '-').replace('．', '.')
            .replace('／', '/')
            .replace('０', '0').replace('１', '1')
            .replace('２', '2').replace('３', '3')
            .replace('４', '4').replace('５', '5')
            .replace('６', '6').replace('７', '7')
            .replace('８', '8').replace('９', '9')
            .replace('：', ':').replace('；', ';')
            .replace('＜', '<').replace('＝', '=')
            .replace('＞', '>').replace('？', '?')
            .replace('＠', '@')
            .replace('Ａ', 'A').replace('Ｂ', 'B')
            .replace('Ｃ', 'C').replace('Ｄ', 'D')
            .replace('Ｅ', 'E').replace('Ｆ', 'F')
            .replace('Ｇ', 'G').replace('Ｈ', 'H')
            .replace('Ｉ', 'I').replace('Ｊ', 'J')
            .replace('Ｋ', 'K').replace('Ｌ', 'L')
            .replace('Ｍ', 'M').replace('Ｎ', 'N')
            .replace('Ｏ', 'O').replace('Ｐ', 'P')
            .replace('Ｑ', 'Q').replace('Ｒ', 'R')
            .replace('Ｓ', 'S').replace('Ｔ', 'T')
            .replace('Ｕ', 'U').replace('Ｖ', 'V')
            .replace('Ｗ', 'W').replace('Ｘ', 'X')
            .replace('Ｙ', 'Y').replace('Ｚ', 'Z')
            .replace('ａ', 'a').replace('ｂ', 'b')
            .replace('ｃ', 'c').replace('ｄ', 'd')
            .replace('ｅ', 'e').replace('ｆ', 'f')
            .replace('ｇ', 'g').replace('ｈ', 'h')
            .replace('ｉ', 'i').replace('ｊ', 'j')
            .replace('ｋ', 'k').replace('ｌ', 'l')
            .replace('ｍ', 'm').replace('ｎ', 'n')
            .replace('ｏ', 'o').replace('ｐ', 'p')
            .replace('ｑ', 'q').replace('ｒ', 'r')
            .replace('ｓ', 's').replace('ｔ', 't')
            .replace('ｕ', 'u').replace('ｖ', 'v')
            .replace('ｗ', 'w').replace('ｘ', 'x')
            .replace('ｙ', 'y').replace('ｚ', 'z');

        // Unicode 形近字映射
        for (String[] pair : HOMOGLYPHS) {
            result = result.replace(pair[0], pair[1]);
        }

        // 去除所有标点/特殊字符/空格
        String stripped = result.replaceAll(
            "[\\s　\\r\\n\\t.,;:!?()（）【】\\[\\]{}《》<>\"'`~@#$%^&*\\-+=_/\\\\|·…×※﹒‧・．｡､，。；：！？＠＃＄％︿＆＊－＋＝＿／＼｜～￣′″‵′″‵︵︶︷︸︹︺︻︼︽︾︿﹀﹁﹂﹃﹄]", ""
        );

        // 重复字符压缩 (中文字符重复3次以上 → 2次)
        stripped = stripped.replaceAll("([\\u4e00-\\u9fff])\\1{2,}", "$1$1");

        return stripped;
    }

    // ==================== 变形攻击检测 ====================
    private static String checkObfuscated(String norm) {
        // 检测用标点分割的敏感词（如 "傻.逼" "f u c k" "傻   逼"）
        // 已在归一化时去除标点和空格，此方法检查归一化后是否匹配关键模式

        // 高危害即时拦截词（即使归一化后也容易被绕过，在此加强）
        String[] criticalPatterns = {
            "caonima","caonm","caoni","caowo","caosini","caosin",
            "gannima","gannm","ganni","ganniniang",
            "rinima","rinm","rinixianren","rini",
            "diaonm","diaoni",
            "shabi","shab","sabi","shadiao","shagou",
            "jianbi","jianb","jianren","jianhuo",
            "biaozi","biaoz","biaoziyangde",
            "gouri","gouride","goudongxi","gouniangyangde",
            "tamade","tamad","tmd","tmde",
            "nimabi","nimab","nimade","nimalegb",
            "wocaonima","wocaonm","wocao","wocaon",
            "nmsl","nm$l","nmslwsnd","wdnmd",
            "fuckyou","fckyou","fkyou","fku",
            "motherfucker","mtherfcker",
            "sonofbitch","sonofabitch","sob",
            "goddamn","goddam","gddmn",
            "fuckoff","fckoff","fkoff",
            "suckmy","sckmy","sukmy",
            "eatmy","eatmyshorts",
            "pieceofshit","pieceofsht","pos",
            "killurself","killyourself","kys","kms",
        };

        for (String p : criticalPatterns) {
            if (norm.contains(p)) return "请文明交流";
        }

        // 检测极端重复字符 (用相同字符刷屏)
        if (norm.matches(".*(.)\\1{9,}.*")) return "请勿重复发送相同内容";
        if (norm.length() > 0 && norm.replaceAll("[^a-z]", "").length() > 200)
            return "请勿发送过长的乱码内容";

        return null;
    }

    // ==================== 获取所有词条数量（用于统计） ====================
    public static int getTotalWordCount() {
        int count = 0;
        for (String[] list : new String[][]{SEXUAL, INSULTS, LEWD, GAMBLING, SCAM, POLITICAL, ADS}) {
            count += list.length;
        }
        count += HOMOPHONES.length;
        count += HOMOGLYPHS.length;
        count += TRAD_TO_SIMP.length;
        return count;
    }
}
