package org.helioviewer.viewmodel.imageformat;

/**
 * Representation of a single channel image format.
 * 
 * <p>
 * This format represents images with only one single channel. The bits per
 * pixel may vary and can be set during construction.
 * 
 * <p>
 * For further information on how to use image formats, see {@link ImageFormat}.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public class SingleChannelImageFormat implements ImageFormat {

    int bitDepth;

    /**
     * Default constructor.
     * 
     * @param newBitDepth
     *            bits per pixel, usually between eight and sixteen.
     */
    public SingleChannelImageFormat(int newBitDepth) {
        bitDepth = newBitDepth;
    }

    /**
     * Returns bits per pixel
     * 
     * @return Bits per pixel
     */
    public int getBitDepth() {
        return bitDepth;
    }

}
