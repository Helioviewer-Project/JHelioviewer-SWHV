package org.helioviewer.viewmodel.imagedata;

import java.awt.image.BufferedImage;

/**
 * Extension of ImageData, representing an image that can be converted into a
 * Java Buffered Image.
 * 
 * <p>
 * Since BufferedImage is very common image representation in Java, it might be
 * helpful to get the pixel data as a BufferedImage instead of an array, because
 * the user might want to use the Java Graphics or Graphics2D object.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public interface JavaBufferedImageData extends ImageData {

    /**
     * Returns the pixel data as a BufferedImage.
     * 
     * <p>
     * Note, that all implementations of this function implement a lazy
     * behavior, which means that the BufferedImage is first generated the
     * moment the user requested it. The creation of the image might take some
     * time, depending on size and format of the underlying image data. Thus:
     * <p>
     * <b>It is highly recommended to only this function, if absolute
     * necessary.</b><br>
     * If possible, use {@link #getImageTransport()} instead.
     * 
     * @return pixel data
     */
    public BufferedImage getBufferedImage();

}
