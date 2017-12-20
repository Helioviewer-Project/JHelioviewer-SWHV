package org.helioviewer.jhv.plugins.swek;

import java.util.HashMap;

import javax.swing.ImageIcon;

class SWEKIconBank {

    private static final HashMap<String, ImageIcon> iconBank = new HashMap<>();

    public static ImageIcon getIcon(String iconName) {
        ImageIcon tempIcon = iconBank.get(iconName);
        return tempIcon == null ? iconBank.get("Other") : tempIcon;
    }

    public static void init() {
        iconBank.put("ActiveRegion", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ar_icon.png")));
        iconBank.put("CoronalDimming", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/cd_icon.png")));
        iconBank.put("CME", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ce_icon.png")));
        iconBank.put("CoronalHole", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ch_icon.png")));
        iconBank.put("CoronalWave", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/cw_icon.png")));
        iconBank.put("EmergingFlux", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ef_icon.png")));
        iconBank.put("Eruption", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/er_icon.png")));
        iconBank.put("FilamentActivation", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/fa_icon.png")));
        iconBank.put("FilamentEruption", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/fe_icon.png")));
        iconBank.put("Filament", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/fi_icon.png")));
        iconBank.put("Flare", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/fl_icon.png")));
        iconBank.put("Loop", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/lp_icon.png")));
        iconBank.put("NothingReported", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/nr_icon.png")));
        iconBank.put("Oscillation", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/os_icon.png")));
        iconBank.put("Other", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ot_icon.png")));
        iconBank.put("Plage", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/pg_icon.png")));
        iconBank.put("Sigmoid", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/sg_icon.png")));
        iconBank.put("SpraySurge", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/sp_icon.png")));
        iconBank.put("SunSpot", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ss_icon.png")));
        iconBank.put("Comesep", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/comesep.png")));
    }

}
