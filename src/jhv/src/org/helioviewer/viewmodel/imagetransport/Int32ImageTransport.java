package org.helioviewer.viewmodel.imagetransport;

/**
 * Class for reading integer pixel data.
 * 
 * <p>
 * The class manages an integer array, from which the caller can read the pixel
 * data. The array is one-dimensional, the pixel data is ordered line by line.
 * To get height and width of the image, refer to
 * {@link org.helioviewer.viewmodel.imagedata.ImageData}.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public class Int32ImageTransport implements ImageTransport {

    int[] pixelData;

    /**
     * Default constructor.
     * 
     * @param newPixelData
     *            pixel data to manage
     */
    public Int32ImageTransport(int[] newPixelData) {
        pixelData = newPixelData;
    }

    /**
     * Returns managed integer pixel data
     * 
     * @return pixel data
     */
    public int[] getInt32PixelData() {
        return pixelData;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumBitsPerPixel() {
        return 32;
    }

}
