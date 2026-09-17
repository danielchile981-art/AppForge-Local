package com.daniel.s25presetcam;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 7;
    private String pendingPresetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(9, 14, 28));
        getWindow().setNavigationBarColor(Color.rgb(9, 14, 28));
        setContentView(buildContent());
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(9, 14, 28));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        ImageView coverLogo = new ImageView(this);
        coverLogo.setImageResource(R.drawable.ic_camera_logo);
        coverLogo.setAlpha(0f);
        coverLogo.setScaleX(.72f);
        coverLogo.setScaleY(.72f);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(104), dp(104));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.bottomMargin = dp(14);
        root.addView(coverLogo, logoParams);

        TextView eyebrow = text("CÂMERA COM PRESETS", 12, Color.rgb(68, 214, 232), true);
        eyebrow.setGravity(Gravity.CENTER);
        root.addView(eyebrow);

        TextView title = text("S25 Preset Cam", 30, Color.rgb(247, 249, 255), true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(7);
        root.addView(title, titleParams);

        TextView subtitle = text(
                "Escolha o objetivo. O app abre a câmera e aplica a melhor configuração disponível no aparelho.",
                15,
                Color.rgb(180, 189, 211),
                false
        );
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(8);
        subParams.bottomMargin = dp(20);
        root.addView(subtitle, subParams);

        TextView tip = text(
                "4K e FPS são ajustados automaticamente se uma lente não aceitar a combinação escolhida.",
                12,
                Color.rgb(247, 249, 255),
                false
        );
        tip.setPadding(dp(14), dp(12), dp(14), dp(12));
        tip.setBackground(rounded(Color.rgb(24, 34, 58), Color.rgb(48, 64, 95), dp(14)));
        LinearLayout.LayoutParams tipParams = new LinearLayout.LayoutParams(-1, -2);
        tipParams.bottomMargin = dp(18);
        root.addView(tip, tipParams);

        List<Preset> presets = PresetRepository.all();
        int animationIndex = 0;
        for (Preset preset : presets) {
            View presetView = presetCard(preset);
            presetView.setAlpha(0f);
            presetView.setTranslationY(dp(26));
            root.addView(presetView);
            animateEntry(presetView, 280L + animationIndex * 65L);
            animationIndex++;
        }

        TextView footer = text(
                "As gravações são salvas em Galeria > Movies > S25 Preset Cam.",
                12,
                Color.rgb(127, 138, 165),
                false
        );
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(12);
        root.addView(footer, footerParams);
        coverLogo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(650).setInterpolator(new android.view.animation.OvershootInterpolator()).start();
        eyebrow.setAlpha(0f);
        title.setAlpha(0f);
        subtitle.setAlpha(0f);
        animateEntry(eyebrow, 70);
        animateEntry(title, 130);
        animateEntry(subtitle, 190);
        return scroll;
    }

    private View presetCard(Preset preset) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(5), dp(4), dp(16), dp(4));
        card.setBackground(rounded(Color.rgb(21, 30, 52), Color.rgb(43, 57, 87), dp(16)));
        card.setClickable(true);
        card.setFocusable(true);

        View accent = new View(this);
        GradientDrawable accentBg = new GradientDrawable();
        accentBg.setColor(preset.accentColor);
        accentBg.setCornerRadius(dp(5));
        accent.setBackground(accentBg);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(6), dp(76));
        accentParams.rightMargin = dp(14);
        card.addView(accent, accentParams);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0, dp(12), 0, dp(12));
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        info.addView(text(preset.name, 18, Color.rgb(247, 249, 255), true));
        TextView objective = text(preset.objective, 13, Color.rgb(180, 189, 211), false);
        LinearLayout.LayoutParams objectiveParams = new LinearLayout.LayoutParams(-1, -2);
        objectiveParams.topMargin = dp(3);
        info.addView(objective, objectiveParams);
        TextView settings = text(preset.shortSettings(), 12, preset.accentColor, true);
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(-1, -2);
        settingsParams.topMargin = dp(5);
        info.addView(settings, settingsParams);

        TextView arrow = text("›", 30, preset.accentColor, false);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(26), dp(60)));

        card.setOnClickListener(v -> {
            v.animate().scaleX(.97f).scaleY(.97f).setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                        launchPreset(preset.id);
                    }).start();
        });
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        return card;
    }

    private void animateEntry(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(dp(20));
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void launchPreset(String presetId) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingPresetId = presetId;
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
            return;
        }
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(CameraActivity.EXTRA_PRESET_ID, presetId);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS) return;
        boolean granted = grantResults.length >= 2;
        for (int result : grantResults) granted &= result == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingPresetId != null) {
            launchPreset(pendingPresetId);
        } else {
            Toast.makeText(this, "A câmera e o microfone precisam de permissão para gravar.", Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        return text;
    }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(radius);
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
