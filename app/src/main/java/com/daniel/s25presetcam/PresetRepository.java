package com.daniel.s25presetcam;

import android.graphics.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PresetRepository {
    private static final long S_1_50 = 20_000_000L;
    private static final long S_1_60 = 16_666_667L;
    private static final long S_1_120 = 8_333_333L;
    private static final long S_1_240 = 4_166_667L;

    private static final List<Preset> PRESETS = Collections.unmodifiableList(Arrays.asList(
            new Preset("daily", "Dia a dia", "Família, viagem e vídeos gerais", 1f, false,
                    3840, 2160, 30, null, null, null, true, Color.rgb(68, 214, 232)),
            new Preset("cinema", "Cinema", "Movimento natural e visual cinematográfico", 1f, false,
                    3840, 2160, 24, 100, S_1_50, 5600, true, Color.rgb(84, 223, 160)),
            new Preset("motion", "Movimento", "Esportes, animais, carros e crianças", 1f, false,
                    3840, 2160, 60, 200, S_1_120, 5600, true, Color.rgb(138, 120, 255)),
            new Preset("night", "Noite", "Cidade, festa e ambiente interno", 1f, false,
                    3840, 2160, 30, 800, S_1_60, 4200, true, Color.rgb(255, 180, 91)),
            new Preset("landscape", "Paisagem ampla", "Arquitetura, natureza e espaços apertados", 0.6f, false,
                    3840, 2160, 30, null, null, 5600, true, Color.rgb(55, 190, 255)),
            new Preset("product", "Retrato / Produto", "Pessoas, comida e detalhes", 2f, false,
                    3840, 2160, 30, 200, S_1_60, 5000, true, Color.rgb(255, 209, 102)),
            new Preset("concert_near", "Show perto", "Artista em distância média", 3f, false,
                    3840, 2160, 30, 800, S_1_60, 4000, true, Color.rgb(255, 111, 130)),
            new Preset("concert_far", "Show longe", "Palco distante e detalhes", 5f, false,
                    3840, 2160, 30, 800, S_1_60, 4000, true, Color.rgb(193, 121, 255)),
            new Preset("interview", "Entrevista", "Pessoa falando com ajustes travados", 2f, false,
                    3840, 2160, 30, 400, S_1_60, 4500, true, Color.rgb(70, 225, 190)),
            new Preset("slow", "Câmera lenta", "Ação rápida para desacelerar", 1f, false,
                    1920, 1080, 120, 200, S_1_240, 5600, false, Color.rgb(255, 224, 100)),
            new Preset("vlog", "Vlog frontal", "Reels, Stories e explicações", 1f, true,
                    3840, 2160, 30, null, null, null, true, Color.rgb(255, 105, 155))
    ));

    private PresetRepository() {}

    public static List<Preset> all() {
        return PRESETS;
    }

    public static Preset find(String id) {
        for (Preset preset : PRESETS) {
            if (preset.id.equals(id)) return preset;
        }
        return PRESETS.get(0);
    }
}
