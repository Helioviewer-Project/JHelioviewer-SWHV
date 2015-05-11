package org.helioviewer.viewmodel.imagedata;

import java.awt.image.BufferedImage;

import org.helioviewer.base.Region;
import org.helioviewer.viewmodel.imageformat.ImageFormat;
import org.helioviewer.viewmodel.imagetransport.ImageTransport;
import org.helioviewer.viewmodel.metadata.MetaData;

/**
 * Basic representation of an image handled by the view chain.
 *
 * <p>
 * Within the view chain, the image data is given from view to view as an
 * ImageData object. The object provides only methods to read informations, such
 * as dimensions and pixel data. To write data, a new ImageData object has to be
 * created.
 *
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 *
 */
public interface ImageData {
    /**
     * Returns the width of the image
     *
     * @return width of the image
     */
    public int getWidth();
    /**
     * Returns the height of the image
     *
     * @return height of the image
     */
    public int getHeight();
    /**
     * Returns an object to read the pixel data
     *
     * @return object to read pixels
     */
    public ImageTransport getImageTransport();
    /**
     * Returns an object to get informations about the image format
     *
     * @return object containing informations about the image format
     */
    public ImageFormat getImageFormat();

    public void setFrameNumber(int framenumber);

    public int getFrameNumber();

    public Region getRegion();

    public void setRegion(Region r);

    public MetaData getMETADATA();

    public void setMETADATA(MetaData r);

    public BufferedImage getBufferedImage();

}
