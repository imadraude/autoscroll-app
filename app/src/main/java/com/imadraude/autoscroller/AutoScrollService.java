package com.imadraude.autoscroller;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

public class AutoScrollService extends AccessibilityService {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private WindowManager windowManager;
    private View overlay;
    private WindowManager.LayoutParams overlayParams;
    private boolean running = false;
    private boolean scrollDown = true;
    private boolean gestureInFlight = false;
    private boolean collapsed = false;
    private int frequencyLevel = ScrollTiming.DEFAULT_FREQUENCY_LEVEL;

    private TextView handleView;
    private Button playButton;
    private Button directionButton;
    private TextView frequencyView;
    private View[] expandedControls;

    private final Runnable scrollLoop = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            performHumanSwipe();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        showOverlay();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Події навмисно не читаються й не зберігаються.
    }

    @Override
    public void onInterrupt() {
        stopScrolling();
    }

    @Override
    public void onDestroy() {
        stopScrolling();
        removeOverlay();
        super.onDestroy();
    }

    private void showOverlay() {
        if (overlay != null) {
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(5), dp(4), dp(5), dp(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF0202227);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0x66555A63);
        panel.setBackground(bg);

        handleView = new TextView(this);
        handleView.setText("⋮⋮");
        handleView.setTextSize(20);
        handleView.setTextColor(Color.WHITE);
        handleView.setGravity(Gravity.CENTER);
        handleView.setPadding(dp(5), 0, dp(5), 0);
        handleView.setContentDescription(getString(R.string.drag_handle_description));
        panel.addView(handleView, new LinearLayout.LayoutParams(dp(38), dp(44)));

        playButton = makeButton("▶");
        playButton.setContentDescription(getString(R.string.start_pause_description));
        playButton.setOnClickListener(v -> toggleScrolling());
        panel.addView(playButton);

        directionButton = makeButton("↓");
        directionButton.setContentDescription(getString(R.string.change_direction_description));
        directionButton.setOnClickListener(v -> {
            scrollDown = !scrollDown;
            directionButton.setText(scrollDown ? "↓" : "↑");
        });
        panel.addView(directionButton);

        Button lessFrequent = makeButton("−");
        lessFrequent.setContentDescription(getString(R.string.less_frequent_description));
        lessFrequent.setOnClickListener(v -> {
            frequencyLevel = ScrollTiming.lessFrequent(frequencyLevel);
            updateFrequencyView();
        });
        panel.addView(lessFrequent);

        frequencyView = new TextView(this);
        frequencyView.setTextSize(15);
        frequencyView.setTextColor(Color.WHITE);
        frequencyView.setGravity(Gravity.CENTER);
        frequencyView.setContentDescription(getString(R.string.frequency_level_description));
        panel.addView(frequencyView, new LinearLayout.LayoutParams(dp(34), dp(44)));

        Button moreFrequent = makeButton("+");
        moreFrequent.setContentDescription(getString(R.string.more_frequent_description));
        moreFrequent.setOnClickListener(v -> {
            frequencyLevel = ScrollTiming.moreFrequent(frequencyLevel);
            updateFrequencyView();
        });
        panel.addView(moreFrequent);

        Button minimize = makeButton("—");
        minimize.setContentDescription(getString(R.string.minimize_panel_description));
        minimize.setOnClickListener(v -> setCollapsed(true));
        panel.addView(minimize);

        Button close = makeButton("×");
        close.setContentDescription(getString(R.string.close_panel_description));
        close.setOnClickListener(v -> {
            stopScrolling();
            removeOverlay();
            disableSelf();
        });
        panel.addView(close);

        expandedControls = new View[]{
                playButton,
                directionButton,
                lessFrequent,
                frequencyView,
                moreFrequent,
                minimize,
                close
        };

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(10);
        params.y = dp(150);

        overlay = panel;
        overlayParams = params;

        handleView.setOnClickListener(v -> {
            if (collapsed) {
                setCollapsed(false);
            }
        });
        installDrag(handleView, params);

        windowManager.addView(overlay, params);
        updateFrequencyView();
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(44)));
        return button;
    }

    private void setCollapsed(boolean shouldCollapse) {
        collapsed = shouldCollapse;
        if (expandedControls != null) {
            for (View control : expandedControls) {
                control.setVisibility(collapsed ? View.GONE : View.VISIBLE);
            }
        }
        if (handleView != null) {
            handleView.setText(collapsed ? "●" : "⋮⋮");
            handleView.setContentDescription(getString(collapsed
                    ? R.string.expand_panel_description
                    : R.string.drag_handle_description));
        }
        if (overlay != null && windowManager != null && overlayParams != null) {
            windowManager.updateViewLayout(overlay, overlayParams);
        }
    }

    private void installDrag(View handle, WindowManager.LayoutParams params) {
        final float[] touchX = new float[1];
        final float[] touchY = new float[1];
        final int[] startX = new int[1];
        final int[] startY = new int[1];
        final boolean[] moved = new boolean[1];

        handle.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchX[0] = event.getRawX();
                    touchY[0] = event.getRawY();
                    startX[0] = params.x;
                    startY[0] = params.y;
                    moved[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - touchX[0];
                    float deltaY = event.getRawY() - touchY[0];
                    if (Math.abs(deltaX) > dp(4) || Math.abs(deltaY) > dp(4)) {
                        moved[0] = true;
                    }
                    params.x = startX[0] + Math.round(deltaX);
                    params.y = startY[0] + Math.round(deltaY);
                    if (overlay != null) {
                        windowManager.updateViewLayout(overlay, params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!moved[0]) {
                        view.performClick();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return true;
                default:
                    return false;
            }
        });
    }

    private void toggleScrolling() {
        if (running) {
            stopScrolling();
        } else {
            running = true;
            if (playButton != null) {
                playButton.setText("Ⅱ");
            }
            handler.removeCallbacks(scrollLoop);
            handler.post(scrollLoop);
        }
    }

    private void stopScrolling() {
        running = false;
        gestureInFlight = false;
        handler.removeCallbacks(scrollLoop);
        if (playButton != null) {
            playButton.setText("▶");
        }
    }

    private void performHumanSwipe() {
        if (!running || gestureInFlight) {
            return;
        }

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        float startX = width * randomBetween(0.66f, 0.76f);
        float endX = startX + width * randomBetween(-0.025f, 0.025f);

        float startRatio = scrollDown
                ? randomBetween(0.70f, 0.80f)
                : randomBetween(0.20f, 0.30f);
        float endRatio = scrollDown
                ? randomBetween(0.20f, 0.34f)
                : randomBetween(0.66f, 0.80f);

        float startY = height * startRatio;
        float endY = height * endRatio;
        float deltaY = endY - startY;
        float bend = width * randomBetween(-0.018f, 0.018f);

        float control1X = startX + bend;
        float control1Y = startY + deltaY * randomBetween(0.24f, 0.36f);
        float control2X = endX - bend * 0.55f;
        float control2Y = startY + deltaY * randomBetween(0.66f, 0.82f);

        SharedPreferences preferences = getSharedPreferences(AppPreferences.FILE_NAME, MODE_PRIVATE);
        int swipeSpeed = ScrollTiming.normalizeSwipeSpeed(preferences.getInt(
                AppPreferences.KEY_SWIPE_SPEED,
                ScrollTiming.DEFAULT_SWIPE_SPEED_LEVEL
        ));

        long baseDuration = ScrollTiming.swipeDurationForSpeed(swipeSpeed);
        long duration = randomizedTime(baseDuration, 0.10f, 70L);
        long basePeriod = ScrollTiming.periodForFrequency(frequencyLevel);
        long period = randomizedTime(basePeriod, 0.04f, 200L);
        long nextDelay = Math.max(80L, period - duration);

        Path path = new Path();
        path.moveTo(startX, startY);
        path.cubicTo(control1X, control1Y, control2X, control2Y, endX, endY);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, duration);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        gestureInFlight = true;
        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                gestureInFlight = false;
                if (running) {
                    handler.postDelayed(scrollLoop, nextDelay);
                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                gestureInFlight = false;
                if (running) {
                    handler.postDelayed(scrollLoop, nextDelay + 150L);
                }
            }
        }, handler);

        if (!accepted) {
            gestureInFlight = false;
            handler.postDelayed(scrollLoop, 300L);
        }
    }

    private void updateFrequencyView() {
        if (frequencyView != null) {
            frequencyView.setText(String.valueOf(frequencyLevel));
        }
    }

    private float randomBetween(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private long randomizedTime(long base, float fraction, long minimum) {
        float factor = randomBetween(1.0f - fraction, 1.0f + fraction);
        return Math.max(minimum, Math.round(base * factor));
    }

    private void removeOverlay() {
        if (overlay != null && windowManager != null) {
            try {
                windowManager.removeView(overlay);
            } catch (RuntimeException ignored) {
                // Вікно вже могло бути видалене системою.
            }
        }
        overlay = null;
        overlayParams = null;
        handleView = null;
        playButton = null;
        directionButton = null;
        frequencyView = null;
        expandedControls = null;
        collapsed = false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
