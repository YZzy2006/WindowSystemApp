package com.window.common;

public class KeywordUtil {

    /**
     * Escape SQL LIKE wildcards (% and _) in user input
     * so they are treated as literal characters.
     */
    public static String escapeLike(String keyword) {
        if (keyword == null || keyword.isEmpty()) return keyword;
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
