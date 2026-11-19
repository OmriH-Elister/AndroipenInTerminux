package com.omri.openinterminal;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND";
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = dp(24);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(48), pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(248, 248, 252));

        TextView icon = new TextView(this);
        icon.setText(">_");
        icon.setTextSize(40);
        icon.setTextColor(Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundResource(com.omri.openinterminal.R.drawable.ic_terminal);
        root.addView(icon, new LinearLayout.LayoutParams(dp(82), dp(82)));

        TextView title = text("Open in Terminal", 28, Color.rgb(30, 30, 36));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(24);
        root.addView(title, titleLp);

        TextView body = text("Long-press one folder in Samsung My Files, then tap the Open in terminal button that appears. Termux opens with that folder as its working directory.", 17, Color.DKGRAY);
        body.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(-1, -2);
        bodyLp.topMargin = dp(16);
        root.addView(body, bodyLp);

        status = text("", 16, Color.rgb(70, 70, 80));
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.topMargin = dp(28);
        root.addView(status, statusLp);

        Button permission = button("Allow Termux command access");
        permission.setOnClickListener(v -> requestPermissions(new String[]{TERMUX_PERMISSION}, 10));
        addButton(root, permission);

        Button accessibility = button("Enable My Files integration");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        addButton(root, accessibility);

        TextView privacy = text("Privacy: this app has no Internet permission or saved history. The accessibility service watches Samsung My Files for single-item selection mode, then reads the selected folder path only after you press Open in terminal. Other apps are ignored.", 14, Color.GRAY);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyLp = new LinearLayout.LayoutParams(-1, -2);
        privacyLp.topMargin = dp(28);
        root.addView(privacy, privacyLp);

        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        updateStatus();
    }

    private void updateStatus() {
        boolean command = checkSelfPermission(TERMUX_PERMISSION) == PackageManager.PERMISSION_GRANTED;
        boolean access = isAccessibilityEnabled();
        status.setText("Termux command access: " + (command ? "enabled" : "not enabled")
                + "\nMy Files integration: " + (access ? "enabled" : "not enabled"));
        status.setTextColor(command && access ? Color.rgb(20, 120, 55) : Color.rgb(150, 75, 0));
    }

    private boolean isAccessibilityEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName own = new ComponentName(this, MyFilesAccessibilityService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            ComponentName item = ComponentName.unflattenFromString(splitter.next());
            if (own.equals(item)) return true;
        }
        return false;
    }


    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.15f);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setAllCaps(false);
        return button;
    }

    private void addButton(LinearLayout root, View view) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(56));
        lp.topMargin = dp(16);
        root.addView(view, lp);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
