package com.imadraude.autoscroller;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SharedPreferences preferences;
    private TextView swipeSpeedValue;
    private int swipeSpeedLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            getWindow().setAttributes(attributes);
        }

        preferences = getSharedPreferences(AppPreferences.FILE_NAME, MODE_PRIVATE);
        swipeSpeedLevel = ScrollTiming.normalizeSwipeSpeed(preferences.getInt(
                AppPreferences.KEY_SWIPE_SPEED,
                ScrollTiming.DEFAULT_SWIPE_SPEED_LEVEL
        ));

        int horizontalPad = dp(20);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setFitsSystemWindows(true);
        scrollView.setBackgroundColor(0xFF0F1115);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(horizontalPad, dp(36), horizontalPad, dp(28));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(0xFF0F1115);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(0, dp(12)));

        TextView info = new TextView(this);
        info.setText(R.string.main_info);
        info.setTextSize(17);
        info.setTextColor(0xFFC7CAD1);
        info.setLineSpacing(0f, 1.15f);
        root.addView(info, matchWrap(0, dp(26)));

        TextView speedLabel = new TextView(this);
        speedLabel.setText(R.string.swipe_speed_label);
        speedLabel.setTextSize(18);
        speedLabel.setTextColor(Color.WHITE);
        root.addView(speedLabel, matchWrap(0, dp(6)));

        LinearLayout speedRow = new LinearLayout(this);
        speedRow.setOrientation(LinearLayout.HORIZONTAL);
        speedRow.setGravity(Gravity.CENTER);

        Button slower = makeControlButton("−");
        slower.setContentDescription(getString(R.string.decrease_swipe_speed_description));
        slower.setOnClickListener(v -> {
            swipeSpeedLevel = ScrollTiming.slowerSwipe(swipeSpeedLevel);
            persistSwipeSpeed();
        });
        speedRow.addView(slower);

        swipeSpeedValue = new TextView(this);
        swipeSpeedValue.setTextSize(20);
        swipeSpeedValue.setTextColor(Color.WHITE);
        swipeSpeedValue.setGravity(Gravity.CENTER);
        speedRow.addView(swipeSpeedValue, new LinearLayout.LayoutParams(dp(64), dp(52)));

        Button faster = makeControlButton("+");
        faster.setContentDescription(getString(R.string.increase_swipe_speed_description));
        faster.setOnClickListener(v -> {
            swipeSpeedLevel = ScrollTiming.fasterSwipe(swipeSpeedLevel);
            persistSwipeSpeed();
        });
        speedRow.addView(faster);

        root.addView(speedRow, matchWrap(0, dp(8)));

        TextView speedHint = new TextView(this);
        speedHint.setText(R.string.swipe_speed_hint);
        speedHint.setTextSize(14);
        speedHint.setTextColor(0xFF9297A1);
        root.addView(speedHint, matchWrap(0, dp(26)));

        Button settings = new Button(this);
        settings.setText(R.string.enable_service);
        settings.setAllCaps(false);
        settings.setTextColor(Color.WHITE);
        settings.setBackgroundColor(0xFF2B3038);
        settings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        root.addView(settings, matchWrap(0, dp(12)));

        TextView privacy = new TextView(this);
        privacy.setText(R.string.privacy_no_internet);
        privacy.setTextSize(14);
        privacy.setTextColor(0xFF858A94);
        root.addView(privacy, matchWrap(0, 0));

        scrollView.addView(root);
        setContentView(scrollView);
        updateSwipeSpeedValue();
    }

    private Button makeControlButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(22);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackgroundColor(0xFF242830);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(52)));
        return button;
    }

    private void persistSwipeSpeed() {
        preferences.edit().putInt(AppPreferences.KEY_SWIPE_SPEED, swipeSpeedLevel).apply();
        updateSwipeSpeedValue();
    }

    private void updateSwipeSpeedValue() {
        if (swipeSpeedValue != null) {
            swipeSpeedValue.setText(String.valueOf(swipeSpeedLevel));
        }
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
