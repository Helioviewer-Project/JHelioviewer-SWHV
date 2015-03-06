package org.helioviewer.jhv.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.net.URL;

import javax.swing.ImageIcon;

import org.helioviewer.base.FileUtils;

/**
 * This class provides access to all images, icons and cursors which are used by
 * the program.
 * 
 * @author caplins
 * @author dmueller
 * @author Stephan Pagel
 */
public class IconBank {

    /**
     * The enum has all the icons, you supply these enums to the getIcon method.
     * 
     * @author caplins cleaned up unused icons
     * @author dmueller
     * 
     */
    public static enum JHVIcon {

        // The formatter will not merge together multiple lines, if at least one
        // empty line is inserted in between:

        PROPERTIES("properties16.gif"), BLANK("blank_square.gif"), ADD("edit_add.png"), DOWNLOAD("download_dm.png"),

        // MOVIE CONTROLS
        PLAY("play_dm.png"), PAUSE("agt_pause-queue.png"), BACK("agt_back.png"), FORWARD("agt_forward.png"),

        // ZOOMING
        ZOOM_IN("zoomIn24.png"), ZOOM_IN_SMALL("zoomIn24small.png"),

        ZOOM_FIT("zoomFit24.png"), ZOOM_FIT_SMALL("zoomFit24small.png"),

        ZOOM_OUT("zoomOut24.png"), ZOOM_OUT_SMALL("zoomOut24small.png"),

        ZOOM_1TO1("zoom1to124.png"), ZOOM_1TO1_SMALL("zoom1to124small.png"),

        // ARROWS
        LEFT("arrow_left.gif"), RIGHT("arrow_right.gif"), UP("1uparrow1.png"), DOWN("1downarrow1.png"), RIGHT2("arrow.plain.right.gif"), DOWN2("arrow.plain.down.gif"),

        // MOUSE POINTERS

        OPEN_HAND("OpenedHand.gif"), OPEN_HAND_SMALL("OpenedHand2.gif"), CLOSED_HAND("ClosedHand.gif"),

        PAN("pan24x24.png"), PAN_SELECTED("pan_selected24x24.png"),

        SELECT("select24x24.png"), SELECT_SELECTED("select_selected24x24.png"),

        FOCUS("crosshairs24x24.png"), FOCUS_SELECTED("crosshairs_checked24x24.png"),

        // MISC ICONS

        VISIBLE("layer_visible_dm.png"), HIDDEN("layer_invisible_dm.png"),

        REMOVE_LAYER("button_cancel.png"), INFO("info.png"),

        CHECK("button_ok.png"), EX("button_cancel.png"), RUBBERBAND("rubberband.gif"), NOIMAGE("NoImageLoaded_256x256.png"),

        CONNECTED("connected_dm.png"), DISCONNECTED("not_connected_dm.png"),

        MOVIE_LINK("unlocked.png"), MOVIE_UNLINK("locked.png"),

        SPLASH("jhv_splash.png"), HVLOGO_SMALL("hvImage_160x160.png"),

        SIMPLE_ARROW_RIGHT("Arrow-Right.png"), SIMPLE_ARROW_LEFT("Arrow-Left.png"),

        SIMPLE_DOUBLEARROW_RIGHT("DoubleArrow-Right.png"),

        SIMPLE_DOUBLEARROW_LEFT("DoubleArrow-Left.png"),

        DATE("date.png"),

        SHOW_LESS("1uparrow1.png"), SHOW_MORE("1downarrow1.png"),

        INVERT("invert.png"), LOADING_BIG("Loading_256x256.png"), LOADING_SMALL("Loading_219x50.png"),

        // 3D Icons
        MODE_3D("3D_24x24.png"), MODE_2D("2D_24x24.png"), MODE_3D_SELECTED("3D_selected_24x24.png"), MODE_2D_SELECTED("2D_selected_24x24.png"), RESET("Reset_24x24.png"), ROTATE("Rotate_24x24.png"), ROTATE_SELECTED("Rotate_selected_24x24.png"),

