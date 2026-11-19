package com.omri.openinterminal;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MyFilesAccessibilityService extends AccessibilityService {
    private static final String MY_FILES = "com.sec.android.app.myfiles";
    private static final String TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND";
    private static final int IDLE = 0;
    private static final int WAIT_DETAILS_MENU = 1;
    private static final int WAIT_DETAILS_DIALOG = 2;

    private WindowManager windowManager;
    private Button overlayButton;
    private boolean overlayVisible;
    private int state = IDLE;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override protected void onServiceConnected() {
        File externalDir = getExternalFilesDir(null);
        if (externalDir != null) new File(externalDir, "events.log").delete();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayButton = new Button(this);
        overlayButton.setText(">_  Open in terminal");
        overlayButton.setTextSize(15);
        overlayButton.setTextColor(Color.WHITE);
        overlayButton.setAllCaps(false);
        overlayButton.setBackgroundColor(Color.rgb(55, 55, 65));
        overlayButton.setOnClickListener(v -> beginOpen());
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        String eventPackage = event.getPackageName() == null ? null : event.getPackageName().toString();
        String activePackage = root == null || root.getPackageName() == null ? null : root.getPackageName().toString();
        if (EventPolicy.shouldHideOverlay(eventPackage, activePackage)) {
            resetAttempt();
            hideOverlay();
            return;
        }
        if (!MY_FILES.equals(eventPackage)) return;
        if (root == null) return;

        if (state == WAIT_DETAILS_MENU) {
            AccessibilityNodeInfo details = findByText(root, "Details");
            if (details != null && clickNode(details)) {
                state = WAIT_DETAILS_DIALOG;
                return;
            }
        } else if (state == WAIT_DETAILS_DIALOG) {
            List<String> strings = new ArrayList<>();
            collectStrings(root, strings);
            boolean isDetailsDialog = containsIgnoreCase(strings, "Details")
                    && containsIgnoreCase(strings, "Path")
                    && containsIgnoreCase(strings, "OK");
            if (!isDetailsDialog) return;
            String displayed = PathParser.extractDisplayedPath(strings);
            if (displayed != null) {
                boolean folder = containsIgnoreCase(strings, "Contains");
                String path = PathParser.normalizeDisplayedPath(displayed, findSdCardId());
                AccessibilityNodeInfo ok = findByText(root, "OK");
                if (ok != null) clickNode(ok);
                resetAttempt();
                hideOverlay();
                if (path == null) {
                    toast("This storage location is not supported yet");
                } else if (!folder) {
                    toast("Select a folder, not a file");
                } else {
                    openTermux(path);
                }
                return;
            }
        }

        if (state == IDLE && hasExactlyOneSelection(root)) showOverlay();
        else if (state == IDLE) hideOverlay();
    }

    @Override public void onInterrupt() {
        resetAttempt();
        hideOverlay();
    }

    @Override public void onDestroy() {
        resetAttempt();
        hideOverlay();
        super.onDestroy();
    }

    private void beginOpen() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null || !MY_FILES.contentEquals(root.getPackageName())) {
            toast("My Files is not ready");
            resetAttempt();
            hideOverlay();
            return;
        }
        AccessibilityNodeInfo more = findByDescription(root, "More");
        if (more == null) more = findByText(root, "More");
        if (more == null || !clickNode(more)) {
            toast("Could not open the My Files menu");
            return;
        }
        state = WAIT_DETAILS_MENU;
        cancelTimeout();
        timeoutRunnable = () -> {
            if (state != IDLE) {
                resetAttempt();
                hideOverlay();
                toast("Could not read the selected folder path");
            }
        };
        handler.postDelayed(timeoutRunnable, 5000);
    }

    private void openTermux(String path) {
        if (checkSelfPermission(TERMUX_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            toast("Open the Open in Terminal app and allow Termux command access");
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        Intent intent = new Intent("com.termux.RUN_COMMAND");
        intent.setClassName("com.termux", "com.termux.app.RunCommandService");
        intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-l"});
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", path);
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        try {
            startService(intent);
            handler.postDelayed(() -> {
                Intent terminal = new Intent();
                terminal.setClassName("com.termux", "com.termux.app.TermuxActivity");
                terminal.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                try {
                    startActivity(terminal);
                } catch (Exception error) {
                    toast("Terminal session started; open Termux from its notification");
                }
            }, 250);
        } catch (Exception error) {
            toast("Termux could not open this folder: " + error.getClass().getSimpleName());
        }
    }

    private boolean hasExactlyOneSelection(AccessibilityNodeInfo root) {
        List<String> strings = new ArrayList<>();
        collectStrings(root, strings);
        for (String value : strings) {
            String lower = value.toLowerCase(Locale.ROOT).trim();
            if (lower.equals("1 selected") || lower.equals("selected 1")) return true;
        }
        return false;
    }

    private void showOverlay() {
        if (overlayVisible || overlayButton == null || windowManager == null) return;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                dp(52),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.y = dp(88);
        try {
            windowManager.addView(overlayButton, lp);
            overlayVisible = true;
        } catch (RuntimeException error) {
            overlayVisible = false;
            toast("Open in Terminal overlay failed: " + error.getClass().getSimpleName());
        }
    }

    private void hideOverlay() {
        if (!overlayVisible || windowManager == null || overlayButton == null) return;
        try { windowManager.removeView(overlayButton); } catch (Exception ignored) {}
        overlayVisible = false;
    }

    private AccessibilityNodeInfo findByText(AccessibilityNodeInfo root, String wanted) {
        List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByText(wanted);
        for (AccessibilityNodeInfo node : found) {
            CharSequence text = node.getText();
            if (text != null && wanted.equalsIgnoreCase(text.toString().trim())) return node;
        }
        return found.isEmpty() ? null : found.get(0);
    }

    private AccessibilityNodeInfo findByDescription(AccessibilityNodeInfo node, String wanted) {
        if (node == null) return null;
        CharSequence description = node.getContentDescription();
        if (description != null && wanted.equalsIgnoreCase(description.toString().trim())) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findByDescription(node.getChild(i), wanted);
            if (found != null) return found;
        }
        return null;
    }

    private boolean clickNode(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isClickable() && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            current = current.getParent();
        }
        return false;
    }

    private void collectStrings(AccessibilityNodeInfo node, List<String> out) {
        if (node == null) return;
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) out.add(text.toString());
        CharSequence description = node.getContentDescription();
        if (description != null && description.length() > 0) out.add(description.toString());
        for (int i = 0; i < node.getChildCount(); i++) collectStrings(node.getChild(i), out);
    }

    private boolean containsIgnoreCase(List<String> values, String wanted) {
        for (String value : values) if (wanted.equalsIgnoreCase(value.trim())) return true;
        return false;
    }

    private void resetAttempt() {
        state = IDLE;
        cancelTimeout();
    }

    private void cancelTimeout() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    private String findSdCardId() {
        File storage = new File("/storage");
        File[] entries = storage.listFiles();
        if (entries == null) return null;
        for (File entry : entries) {
            String name = entry.getName();
            if (name.matches("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")) return name;
        }
        return null;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
