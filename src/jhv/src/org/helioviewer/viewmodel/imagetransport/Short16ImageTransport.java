package org.helioviewer.viewmodel.imagetransport;

/**
 * Class for reading short pixel data.
 * 
 * <p>
 * The class manages a short array, from which the caller can read the pixel
 * data. The array is one-dimensional, the pixel data is ordered line by line.
 * To get height and width of the image, refer to
 * {@link org.helioviewer.viewmodel.imagedata.ImageData}.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public class Short16ImageTransport implements ImageTransport {

    short[] pixelData;

    /**
     * Default constructor.
     * 
     * @param newPixelData
     *            pixel data to manage
     */
    public Short16ImageTransport(short[] newPixelData) {
        pixelData = newPixelData;
    }

    /**
     * Returns managed short pixel data
     * 
     * @return pixel data
     */
    public short[] getShort16PixelData() {
        return pixelData;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumBitsPerPixel() {
        return 16;
    }

}
