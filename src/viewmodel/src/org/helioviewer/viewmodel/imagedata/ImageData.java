package org.helioviewer.viewmodel.imagedata;

import org.helioviewer.viewmodel.imageformat.ImageFormat;
import org.helioviewer.viewmodel.imagetransport.ImageTransport;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;

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

    /**
     * Returns the color mask which should be used when drawing the image.
     *
     * @return the color mask which should be used when drawing the image.
     */
    public ColorMask getColorMask();

    public long getDateMillis();

    public void setDateMillis(long dateMillis);

    public void setFrameNumber(int framenumber);
    public int getFrameNumber();
    public void setZoomPercent(double percent);
    public double getZoomPercent();
    public void setSubImage(SubImage subImage);
    public SubImage getSubImage();
    public double getScaleX();
    public void setScaleX(double scaleX);
    public double getScaleY();
    public void setScaleY(double scaleY);
    public Region getRegion();
    public void setRegion(Region r);


}
