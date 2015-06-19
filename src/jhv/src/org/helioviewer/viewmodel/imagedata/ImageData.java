package org.helioviewer.viewmodel.imagedata;

import java.awt.image.BufferedImage;

import org.helioviewer.base.Region;
import org.helioviewer.viewmodel.imageformat.ImageFormat;
import org.helioviewer.viewmodel.imagetransport.ImageTransport;
import org.helioviewer.viewmodel.metadata.MetaData;

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

    public BufferedImage getBufferedImage();

    public void setMetaData(MetaData m);

    public MetaData getMetaData();

}
