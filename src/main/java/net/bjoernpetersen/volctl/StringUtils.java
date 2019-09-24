package net.bjoernpetersen.volctl;

import org.jetbrains.annotations.NotNull;

final class StringUtils {
    private StringUtils() {
    }

    @NotNull
    static String substringBeforeLast(@NotNull String s, char chr) {
        int index = s.lastIndexOf(chr);
        if (index < 0) return s;
        return s.substring(0, index);
    }

    @NotNull
    static String substringAfterLast(@NotNull String s, char chr) {
        int index = s.lastIndexOf(chr);
        if (index < 0) return s;
        return s.substring(index + 1);
    }
}
