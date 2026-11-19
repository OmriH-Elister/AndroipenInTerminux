package com.omri.openinterminal;

public final class PathParser {
    private PathParser() {}

    public static String normalizeDisplayedPath(String displayed) {
        return normalizeDisplayedPath(displayed, null);
    }

    public static String normalizeDisplayedPath(String displayed, String sdCardId) {
        if (displayed == null) return null;
        String value = displayed.trim().replace('\n', '/');
        while (value.contains("//")) value = value.replace("//", "/");
        if (value.startsWith("/Internal storage/")) {
            return "/storage/emulated/0/" + value.substring("/Internal storage/".length());
        }
        if (value.equals("/Internal storage") || value.equals("Internal storage")) {
            return "/storage/emulated/0";
        }
        if (value.startsWith("Internal storage/")) {
            return "/storage/emulated/0/" + value.substring("Internal storage/".length());
        }
        if (sdCardId != null && !sdCardId.isEmpty()) {
            if (value.startsWith("/SD card/")) {
                return "/storage/" + sdCardId + "/" + value.substring("/SD card/".length());
            }
            if (value.startsWith("SD card/")) {
                return "/storage/" + sdCardId + "/" + value.substring("SD card/".length());
            }
            if (value.equals("/SD card") || value.equals("SD card")) {
                return "/storage/" + sdCardId;
            }
        }
        return null;
    }

    public static String shellQuote(String value) {
        if (value == null) return "''";
        return "'" + value.replace("'", "'\\''") + "'";
    }

    public static String extractDisplayedPath(java.util.List<String> values) {
        if (values == null) return null;
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i) == null ? "" : values.get(i).trim();
            if (value.startsWith("/Internal storage") || value.startsWith("Internal storage")
                    || value.startsWith("/SD card") || value.startsWith("SD card")) {
                StringBuilder path = new StringBuilder(value.startsWith("/") ? value : "/" + value);
                if (path.toString().endsWith("/") && i + 1 < values.size()) {
                    String continuation = values.get(i + 1) == null ? "" : values.get(i + 1).trim();
                    boolean metadataLabel = continuation.equalsIgnoreCase("Size")
                            || continuation.equalsIgnoreCase("Contains")
                            || continuation.equalsIgnoreCase("Modified")
                            || continuation.equalsIgnoreCase("Type")
                            || continuation.equalsIgnoreCase("Path")
                            || continuation.equalsIgnoreCase("OK");
                    boolean looksLikePath = !metadataLabel
                            && (continuation.contains("/") || continuation.matches("[A-Za-z0-9._-]+"));
                    if (looksLikePath) path.append(continuation);
                }
                return path.toString();
            }
        }
        return null;
    }
}
