package org.helioviewer.jhv.plugins.swek;

import java.net.URL;
import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.swing.ImageIcon;

import org.helioviewer.jhv.gui.IconBank;

class SWEKIconBank {

    private static final HashMap<String, ImageIcon> iconBank = new HashMap<>();

    @Nonnull
    static ImageIcon getIcon(String iconName) {
        ImageIcon icon = iconBank.get(iconName);
        icon = icon == null ? iconBank.get("Other") : icon;
        return icon == null ? IconBank.getBlank() : icon;
    }

    private static ImageIcon getImageIcon(String path) throws Exception {
        URL url = SWEKPlugin.class.getResource(path);
        if (url == null)
            throw new Exception("Resource " + path + " not found");
        return new ImageIcon(url);
    }

    static void init() {
        try {
            iconBank.put("ActiveRegion", getImageIcon("/images/EventIcons/ar_icon.png"));
            iconBank.put("CoronalDimming", getImageIcon("/images/EventIcons/cd_icon.png"));
            iconBank.put("CME", getImageIcon("/images/EventIcons/ce_icon.png"));
            iconBank.put("CoronalHole", getImageIcon("/images/EventIcons/ch_icon.png"));
            iconBank.put("CoronalWave", getImageIcon("/images/EventIcons/cw_icon.png"));
            iconBank.put("EmergingFlux", getImageIcon("/images/EventIcons/ef_icon.png"));
            iconBank.put("Eruption", getImageIcon("/images/EventIcons/er_icon.png"));
            iconBank.put("FilamentActivation", getImageIcon("/images/EventIcons/fa_icon.png"));
            iconBank.put("FilamentEruption", getImageIcon("/images/EventIcons/fe_icon.png"));
            iconBank.put("Filament", getImageIcon("/images/EventIcons/fi_icon.png"));
            iconBank.put("Flare", getImageIcon("/images/EventIcons/fl_icon.png"));
            iconBank.put("Loop", getImageIcon("/images/EventIcons/lp_icon.png"));
            iconBank.put("NothingReported", getImageIcon("/images/EventIcons/nr_icon.png"));
            iconBank.put("Oscillation", getImageIcon("/images/EventIcons/os_icon.png"));
            iconBank.put("Other", getImageIcon("/images/EventIcons/ot_icon.png"));
            iconBank.put("Plage", getImageIcon("/images/EventIcons/pg_icon.png"));
            iconBank.put("Sigmoid", getImageIcon("/images/EventIcons/sg_icon.png"));
            iconBank.put("SpraySurge", getImageIcon("/images/EventIcons/sp_icon.png"));
            iconBank.put("SunSpot", getImageIcon("/images/EventIcons/ss_icon.png"));
            iconBank.put("Comesep", getImageIcon("/images/EventIcons/comesep.png"));
            iconBank.put("RHESSI", getImageIcon("/images/EventIcons/rhessi.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
