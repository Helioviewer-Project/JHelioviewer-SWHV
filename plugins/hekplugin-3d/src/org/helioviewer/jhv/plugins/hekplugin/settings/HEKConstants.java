package org.helioviewer.jhv.plugins.hekplugin.settings;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.plugins.hekplugin.HEKPlugin;

public class HEKConstants {

    // the sole instance of this class
    private static final HEKConstants singletonInstance = new HEKConstants();

    public static final String ACRONYM_FALLBACK = "OT"; // other

    public static final String HEK_SUMMARY_URL = "http://www.lmsal.com/hek/her?cmd=view-voevent&ivorn=";

    private SimpleDateFormat hekDateFormat;
    private HashMap<String, String> acronymStrings;

    private AbstractMap<String, ImageIcon> smallIcons = new HashMap<String, ImageIcon>();
    private AbstractMap<String, BufferedImage> smallBufImgs = new HashMap<String, BufferedImage>();
    private AbstractMap<String, BufferedImage> smallOverlayBufImgs = new HashMap<String, BufferedImage>();

    private AbstractMap<String, ImageIcon> largeIcons = new HashMap<String, ImageIcon>();
    private AbstractMap<String, BufferedImage> largeBufImgs = new HashMap<String, BufferedImage>();
    private AbstractMap<String, BufferedImage> largeOverlayBufImgs = new HashMap<String, BufferedImage>();
    private AbstractMap<String, Color> colors = new HashMap<String, Color>();

    public static final Interval<Date> DEFAULT_EMPTY_INTERVAL = new Interval<Date>(new Date(0), new Date(0));

