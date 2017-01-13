package org.helioviewer.jhv.viewmodel.imagedata;

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
    int getWidth();
    /**
     * Returns the height of the image
     *
     * @return height of the image
     */
    int getHeight();

    int getBitsPerPixel();

    Buffer getBuffer();

    /**
     * Returns an object to get informations about the image format
     *
     * @return object containing informations about the image format
     */
    ImageFormat getImageFormat();

    Region getRegion();

    void setRegion(Region r);

    BufferedImage getBufferedImage();

    void setMetaData(MetaData m);

    MetaData getMetaData();

    void setViewpoint(Position.Q p);

    Position.Q getViewpoint();

    boolean getUploaded();

    void setUploaded(boolean uploaded);

    float getAutoContrast();

    void setAutoContrast(float factor);

}
