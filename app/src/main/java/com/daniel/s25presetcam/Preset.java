package com.daniel.s25presetcam;

public final class Preset {
    public final String id;
    public final String name;
    public final String objective;
    public final float zoom;
    public final boolean frontCamera;
    public final int width;
    public final int height;
    public final int fps;
    public final Integer iso;
    public final Long exposureNs;
    public final Integer whiteBalanceKelvin;
    public final boolean stabilization;
    public final int accentColor;

    public Preset(
            String id,
            String name,
            String objective,
            float zoom,
            boolean frontCamera,
            int width,
            int height,
            int fps,
            Integer iso,
            Long exposureNs,
            Integer whiteBalanceKelvin,
            boolean stabilization,
            int accentColor
    ) {
        this.id = id;
        this.name = name;
        this.objective = objective;
        this.zoom = zoom;
        this.frontCamera = frontCamera;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.iso = iso;
        this.exposureNs = exposureNs;
        this.whiteBalanceKelvin = whiteBalanceKelvin;
        this.stabilization = stabilization;
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
        String resolution = width >= 3800 ? "4K" : "FHD";
        StringBuilder value = new StringBuilder(resolution)
                .append(" ").append(fps).append(" fps | ").append(lensLabel());
        if (usesManualExposure()) {
            value.append(" | ISO ").append(iso)
                    .append(" | 1/").append(Math.round(1_000_000_000d / exposureNs));
        } else {
            value.append(" | Automático");
        }
        return value.toString();
    }
}