    /**
     * Setup all other BufferedImages needed
     * 
     * @throws IOException
     */
    private void setupBufImgs() throws IOException {
        smallOverlayBufImgs.put("HUMAN", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/human-flag-small.png")));
        smallOverlayBufImgs.put("LOADING", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/loading-flag-small.png")));
        smallOverlayBufImgs.put("QUEUED", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/queued-flag-small.png")));
        smallOverlayBufImgs.put("HEK", ImageIO.read(HEKPlugin.getResourceUrl("/images/hekLogoSmall.png")));
        largeOverlayBufImgs.put("HUMAN", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/human-flag.png")));
        largeOverlayBufImgs.put("LOADING", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/loading-flag.png")));
        largeOverlayBufImgs.put("QUEUED", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/queued-flag.png")));
        largeOverlayBufImgs.put("HEK", ImageIO.read(HEKPlugin.getResourceUrl("/images/hekLogo.png")));
    }

    /**
     * Setup Strings explaining acronyms used by the HEK API
     * 
     * @throws IOException
     */
    private void setupEventTypes() {

        Log.debug("HEKConstants -> Setting up event acronyms...");

        acronymStrings = new HashMap<String, String>();

        acronymStrings.put("AR", "Active Region");
        acronymStrings.put("CE", "CME");
        acronymStrings.put("CC", "Coronal Cavity");
        acronymStrings.put("CD", "Coronal Dimming");
        acronymStrings.put("CH", "Coronal Hole");
        acronymStrings.put("CW", "Coronal Wave");
        acronymStrings.put("CR", "Coronal Rain");
        acronymStrings.put("ER", "Curious Polar Eruption");
        acronymStrings.put("FI", "Filament");
        acronymStrings.put("FE", "Filament Eruption");
        acronymStrings.put("FA", "Filament Activation");
        acronymStrings.put("FL", "Flare");
        acronymStrings.put("LP", "Loop");
        acronymStrings.put("OS", "Oscillation");
        acronymStrings.put("SS", "Sunspot");
        acronymStrings.put("EF", "Emerging Flux");
        acronymStrings.put("CJ", "Coronal Jet");
        acronymStrings.put("PG", "Plage");
        acronymStrings.put("OT", "Other");
        acronymStrings.put("NR", "Nothing Reported");
        acronymStrings.put("SG", "Sigmoid");
        acronymStrings.put("SP", "Spray Surge");

        Log.info("HEKConstants -> Setting up event acronyms... Done.");

    }

    /**
     * Preload ImageIcons representing acronyms used by the HEK API
     * 
     * @throws IOException
     */
    private void setupEventIcons() {
        // TODO: Malte Nuhn - Use IconBank (wait for newest version)

        Log.debug("HEKConstants -> Setting up event icons...");

        smallIcons.put("AR", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ar_small.png")));
        smallIcons.put("BP", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/bp_small.png")));
        smallIcons.put("CD", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/cd_small.png")));
        smallIcons.put("CE", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ce_small.png")));
        smallIcons.put("CH", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ch_small.png")));
        smallIcons.put("CJ", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/cj_small.png")));
        smallIcons.put("CW", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/cw_small.png")));
        smallIcons.put("EF", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ef_small.png")));
        smallIcons.put("FA", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/fa_small.png")));
        smallIcons.put("FE", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/fe_small.png")));
        smallIcons.put("FI", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/fi_small.png")));
        smallIcons.put("FL", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/fl_small.png")));
        smallIcons.put("LP", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/lp_small.png")));
        smallIcons.put("NR", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/nr_small.png")));
        smallIcons.put("OS", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/os_small.png")));
        smallIcons.put("OT", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ot_small.png")));
        smallIcons.put("SG", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/sg_small.png")));
        smallIcons.put("SP", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/sp_small.png")));
        smallIcons.put("SS", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ss_small.png")));

        largeIcons.put("AR", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ar_icon.png")));
        largeIcons.put("BP", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/bp_icon.png")));
        largeIcons.put("CD", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/cd_icon.png")));
        largeIcons.put("CE", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ce_icon.png")));
        largeIcons.put("CH", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ch_icon.png")));
        largeIcons.put("CJ", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/cj_icon.png")));
        largeIcons.put("CW", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/cw_icon.png")));
        largeIcons.put("EF", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ef_icon.png")));
        largeIcons.put("FA", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/fa_icon.png")));
        largeIcons.put("FE", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/fe_icon.png")));
        largeIcons.put("FI", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/fi_icon.png")));
        largeIcons.put("FL", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/fl_icon.png")));
        largeIcons.put("LP", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/lp_icon.png")));
        largeIcons.put("NR", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/nr_icon.png")));
        largeIcons.put("OS", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/os_icon.png")));
        largeIcons.put("OT", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ot_icon.png")));
        largeIcons.put("SG", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/sg_icon.png")));
        largeIcons.put("SP", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/sp_icon.png")));
        largeIcons.put("SS", new ImageIcon(HEKPlugin.getResourceUrl("/images/EventIcons/ss_icon.png")));

        // eventIcons.put("PH", new
        // ImageIcon(HEKEventRenderer.class.getResource("./resources/big/ph_small.png")));

        Log.info("HEKConstants -> Setting up event icons... Done.");

    }

    /**
     * Preload BufferedImages representing acronyms used by the HEK API
     * 
     * @throws IOException
     */
    private void setupEventBufImgs() throws IOException {

        Log.debug("HEKConstants -> Setting up event buffered images...");
        // load the icons for the different event types

        smallBufImgs.put("AR", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ar_small.png")));
        smallBufImgs.put("BP", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/bp_small.png")));
        smallBufImgs.put("CD", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/cd_small.png")));
        smallBufImgs.put("CE", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ce_small.png")));
        smallBufImgs.put("CH", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ch_small.png")));
        smallBufImgs.put("CJ", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/cj_small.png")));
        smallBufImgs.put("CW", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/cw_small.png")));
        smallBufImgs.put("EF", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ef_small.png")));
        smallBufImgs.put("FA", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/fa_small.png")));
        smallBufImgs.put("FE", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/fe_small.png")));
        smallBufImgs.put("FI", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/fi_small.png")));
        smallBufImgs.put("FL", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/fl_small.png")));
        smallBufImgs.put("LP", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/lp_small.png")));
        smallBufImgs.put("NR", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/nr_small.png")));
        smallBufImgs.put("OS", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/os_small.png")));
        smallBufImgs.put("OT", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ot_small.png")));
        smallBufImgs.put("SG", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/sg_small.png")));
        smallBufImgs.put("SP", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/sp_small.png")));
        smallBufImgs.put("SS", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ss_small.png")));

        largeBufImgs.put("AR", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ar_icon.png")));
        largeBufImgs.put("BP", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/bp_icon.png")));
        largeBufImgs.put("CD", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/cd_icon.png")));
        largeBufImgs.put("CE", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ce_icon.png")));
        largeBufImgs.put("CH", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ch_icon.png")));
        largeBufImgs.put("CJ", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/cj_icon.png")));
        largeBufImgs.put("CW", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/cw_icon.png")));
        largeBufImgs.put("EF", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ef_icon.png")));
        largeBufImgs.put("FA", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/fa_icon.png")));
        largeBufImgs.put("FE", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/fe_icon.png")));
        largeBufImgs.put("FI", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/fi_icon.png")));
        largeBufImgs.put("FL", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/fl_icon.png")));
        largeBufImgs.put("LP", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/lp_icon.png")));
        largeBufImgs.put("NR", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/nr_icon.png")));
        largeBufImgs.put("OS", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/os_icon.png")));
        largeBufImgs.put("OT", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ot_icon.png")));
        largeBufImgs.put("SG", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/sg_icon.png")));
        largeBufImgs.put("SP", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/sp_icon.png")));
        largeBufImgs.put("SS", ImageIO.read(HEKPlugin.getResourceUrl("/images/EventIcons/ss_icon.png")));

        // if we are missing some colors, take the average color of the icon
        for (String key : smallBufImgs.keySet()) {

            if (!colors.containsKey(key)) {

                colors.put(key, IconBank.getAverageColor(smallBufImgs.get(key)));
            }
        }

        // eventIcons.put("PH",
        // ImageIO.read(HEKEventRenderer.class.getResource("./resources/big/ph_small.png")));

        Log.info("HEKConstants -> Setting up event buffered images... Done.");

    }

    /**
     * The private constructor to support the singleton pattern.
     * */
    private HEKConstants() {
        try {
            hekDateFormat = new SimpleDateFormat(HEKSettings.API_DATE_FORMAT);
            setupEventTypes();
            setupEventIcons();
            setupEventBufImgs();
            setupBufImgs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static HEKConstants getSingletonInstance() {
        return singletonInstance;
    }

    /**
     * Retrieve the default color for the given acronym This method is NOT
     * case-sensitive.
     * 
     * @see org.helioviewer.jhv.plugins.overlay.hek.settings.HEKConstants#colors
     * @param input
     *            - acronym
     * @return - default color, or white if no color was found
     */
    public Color acronymToColor(String input, int alpha) {

        String type = input.toUpperCase();

        if (this.colors.containsKey(type)) {
            Color color = colors.get(type);
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        } else {
            return new Color(255, 255, 255, alpha);
        }

    }

    /**
     * Converts acronyms used in the HEK API into human readable descriptions
     * <p>
     * This method is NOT case-sensitive.
     * 
     * @see org.helioviewer.jhv.plugins.overlay.hek.settings.HEKConstants#acronymStrings
     * @param input
     *            - acronym
     * @return - human readable description
     */
    public String acronymToString(String input) {

        String type = input.toUpperCase();

        if (this.acronymStrings.containsKey(type)) {
            return acronymStrings.get(type);
        }

        return input;

    }

    /**
     * Converts acronyms used in the HEK API into ImageIcons representing that
     * acronym
     * <p>
     * This method is NOT case-sensitive.
     * 
     * @see org.helioviewer.jhv.plugins.overlay.hek.settings.HEKConstants#smallIcons
     * @param input
     * @return - null if the acronym is not known
     */
    public ImageIcon acronymToIcon(String input, boolean large) {

        String type = input.toUpperCase();

        if (large) {
            if (this.largeIcons.containsKey(type)) {
                return largeIcons.get(type);
            }
        } else {
            if (this.smallIcons.containsKey(type)) {
                return smallIcons.get(type);
            }
        }

        return null;

    }

    /**
     * Converts acronyms used in the HEK API into BufferedImages representing
     * that acronym
     * <p>
     * This method is NOT case-sensitive.
     * 
     * @see org.helioviewer.jhv.plugins.overlay.hek.settings.HEKConstants#smallBufImgs
     * @param input
     * @param large
     * @return - null if the acronym is not known
     */
    public BufferedImage acronymToBufferedImage(String input, boolean large) {

        String type = input.toUpperCase();

        if (large) {
            if (this.largeBufImgs.containsKey(type)) {
                return largeBufImgs.get(type);
            }
        } else {
            if (this.smallBufImgs.containsKey(type)) {
                return smallBufImgs.get(type);
            }
        }

        return null;

    }

    /**
     * Get the DateFormat needed to parse Dates provided by the HEK API
     * 
     * @return SimpleDateFormat ready to parse date strings provided by the HEK
     *         API
     */
    public SimpleDateFormat getDateFormat() {
        return hekDateFormat;
    }

    public AbstractMap<String, BufferedImage> getAcronymBufImgs() {
        return smallBufImgs;
    }

    public BufferedImage getOverlayBufferedImage(String input, boolean large) {
        if (large) {
            return largeOverlayBufImgs.get(input.toUpperCase());
        } else {
            return smallOverlayBufImgs.get(input.toUpperCase());
        }
    }

}
