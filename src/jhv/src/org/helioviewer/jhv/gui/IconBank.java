package org.helioviewer.jhv.gui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import com.jidesoft.icons.IconsFactory;

public class IconBank {

    // The enum has all the icons, you supply these enums to the getIcon method.
    public enum JHVIcon {
        // The formatter will not merge together multiple lines, if at least one empty line is inserted in between
        CLOSED_HAND("ClosedHand.gif"), CLOSED_HAND_MAC("drag.png"),

        HVLOGO_SMALL("hvImage_160x160.png");

        final String fname;

        JHVIcon(String _fname) {
            fname = _fname;
        }

    }

    /**
     * Returns the ImageIcon associated with the given enum
     *
     * @param icon
     *            enum which represents the image
     * @return the image icon of the given enum
     * */
    public static ImageIcon getIcon(JHVIcon icon) {
        return IconsFactory.getImageIcon(IconBank.class, "/images/" + icon.fname);
    }

    public static ImageIcon getIcon(JHVIcon icon, int w, int h) {
        return new ImageIcon(getIcon(icon).getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH));
    }

    /**
     * Returns the Image with the given enum.
     *
     * @param icon
     *            Name of the image which should be loaded
     * @return Image for the given name or null if it fails to load the image.
     * */
    public static BufferedImage getImage(JHVIcon icon) {
        Image image = getIcon(icon).getImage();
        if (image != null && image.getWidth(null) > 0 && image.getHeight(null) > 0) {
            BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = bi.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();

            return bi;
        }
        return null;
    }

}
