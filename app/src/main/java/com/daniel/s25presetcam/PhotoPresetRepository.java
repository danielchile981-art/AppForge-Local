package com.daniel.s25presetcam;

import android.graphics.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PhotoPresetRepository {
    private static final long S_1_125 = 8_000_000L;
    private static final long S_1_250 = 4_000_000L;
    private static final long S_2 = 2_000_000_000L;
    private static final long S_10 = 10_000_000_000L;

    private static final List<PhotoPreset> PRESETS = Collections.unmodifiableList(Arrays.asList(
            new PhotoPreset("photo_daily", "Câmera padrão", "Dia a dia", "Família, viagem e registros rápidos", 1f, false,
                    12, null, null, null, 0f, false, 0, Color.rgb(68, 214, 232)),
            new PhotoPreset("photo_ultrawide", "Câmera padrão", "Paisagem ampla", "Arquitetura, natureza e espaços apertados", 0.6f, false,
                    12, null, null, null, -0.3f, false, 0, Color.rgb(55, 190, 255)),
            new PhotoPreset("photo_portrait", "Câmera padrão", "Retrato", "Pessoas, animais e fundo mais comprimido", 3f, false,
                    12, null, null, null, 0f, false, 0, Color.rgb(255, 209, 102)),
            new PhotoPreset("photo_distance", "Câmera padrão", "Assunto distante", "Palco, detalhes e objetos longe", 5f, false,
                    12, null, null, null, -0.3f, false, 0, Color.rgb(193, 121, 255)),
            new PhotoPreset("photo_selfie", "Câmera padrão", "Selfie", "Retrato frontal e grupos", 1f, true,
                    12, null, null, null, 0f, false, 0, Color.rgb(255, 105, 155)),

            new PhotoPreset("raw_daily", "Foto Pro · inspirado no Expert RAW", "RAW coringa", "Cena geral com margem para edição", 1f, false,
                    24, null, null, null, -0.3f, false, 0, Color.rgb(84, 223, 160)),
            new PhotoPreset("raw_motion", "Foto Pro · inspirado no Expert RAW", "Movimento", "Crianças, animais, carros e ação", 1f, false,
                    24, 400, S_1_250, null, 0f, false, 0, Color.rgb(138, 120, 255)),
            new PhotoPreset("raw_landscape", "Foto Pro · inspirado no Expert RAW", "Paisagem 50 MP", "Máximo detalhe com muita luz", 1f, false,
                    50, 50, S_1_250, 5600, -0.3f, false, 0, Color.rgb(88, 221, 164)),
            new PhotoPreset("raw_night_hand", "Foto Pro · inspirado no Expert RAW", "Noite à mão", "Rua, festa e interiores sem tripé", 1f, false,
                    24, null, null, null, 0f, false, 0, Color.rgb(255, 180, 91)),
            new PhotoPreset("raw_tripod", "Foto Pro · inspirado no Expert RAW", "Noite com tripé", "Luzes urbanas e cena parada", 1f, false,
                    24, 100, S_2, 4000, 0f, false, 2, Color.rgb(255, 140, 94)),
            new PhotoPreset("raw_moon", "Foto Pro · inspirado no Expert RAW", "Lua", "Lua detalhada sem estourar os realces", 5f, false,
                    24, 100, S_1_250, 5000, -0.7f, true, 2, Color.rgb(205, 210, 255)),
            new PhotoPreset("raw_astro", "Foto Pro · inspirado no Expert RAW", "Astrofotografia", "Céu estrelado com celular no tripé", 1f, false,
                    24, 800, S_10, 4000, 0f, true, 2, Color.rgb(123, 112, 255))
    ));

    private PhotoPresetRepository() {}

    public static List<PhotoPreset> all() {
        return PRESETS;
    }

    public static PhotoPreset find(String id) {
        for (PhotoPreset preset : PRESETS) if (preset.id.equals(id)) return preset;
        return PRESETS.get(0);
    }
}
