package org.helioviewer.jhv.plugins.swek.view;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.helioviewer.jhv.plugins.swek.SWEKPlugin;

/**
 * An Icon bank for the SWEK plugin.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKIconBank {
    /** The instance of the SWEK icon bank */
    private static SWEKIconBank singletonInstance;

    /** the icon bank */
    private final Map<String, Icon> iconBank;

    /**
     * Private default constructor.
     */
    private SWEKIconBank() {
        iconBank = new HashMap<String, Icon>();
        initIconBank();
    }

    /**
     * Gets the singleton instance of the SWEK icon bank.
     * 
     * @return the instance of the SWEK icon bank
     */
    public static SWEKIconBank getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new SWEKIconBank();
        }
        return singletonInstance;
    }

    /**
     * Adds a new Icon to the icon bank.
     * 
     * @param eventType
     *            the event type for which an icon was added
     * @param icon
     *            the icon corresponding to the event
     */
    public void addIcon(String eventType, ImageIcon icon) {
        iconBank.put(eventType, icon);
    }

    /**
     * Initializes the icon bank. Adds all the standard icons to the iconbank.
     */
    private void initIconBank() {
        iconBank.put("ActiveRegion", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ar_icon.png")));
        iconBank.put("CoronalDimming", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/cd_icon.png")));
        iconBank.put("CME", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ce_icon.png")));
        iconBank.put("CoronalHole", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ch_icon.png")));
        iconBank.put("CoronalWave", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/cw_icon.png")));
        iconBank.put("EmergingFlux", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ef_icon.png")));
        iconBank.put("FilamentActivation", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/fa_icon.png")));
        iconBank.put("FilamentEruption", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/fe_icon.png")));
        iconBank.put("Filament", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/fi_icon.png")));
        iconBank.put("Flare", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/fl_icon.png")));
        iconBank.put("Loop", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/lp_icon.png")));
        iconBank.put("NothingReported", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/nr_icon.png")));
        iconBank.put("Oscillation", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/os_icon.png")));
        iconBank.put("Other", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ot_icon.png")));
        iconBank.put("Sigmoid", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/sg_icon.png")));
        iconBank.put("SpraySurge", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/sp_icon.png")));
        iconBank.put("SunSpot", new ImageIcon(SWEKPlugin.class.getResource("/images/EventIcons/ss_icon.png")));
    }
}