        // LAYER ICONS
        LAYER_IMAGE("layer-image.png"), LAYER_IMAGE_OFF("layer-image-off.png"), LAYER_IMAGE_TIME("layer-image-time.png"), LAYER_IMAGE_TIME_MASTER("layer-image-time-master.png"), LAYER_IMAGE_TIME_OFF("layer-image-time-off.png"), LAYER_MOVIE("layer-movie.png"), LAYER_MOVIE_OFF("layer-movie-off.png"), LAYER_MOVIE_TIME("layer-movie-time.png"), LAYER_MOVIE_TIME_MASTER("layer-movie-time-master.png"), LAYER_MOVIE_TIME_OFF("layer-movie-time-off.png");

        private final String fname;

        JHVIcon(String _fname) {
            fname = _fname;
        }

        String getFilename() {
            return fname;
        }
    };

    /** The location of the image files relative to this folder. */
    private static final String RESOURCE_PATH = "/images/";

    /**
     * Returns the ImageIcon associated with the given enum
     * 
     * @param _icon
     *            enum which represents the image
     * @return the image icon of the given enum
     * */
    public static ImageIcon getIcon(JHVIcon _icon) {
        URL imgURL = FileUtils.getResourceUrl(RESOURCE_PATH + _icon.getFilename());
        return new ImageIcon(imgURL);
    }

    /**
     * Returns the Image with the given enum.
     * 
     * @param icon
     *            Name of the image which should be loaded
     * @return Image for the given name or null if it fails to load the image.
     * */
    public static BufferedImage getImage(JHVIcon icon) {

        ImageIcon imageIcon = getIcon(icon);

        if (imageIcon == null)
            return null;

        Image image = imageIcon.getImage();

        if (image != null && image.getWidth(null) > 0 && image.getHeight(null) > 0) {
            BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

            Graphics g = bi.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();

            return bi;
        }

        return null;
    }

    public static Color getAverageColor(BufferedImage bufImg) {
        long sumRed = 0;
        long sumGreen = 0;
        long sumBlue = 0;

        for (int x = 0; x < bufImg.getWidth(); x++) {
            for (int y = 0; y < bufImg.getHeight(); y++) {
                Color curColor = new Color(bufImg.getRGB(x, y), true);
                sumRed += curColor.getRed();
                sumGreen += curColor.getGreen();
                sumBlue += curColor.getBlue();
            }
        }

        int totalPixels = bufImg.getWidth() * bufImg.getHeight();

        float red = ((float) (sumRed / totalPixels)) / 255.0f;
        float green = ((float) (sumGreen / totalPixels)) / 255.0f;
        float blue = ((float) (sumBlue / totalPixels)) / 255.0f;

        // Log.info("RGB: " + red + " " + green + " " + blue);

        return new Color(red, green, blue);

    }

    public static BufferedImage stackImages(BufferedImage[] bufImgs, double horizontal, double vertical) {

        // exit if no real image data is available
        if (bufImgs.length == 0 || bufImgs[0] == null) {
            return null;
        }

        // the first layer is strictly copied
        BufferedImage result = bufImgs[0];

        ColorModel cm = result.getColorModel();
        boolean isAlphaPremultiplied = result.isAlphaPremultiplied();
        WritableRaster raster = result.copyData(null);

        result = new BufferedImage(cm, raster, isAlphaPremultiplied, null);

        int width = result.getWidth();
        int height = result.getHeight();

        Graphics2D gbi = result.createGraphics();

        for (int i = 1; i < bufImgs.length; i++) {
            BufferedImage currentImg = bufImgs[i];

            int offsetX = (int) (horizontal * (double) (width - currentImg.getWidth()));
            int offsetY = (int) (vertical * (double) (height - currentImg.getHeight()));
            gbi.drawImage(currentImg, null, offsetX, offsetY);
        }

        return result;

    }

}
