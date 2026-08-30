package com.imadraude.autoscroller;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
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

public class AutoScrollService extends AccessibilityService {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View overlay;
    private boolean running = false;
    private boolean scrollDown = true;
    private int speedLevel = ScrollTiming.DEFAULT_LEVEL;
    private boolean gestureInFlight = false;

    private TextView statusView;
    private Button playButton;
    private Button directionButton;
    private TextView speedView;

    private final Runnable scrollLoop = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            performScrollGesture();
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
        panel.setPadding(dp(6), dp(5), dp(6), dp(5));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6202020);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0x55444444);
        panel.setBackground(bg);

        TextView handle = new TextView(this);
        handle.setText("⋮⋮");
        handle.setTextSize(20);
        handle.setTextColor(Color.WHITE);
        handle.setGravity(Gravity.CENTER);
        handle.setPadding(dp(6), 0, dp(6), 0);
        panel.addView(handle, new LinearLayout.LayoutParams(dp(38), dp(44)));

        playButton = makeButton("▶");
        playButton.setContentDescription("Старт або пауза");
        playButton.setOnClickListener(v -> toggleScrolling());
        panel.addView(playButton);

        directionButton = makeButton("↓");
        directionButton.setContentDescription("Змінити напрямок");
        directionButton.setOnClickListener(v -> {
            scrollDown = !scrollDown;
            directionButton.setText(scrollDown ? "↓" : "↑");
            updateStatus();
        });
        panel.addView(directionButton);

        Button slower = makeButton("−");
        slower.setContentDescription("Повільніше");
        slower.setOnClickListener(v -> {
            speedLevel = ScrollTiming.slower(speedLevel);
            updateStatus();
        });
        panel.addView(slower);

        speedView = new TextView(this);
        speedView.setTextSize(15);
        speedView.setTextColor(Color.WHITE);
        speedView.setGravity(Gravity.CENTER);
        panel.addView(speedView, new LinearLayout.LayoutParams(dp(32), dp(44)));

        Button faster = makeButton("+");
        faster.setContentDescription("Швидше");
        faster.setOnClickListener(v -> {
            speedLevel = ScrollTiming.faster(speedLevel);
            updateStatus();
        });
        panel.addView(faster);

        statusView = new TextView(this);
        statusView.setTextSize(12);
        statusView.setTextColor(0xFFBDBDBD);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(dp(4), 0, dp(6), 0);
        panel.addView(statusView, new LinearLayout.LayoutParams(dp(52), dp(44)));

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(12);
        params.y = dp(160);

        installDrag(handle, params);

        overlay = panel;
        windowManager.addView(overlay, params);
        updateStatus();
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
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(44)));
        return button;
    }

    private void installDrag(View handle, WindowManager.LayoutParams params) {
        final float[] touchX = new float[1];
        final float[] touchY = new float[1];
        final int[] startX = new int[1];
        final int[] startY = new int[1];

        handle.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchX[0] = event.getRawX();
                    touchY[0] = event.getRawY();
                    startX[0] = params.x;
                    startY[0] = params.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = startX[0] + Math.round(event.getRawX() - touchX[0]);
                    params.y = startY[0] + Math.round(event.getRawY() - touchY[0]);
                    if (overlay != null) {
                        windowManager.updateViewLayout(overlay, params);
                    }
                    return true;
                default:
                    return true;
            }
        });
    }

    private void toggleScrolling() {
        if (running) {
            stopScrolling();
        } else {
            running = true;
            playButton.setText("Ⅱ");
            updateStatus();
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
        updateStatus();
    }

    private void performScrollGesture() {
        if (!running || gestureInFlight) {
            return;
        }

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        float x = width * 0.72f;
        float top = height * 0.30f;
        float bottom = height * 0.72f;

        float startY = scrollDown ? bottom : top;
        float endY = scrollDown ? top : bottom;

        long duration = ScrollTiming.durationForSpeed(speedLevel);
        long pause = ScrollTiming.pauseForSpeed(speedLevel);

        Path path = new Path();
        path.moveTo(x, startY);
        path.lineTo(x, endY);

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
                    handler.postDelayed(scrollLoop, pause);
                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                gestureInFlight = false;
                if (running) {
                    handler.postDelayed(scrollLoop, pause + 150);
                }
            }
        }, handler);

        if (!accepted) {
            gestureInFlight = false;
            handler.postDelayed(scrollLoop, 300);
        }
    }

    private void updateStatus() {
        if (speedView != null) {
            speedView.setText(String.valueOf(speedLevel));
        }
        if (statusView != null) {
            statusView.setText(running ? "СКРОЛ" : "СТОП");
        }
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
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
