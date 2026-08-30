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
        title.setText("AutoScroller Lite");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(0, dp(12)));

        TextView info = new TextView(this);
        info.setText(
                "Мінімальний локальний автоскролер.\n\n" +
                "• без інтернету\n" +
                "• без реклами та аналітики\n" +
                "• без акаунтів\n" +
                "• без збереження вмісту екрана\n\n" +
                "Для виконання жестів Android вимагає увімкнути службу спеціальних можливостей. " +
                "Після увімкнення з'явиться маленька плаваюча панель."
        );
        info.setTextSize(17);
        info.setTextColor(Color.DKGRAY);
        info.setLineSpacing(0f, 1.15f);
        root.addView(info, matchWrap(0, dp(24)));

        Button settings = new Button(this);
        settings.setText("Увімкнути службу");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        root.addView(settings, matchWrap(0, dp(10)));

        TextView privacy = new TextView(this);
        privacy.setText("У маніфесті навмисно відсутній дозвіл INTERNET, тому застосунок не може підключатися до мережі.");
        privacy.setTextSize(14);
        privacy.setTextColor(Color.GRAY);
        root.addView(privacy, matchWrap(0, 0));

        setContentView(root);
    }

    private LinearLayout.LayoutParams matchWrap(int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.topMargin = top;
        p.bottomMargin = bottom;
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
