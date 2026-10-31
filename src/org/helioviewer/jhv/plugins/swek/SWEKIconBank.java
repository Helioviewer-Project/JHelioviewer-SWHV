package org.helioviewer.jhv.plugins.swek;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.helioviewer.jhv.base.PngPixels;
import org.helioviewer.jhv.io.FileUtils;

public final class SWEKIconBank {

    private static final HashMap<String, PngPixels> icons = new HashMap<>();
    private static final HashMap<String, byte[]> files = new HashMap<>();

    static PngPixels getPixels(String iconName) {
        return icons.computeIfAbsent(resource(iconName), value -> {
            try {
                return PngPixels.decode(png(iconName));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot decode SWEK icon " + value, e);
            }
        });
    }

    // The icon's PNG file.
    public static byte[] png(String iconName) {
        return files.computeIfAbsent(resource(iconName), value -> {
            try (InputStream input = FileUtils.getResource(value)) {
                return input.readAllBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot load SWEK icon " + value, e);
            }
        });
    }

    public static String resource(String iconName) {
        String file = switch (iconName) {
            case "ActiveRegion" -> "ar_icon.png";
            case "CoronalDimming" -> "cd_icon.png";
            case "CME" -> "ce_icon.png";
            case "CoronalHole" -> "ch_icon.png";
            case "CoronalWave" -> "cw_icon.png";
            case "EmergingFlux" -> "ef_icon.png";
            case "Eruption" -> "er_icon.png";
            case "FilamentActivation" -> "fa_icon.png";
            case "FilamentEruption" -> "fe_icon.png";
            case "Filament" -> "fi_icon.png";
            case "Flare" -> "fl_icon.png";
            case "Loop" -> "lp_icon.png";
            case "NothingReported" -> "nr_icon.png";
            case "Oscillation" -> "os_icon.png";
            case "Plage" -> "pg_icon.png";
            case "Sigmoid" -> "sg_icon.png";
            case "SpraySurge" -> "sp_icon.png";
            case "SunSpot" -> "ss_icon.png";
            case "Comesep" -> "comesep.png";
            case "RHESSI" -> "rhessi.png";
            default -> "ot_icon.png";
        };
        return "/images/EventIcons/" + file;
    }

}
