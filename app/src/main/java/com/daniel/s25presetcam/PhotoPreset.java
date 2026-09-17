package com.daniel.s25presetcam;

public final class PhotoPreset {
    public final String id;
    public final String family;
    public final String name;
    public final String objective;
    public final float zoom;
    public final boolean frontCamera;
    public final int megapixels;
    public final Integer iso;
    public final Long exposureNs;
    public final Integer whiteBalanceKelvin;
    public final float exposureCompensation;
    public final boolean infinityFocus;
    public final int timerSeconds;
    public final int accentColor;

    public PhotoPreset(
            String id,
            String family,
            String name,
            String objective,
            float zoom,
            boolean frontCamera,
            int megapixels,
            Integer iso,
            Long exposureNs,
            Integer whiteBalanceKelvin,
            float exposureCompensation,
            boolean infinityFocus,
            int timerSeconds,
            int accentColor
    ) {
        this.id = id;
        this.family = family;
        this.name = name;
        this.objective = objective;
        this.zoom = zoom;
        this.frontCamera = frontCamera;
        this.megapixels = megapixels;
        this.iso = iso;
        this.exposureNs = exposureNs;
        this.whiteBalanceKelvin = whiteBalanceKelvin;
        this.exposureCompensation = exposureCompensation;
        this.infinityFocus = infinityFocus;
        this.timerSeconds = timerSeconds;
        this.accentColor = accentColor;
    }

    public boolean usesManualExposure() {
        return iso != null && exposureNs != null;
    }

    public String lensLabel() {
        if (frontCamera) return "Frontal";
        if (zoom < 0.8f) return "0,6x";
        if (zoom < 1.5f) return "1x";
        if (zoom < 2.5f) return "2x";
        if (zoom < 4f) return "3x";
        return "5x";
    }

    public String shortSettings() {
        StringBuilder value = new StringBuilder()
                .append(megapixels).append(" MP | ").append(lensLabel());
        if (usesManualExposure()) {
            value.append(" | ISO ").append(iso).append(" | ").append(shutterLabel());
        } else {
            value.append(" | Auto");
            if (exposureCompensation != 0f) value.append(" | EV ").append(exposureCompensation < 0 ? "−" : "+").append(Math.abs(exposureCompensation));
        }
        if (infinityFocus) value.append(" | Foco ∞");
        if (timerSeconds > 0) value.append(" | Timer ").append(timerSeconds).append(" s");
        return value.toString();
    }

    private String shutterLabel() {
        if (exposureNs == null) return "Auto";
        if (exposureNs >= 1_000_000_000L) {
            double seconds = exposureNs / 1_000_000_000d;
            return seconds == Math.rint(seconds) ? ((int) seconds) + " s" : String.format(java.util.Locale.US, "%.1f s", seconds);
        }
        return "1/" + Math.round(1_000_000_000d / exposureNs);
    }
}
