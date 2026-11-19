package com.omri.openinterminal;

public final class EventPolicy {
    public static final String MY_FILES = "com.sec.android.app.myfiles";

    private EventPolicy() {}

    public static boolean shouldHideOverlay(String eventPackage, String activePackage) {
        if (activePackage == null) return false;
        return !MY_FILES.equals(activePackage);
    }
}
