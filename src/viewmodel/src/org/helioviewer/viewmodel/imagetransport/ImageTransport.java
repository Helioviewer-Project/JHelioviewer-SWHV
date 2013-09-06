package org.helioviewer.viewmodel.imagetransport;

/**
 * Interface for reading pixel data.
 * 
 * <p>
 * This interface provides implementation specific methods to read pixel data.
 * These methods my vary from image format to image format, but they are
 * independent from the image representation in the memory.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public interface ImageTransport {

    /**
     * Returns the number of bits per pixel used.
     * 
     * Typical values are 8 (byte), 16 (short) and 32 (integer)
     * 
     * @return Number of bits per pixel
     */
    public int getNumBitsPerPixel();
}
