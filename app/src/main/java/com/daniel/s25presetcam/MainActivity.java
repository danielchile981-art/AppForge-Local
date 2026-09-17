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

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 7;
    private static final String MODE_PHOTO = "photo";
    private static final String MODE_VIDEO = "video";

    private LinearLayout presetContainer;
    private TextView photoTab;
    private TextView videoTab;
    private TextView tipText;
    private TextView footerText;
    private String pendingPresetId;
    private String pendingMode;
    private boolean showingPhotos = true;

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
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        ImageView coverLogo = new ImageView(this);
        coverLogo.setImageResource(R.drawable.ic_camera_logo);
        coverLogo.setAlpha(0f);
        coverLogo.setScaleX(.72f);
        coverLogo.setScaleY(.72f);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(104), dp(104));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.bottomMargin = dp(14);
        root.addView(coverLogo, logoParams);

        TextView eyebrow = text("FOTOS E VÍDEOS COM PRESETS", 12, Color.rgb(68, 214, 232), true);
        eyebrow.setGravity(Gravity.CENTER);
        root.addView(eyebrow);

        TextView title = text("S25 Preset Cam", 30, Color.rgb(247, 249, 255), true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(7);
        root.addView(title, titleParams);

        TextView subtitle = text(
                "Escolha Foto ou Vídeo e toque no objetivo. A câmera abre com a configuração compatível mais próxima.",
                15, Color.rgb(180, 189, 211), false
        );
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(8);
        subParams.bottomMargin = dp(18);
        root.addView(subtitle, subParams);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabs.setBackground(rounded(Color.rgb(16, 24, 43), Color.rgb(43, 57, 87), dp(18)));

        photoTab = tab("FOTOS", true);
        videoTab = tab("VÍDEOS", false);
        tabs.addView(photoTab, new LinearLayout.LayoutParams(0, dp(48), 1f));
        tabs.addView(videoTab, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(-1, -2);
        tabsParams.bottomMargin = dp(14);
        root.addView(tabs, tabsParams);

        tipText = text("Os presets Pro usam Camera2. O processamento exclusivo do Samsung Expert RAW não pode ser reproduzido por outro app.",
                12, Color.rgb(247, 249, 255), false);
        tipText.setPadding(dp(14), dp(12), dp(14), dp(12));
        tipText.setBackground(rounded(Color.rgb(24, 34, 58), Color.rgb(48, 64, 95), dp(14)));
        LinearLayout.LayoutParams tipParams = new LinearLayout.LayoutParams(-1, -2);
        tipParams.bottomMargin = dp(18);
        root.addView(tipText, tipParams);

        presetContainer = new LinearLayout(this);
        presetContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(presetContainer, new LinearLayout.LayoutParams(-1, -2));

        footerText = text("As fotos são salvas em Galeria > Pictures > S25 Preset Cam.",
                12, Color.rgb(127, 138, 165), false);
        footerText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(12);
        root.addView(footerText, footerParams);

        photoTab.setOnClickListener(v -> showPhotos());
        videoTab.setOnClickListener(v -> showVideos());
        populatePhotoPresets();

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

    private TextView tab(String label, boolean selected) {
        TextView tab = text(label, 13, selected ? Color.rgb(9, 14, 28) : Color.rgb(180, 189, 211), true);
        tab.setGravity(Gravity.CENTER);
        tab.setBackground(selected
                ? rounded(Color.rgb(68, 214, 232), Color.rgb(68, 214, 232), dp(15))
                : rounded(Color.TRANSPARENT, Color.TRANSPARENT, dp(15)));
        return tab;
    }

    private void showPhotos() {
        if (showingPhotos) return;
        showingPhotos = true;
        updateTabs();
        tipText.setText("Os presets Pro usam Camera2. O processamento exclusivo do Samsung Expert RAW não pode ser reproduzido por outro app.");
        footerText.setText("As fotos são salvas em Galeria > Pictures > S25 Preset Cam.");
        populatePhotoPresets();
    }

    private void showVideos() {
        if (!showingPhotos) return;
        showingPhotos = false;
        updateTabs();
        tipText.setText("4K, FPS e lente são ajustados automaticamente quando uma combinação não é aceita pelo sensor.");
        footerText.setText("Os vídeos são salvos em Galeria > Movies > S25 Preset Cam.");
        populateVideoPresets();
    }

    private void updateTabs() {
        styleTab(photoTab, showingPhotos);
        styleTab(videoTab, !showingPhotos);
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setTextColor(selected ? Color.rgb(9, 14, 28) : Color.rgb(180, 189, 211));
        tab.setBackground(selected
                ? rounded(Color.rgb(68, 214, 232), Color.rgb(68, 214, 232), dp(15))
                : rounded(Color.TRANSPARENT, Color.TRANSPARENT, dp(15)));
        tab.setScaleX(.94f);
        tab.setScaleY(.94f);
        tab.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
    }

    private void populatePhotoPresets() {
        presetContainer.removeAllViews();
        String family = "";
        int index = 0;
        for (PhotoPreset preset : PhotoPresetRepository.all()) {
            if (!family.equals(preset.family)) {
                family = preset.family;
                presetContainer.addView(sectionHeader(family));
            }
            View card = photoCard(preset);
            presetContainer.addView(card);
            animateEntry(card, 70L + index * 45L);
            index++;
        }
    }

    private void populateVideoPresets() {
        presetContainer.removeAllViews();
        presetContainer.addView(sectionHeader("Presets de vídeo"));
        int index = 0;
        for (Preset preset : PresetRepository.all()) {
            View card = videoCard(preset);
            presetContainer.addView(card);
            animateEntry(card, 70L + index * 45L);
            index++;
        }
    }

    private TextView sectionHeader(String value) {
        TextView header = text(value.toUpperCase(), 11, Color.rgb(127, 138, 165), true);
        header.setPadding(dp(4), dp(7), 0, dp(9));
        return header;
    }

    private View photoCard(PhotoPreset preset) {
        return presetCard(preset.name, preset.objective, preset.shortSettings(), preset.accentColor,
                () -> launchPreset(MODE_PHOTO, preset.id));
    }

    private View videoCard(Preset preset) {
        return presetCard(preset.name, preset.objective, preset.shortSettings(), preset.accentColor,
                () -> launchPreset(MODE_VIDEO, preset.id));
    }

    private View presetCard(String name, String objectiveValue, String settingsValue, int accentColor, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(5), dp(4), dp(16), dp(4));
        card.setBackground(rounded(Color.rgb(21, 30, 52), Color.rgb(43, 57, 87), dp(16)));
        card.setClickable(true);
        card.setFocusable(true);

        View accent = new View(this);
        GradientDrawable accentBg = new GradientDrawable();
        accentBg.setColor(accentColor);
        accentBg.setCornerRadius(dp(5));
        accent.setBackground(accentBg);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(6), dp(76));
        accentParams.rightMargin = dp(14);
        card.addView(accent, accentParams);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0, dp(12), 0, dp(12));
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
        info.addView(text(name, 18, Color.rgb(247, 249, 255), true));

        TextView objective = text(objectiveValue, 13, Color.rgb(180, 189, 211), false);
        LinearLayout.LayoutParams objectiveParams = new LinearLayout.LayoutParams(-1, -2);
        objectiveParams.topMargin = dp(3);
        info.addView(objective, objectiveParams);

        TextView settings = text(settingsValue, 12, accentColor, true);
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(-1, -2);
        settingsParams.topMargin = dp(5);
        info.addView(settings, settingsParams);

        TextView arrow = text("›", 30, accentColor, false);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(26), dp(60)));

        card.setOnClickListener(v -> v.animate().scaleX(.97f).scaleY(.97f).setDuration(80)
                .withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    action.run();
                }).start());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setAlpha(0f);
        card.setTranslationY(dp(20));
        return card;
    }

    private void animateEntry(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(dp(20));
        view.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(380)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void launchPreset(String mode, String presetId) {
        boolean cameraMissing = checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
        boolean audioMissing = MODE_VIDEO.equals(mode)
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;
        if (cameraMissing || audioMissing) {
            pendingMode = mode;
            pendingPresetId = presetId;
            if (MODE_VIDEO.equals(mode)) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
            }
            return;
        }
        Intent intent;
        if (MODE_PHOTO.equals(mode)) {
            intent = new Intent(this, PhotoCameraActivity.class);
            intent.putExtra(PhotoCameraActivity.EXTRA_PRESET_ID, presetId);
        } else {
            intent = new Intent(this, CameraActivity.class);
            intent.putExtra(CameraActivity.EXTRA_PRESET_ID, presetId);
        }
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS) return;
        boolean granted = grantResults.length > 0;
        for (int result : grantResults) granted &= result == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingPresetId != null && pendingMode != null) {
            launchPreset(pendingMode, pendingPresetId);
        } else {
            String message = MODE_VIDEO.equals(pendingMode)
                    ? "A câmera e o microfone precisam de permissão para gravar."
                    : "A câmera precisa de permissão para fotografar.";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
