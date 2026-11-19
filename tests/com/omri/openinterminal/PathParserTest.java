package com.omri.openinterminal;

public final class PathParserTest {
    public static void main(String[] args) {
        expect("/storage/emulated/0/Download", PathParser.normalizeDisplayedPath("Internal storage/Download"));
        expect("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Backups",
                PathParser.normalizeDisplayedPath("/Internal storage/Android/media/com.whatsapp/WhatsApp/Backups"));
        expect("/storage/1234-5678/Docs", PathParser.normalizeDisplayedPath("SD card/Docs", "1234-5678"));
        expect("'a'\\''b'", PathParser.shellQuote("a'b"));
        expect(null, PathParser.normalizeDisplayedPath("Size 12 MB"));
        expect("/Internal storage/Android/media/com.whatsapp/WhatsApp/Backups",
                PathParser.extractDisplayedPath(java.util.Arrays.asList(
                        "Details", "Backups", "Size", "1.36 MB", "Path",
                        "/Internal storage/Android/media/", "com.whatsapp/WhatsApp/Backups", "OK")));
        expect("/Internal storage/Download",
                PathParser.extractDisplayedPath(java.util.Arrays.asList(
                        "Path", "/Internal storage/Download", "12 MB", "Modified", "Today", "OK")));
        expect("/Internal storage/Download/",
                PathParser.extractDisplayedPath(java.util.Arrays.asList(
                        "Path", "/Internal storage/Download/", "Size", "12 MB", "OK")));
        expect("/Internal storage/",
                PathParser.extractDisplayedPath(java.util.Arrays.asList(
                        "Path", "/Internal storage/", "Contains", "15 folders", "OK")));
        System.out.println("PathParserTest: PASS");
    }

    private static void expect(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
