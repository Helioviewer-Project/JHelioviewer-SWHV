package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.Buffer;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.viewmodel.imageformat.ImageFormat;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

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

    public Rectangle getROI();

    public void setROI(final Rectangle r);

    public int getBitsPerPixel();

    public Buffer getBuffer();

    /**
     * Returns an object to get informations about the image format
     *
     * @return object containing informations about the image format
     */
    public ImageFormat getImageFormat();

    public Region getRegion();

    public void setRegion(Region r);

    public BufferedImage getBufferedImage();

    public void setMetaData(MetaData m);

    public MetaData getMetaData();

    public void setViewpoint(Position.Q p);

    public Position.Q getViewpoint();

    public boolean getUploaded();

    public void setUploaded(boolean uploaded);

}
