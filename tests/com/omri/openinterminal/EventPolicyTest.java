package com.omri.openinterminal;

public final class EventPolicyTest {
    public static void main(String[] args) {
        expect(false, EventPolicy.shouldHideOverlay("com.android.systemui", "com.sec.android.app.myfiles"));
        expect(false, EventPolicy.shouldHideOverlay("com.sec.android.app.myfiles", "com.sec.android.app.myfiles"));
        expect(true, EventPolicy.shouldHideOverlay("com.sec.android.app.myfiles", "com.android.chrome"));
        expect(true, EventPolicy.shouldHideOverlay("com.android.chrome", "com.android.chrome"));
        expect(true, EventPolicy.shouldHideOverlay(null, "com.android.settings"));
        expect(false, EventPolicy.shouldHideOverlay("com.android.systemui", null));
        System.out.println("EventPolicyTest: PASS");
    }

    private static void expect(boolean expected, boolean actual) {
        if (expected != actual) throw new AssertionError("expected=" + expected + " actual=" + actual);
    }
}
