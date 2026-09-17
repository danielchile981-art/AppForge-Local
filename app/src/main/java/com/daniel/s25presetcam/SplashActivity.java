package com.daniel.s25presetcam;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(9, 14, 28));
        getWindow().setNavigationBarColor(Color.rgb(9, 14, 28));

        FrameLayout root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(9, 14, 28), Color.rgb(20, 28, 52), Color.rgb(31, 25, 70)}
        ));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(28), dp(40), dp(28), dp(40));
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.daniel.s25presetcam.R.drawable.ic_camera_logo);
        logo.setAlpha(0f);
        logo.setScaleX(0.55f);
        logo.setScaleY(0.55f);
        logo.setRotation(-12f);
        content.addView(logo, new LinearLayout.LayoutParams(dp(150), dp(150)));

        TextView title = label("S25 Preset Cam", 31, Color.WHITE, true);
        title.setAlpha(0f);
        title.setTranslationY(dp(18));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(18);
        content.addView(title, titleParams);

        TextView subtitle = label("Escolha o objetivo. Grave com a configuração certa.", 15,
                Color.rgb(190, 199, 220), false);
        subtitle.setAlpha(0f);
        subtitle.setTranslationY(dp(14));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(8);
        content.addView(subtitle, subtitleParams);

        TextView badge = label("CRIADO PARA GALAXY S25 ULTRA", 11,
                Color.rgb(68, 214, 232), true);
        badge.setAlpha(0f);
        badge.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(Color.argb(55, 68, 214, 232));
        badgeBg.setStroke(dp(1), Color.rgb(68, 214, 232));
        badgeBg.setCornerRadius(dp(22));
        badge.setBackground(badgeBg);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2);
        badgeParams.topMargin = dp(28);
        content.addView(badge, badgeParams);

        setContentView(root);

        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(0f)
                .setDuration(650).setInterpolator(new android.view.animation.OvershootInterpolator(1.05f)).start();
        title.animate().alpha(1f).translationY(0).setStartDelay(250).setDuration(500).start();
        subtitle.animate().alpha(1f).translationY(0).setStartDelay(430).setDuration(500).start();
        badge.animate().alpha(1f).setStartDelay(650).setDuration(450).start();

        root.postDelayed(() -> root.animate().alpha(0f).scaleX(1.03f).scaleY(1.03f)
                .setDuration(350).setListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                }).start(), 1850);
    }

    private TextView label(String value, float sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        return text;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
