package org.helioviewer.jhv.viewmodel.imagedata;

/**
 * Class to represent a color mask.
 * 
 * A color mask is used to use only a subset of the available red, green and
 * blue channel. When drawing the image, the specified channels are omitted.
 * 
 * @author Markus Langenberg
 */
public class ColorMask {

    boolean red, green, blue;

    /**
     * Default constructor.
     * 
     * Sets the mask to (true, true, true)
     */
    public ColorMask() {
        red = true;
        green = true;
        blue = true;
    }

    /**
     * Constructor to set the color mask.
     * 
     * @param showRed
     *            if true, the red channel will be shown.
     * @param showGreen
     *            if true, the green channel will be shown.
     * @param showBlue
     *            if true, the blue channel will be shown.
     */
    public ColorMask(boolean showRed, boolean showGreen, boolean showBlue) {
        red = showRed;
        green = showGreen;
        blue = showBlue;
    }

    /**
     * Returns, whether the red channel should be shown.
     * 
     * @return if true, the red channel should be shown, false otherwise
     */
    public boolean showRed() {
        return red;
    }

    /**
     * Returns, whether the green channel should be shown.
     * 
     * @return if true, the green channel should be shown, false otherwise
     */
    public boolean showGreen() {
        return green;
    }

    /**
     * Returns, whether the blue channel should be shown.
     * 
     * @return if true, the blue channel should be shown, false otherwise
     */
    public boolean showBlue() {
        return blue;
    }

    /**
     * Returns the color mask.
     * 
     * The mask is a integer which can be used to mask a pixel by using the
     * &-operator.
     * 
     * @return The color mask.
     */
    public int getMask() {
        int mask = 0xFF000000;

        if (red) {
            mask |= 0x00FF0000;
        }
        if (green) {
            mask |= 0x0000FF00;
        }
        if (blue) {
            mask |= 0x000000FF;
        }

        return mask;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof ColorMask)) {
            return false;
        }
        ColorMask m = (ColorMask) o;
        return m.red == red && m.green == green && m.blue == blue;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

}
