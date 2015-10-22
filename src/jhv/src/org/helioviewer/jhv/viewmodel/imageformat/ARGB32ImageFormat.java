package org.helioviewer.jhv.viewmodel.imageformat;

/**
 * Representation of an ARGB32 image format.
 * 
 * <p>
 * This format represents images with four channels (alpha, red, green, blue),
 * with 8 bits per pixel and channel.
 * 
 * <p>
 * For further information on how to use image formats, see {@link ImageFormat}.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public class ARGB32ImageFormat implements RGBImageFormat, AlphaChannelImageFormat {
    private boolean isSingleChannel;

    public void setSingleChannel(boolean isSingleChannel) {
        this.isSingleChannel = isSingleChannel;
    }

    @Override
    public boolean isSingleChannel() {
        return isSingleChannel;
    }
}
