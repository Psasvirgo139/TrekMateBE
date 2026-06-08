package com.trekmate.backend.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class StringUtils {

    private StringUtils() {}

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static String slugify(String text) {
        if (text == null) return null;
        String temp = Normalizer.normalize(text, Normalizer.Form.NFD);
        String deaccented = DIACRITICS.matcher(temp).replaceAll("")
                .toLowerCase()
                .replace("đ", "d")
                .replace("Đ", "d");
        return deaccented.trim()
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
