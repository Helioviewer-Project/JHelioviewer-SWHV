package org.helioviewer.viewmodel.imagetransport;

/**
 * Class for reading byte pixel data.
 * 
 * <p>
 * The class manages a byte array, from which the caller can read the pixel
 * data. The array is one-dimensional, the pixel data is ordered line by line.
 * To get height and width of the image, refer to
 * {@link org.helioviewer.viewmodel.imagedata.ImageData}.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public class Byte8ImageTransport implements ImageTransport {

    byte[] pixelData;

    /**
     * Default constructor.
     * 
     * @param newPixelData
     *            pixel data to manage
     */
    public Byte8ImageTransport(byte[] newPixelData) {
        pixelData = newPixelData;
    }

    /**
     * Returns managed byte pixel data
     * 
     * @return pixel data
     */
    public byte[] getByte8PixelData() {
        return pixelData;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumBitsPerPixel() {
        return 8;
    }

}
