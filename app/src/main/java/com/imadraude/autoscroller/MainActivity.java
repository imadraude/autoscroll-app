package com.imadraude.autoscroller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = dp(20);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(0, dp(12)));

        TextView info = new TextView(this);
        info.setText(R.string.main_info);
        info.setTextSize(17);
        info.setTextColor(Color.DKGRAY);
        info.setLineSpacing(0f, 1.15f);
        root.addView(info, matchWrap(0, dp(24)));

        Button settings = new Button(this);
        settings.setText(R.string.enable_service);
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        root.addView(settings, matchWrap(0, dp(10)));

        TextView privacy = new TextView(this);
        privacy.setText(R.string.privacy_no_internet);
        privacy.setTextSize(14);
        privacy.setTextColor(Color.GRAY);
        root.addView(privacy, matchWrap(0, 0));

        setContentView(root);
    }

    private LinearLayout.LayoutParams matchWrap(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = top;
        params.bottomMargin = bottom;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
