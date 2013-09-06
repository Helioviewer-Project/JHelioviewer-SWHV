package org.helioviewer.jhv.gui.components.tristateCheckbox;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JCheckBox;

/**
 * Very simple hack to the JCheckBox. It allows to set this CheckBox to an
 * indeterminate mode, which draws a 'block' over the checkbox.
 * <p>
 * Since there exist a huge variety of Look-And-Feels for JAVA, and each has to
 * be taken into account individually, this class does a "wonderful" thing: It
 * draws the 'block' nicely centered with nice transparency effects onto the
 * original CheckBox. This way, it should be compatible to (hopefully) all
 * Look-And-Feels, without introducing special cases for each of them.
 * 
 * @author Malte Nuhn
 */
public final class TristateCheckBox extends JCheckBox {

    private static final long serialVersionUID = 1L;

    /**
     * Constant to mark a HEKPath as checked
     */
    final public static int CHECKED = 1;

    /**
     * Constant to mark a HEKPath as unchecked
     */
    final public static int UNCHECKED = 2;

    /**
     * Constant to mark a HEKPath as undeterminated
     */
    final public static int INDETERMINATE = 4;

    /**
     * Constant refering to the default HEKPath state
     */
    final public static int DEFAULT = UNCHECKED;

    /**
     * Size of the (outer) additional rectangle (if the checkbox is in
     * intermediate state)
     * 
     * Note: Only even numbers are possible. Depending on the size of the full
     * checkbox, one pixel may be added to this size
     */
    private static final int largeRectangleSize = 10;

    /**
     * Size of the (inner) additional rectangle (if the checkbox is in
     * intermediate state) This second rectangle is drawn to provide a smoother
     * look
     * 
     * Note: Only even numbers are possible. Depending on the size of the full
     * checkbox, one pixel may be added to this size
     */
    private static final int smallRectangleSize = 6;

    /**
     * This variable defines the opacity of the additionally drawn rectangle (if
     * the checkbox is in intermediate state)
     */
    private static final float rectangleAlpha = 0.3f;

    /**
     * Color of the additional rectangle (if the checkbox is in intermediate
     * state)
     */
    private static final Color rectangleColor = Color.BLACK;

    /**
     * If indeterminate is TRUE, the magic 'block' will be drawn
     */
    boolean indeterminate = false;

    public TristateCheckBox(String caption) {
        super(caption);
    }

    public TristateCheckBox(String caption, int rowState) {
        super(caption);
        setState(rowState);
    }

    private void setState(int rowState) {
        // depending on the rowState set the checkbox state
        if (rowState == TristateCheckBox.CHECKED) {
            this.setSelected(true);
            this.setIndeterminate(false);
        } else if (rowState == TristateCheckBox.UNCHECKED) {
            this.setSelected(false);
            this.setIndeterminate(false);
        } else {
            this.getModel().setSelected(false);
            this.setIndeterminate(true);
        }

    }

    /**
     * Set the indeterminate state
     * 
     * @param indeterminate
     */
    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
    }

    /**
     * Convenience Method to generate AlphaComposite objects from a single float
     * 
     * @param alpha
     *            - alpha value
     * @return AlphaComposite object
     */
    private AlphaComposite makeComposite(float alpha) {
        int type = AlphaComposite.SRC_OVER;
        return (AlphaComposite.getInstance(type, alpha));
    }

    /**
     * Painting routine, which first draws the original component, and finally
     * adds the magic 'block'...
     */
    public void paintComponent(Graphics g) {

        // draw a 'native' checkbox
        super.paintComponent(g);

        // if the checkbox is in intermediate mode, render the additional 'gray
        // box' in the middle
        if (indeterminate) {

            // use graphics2d to add a slightly transparent black box in the
            Graphics2D g2 = (Graphics2D) g;
            g2.setComposite(makeComposite(rectangleAlpha));
            g2.setColor(rectangleColor);

            // calculate the center of checkbox (with respect to having odd/even
            // number of pixels)
            int xCenter, yCenter;

            // depending on having odd/even numbers of pixels, calculate the
            // appropriate width/height of the inner box
            int widthLarge, widthSmall;
            int heightLarge, heightSmall;

            // calculations for x/width
            if (getWidth() % 2 == 0) {
                // even number of pixels
                xCenter = getWidth() / 2;
                widthLarge = largeRectangleSize;
                widthSmall = smallRectangleSize;
            } else {
                // odd number of pixels
                xCenter = (int) Math.floor(getWidth() / 2.0);
                widthLarge = largeRectangleSize + 1;
                widthSmall = smallRectangleSize + 1;
            }

            // calculations for y/height
            if (getHeight() % 2 == 0) {
                // even number of pixels
                yCenter = getHeight() / 2;
                heightLarge = largeRectangleSize;
                heightSmall = smallRectangleSize;
            } else {
                // odd number of pixels
                yCenter = (int) Math.floor(getHeight() / 2.0);
                heightLarge = largeRectangleSize + 1;
                heightSmall = smallRectangleSize + 1;
            }

            // since we already caluclated the appropriate center/width/height
            // we can just draw the rectangles now
            g2.fillRect(xCenter - largeRectangleSize / 2, yCenter - largeRectangleSize / 2, widthLarge, heightLarge);
            g2.fillRect(xCenter - smallRectangleSize / 2, yCenter - smallRectangleSize / 2, widthSmall, heightSmall);
        }

    }
}
