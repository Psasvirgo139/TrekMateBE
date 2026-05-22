package com.trekmate.backend.utils;

public final class StringUtils {

    private StringUtils() {}

    public static String slugify(String text) {
        if (text == null) return null;
        return text.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    public static boolean isNullOrBlank(String text) {
        return text == null || text.isBlank();
    }

    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
