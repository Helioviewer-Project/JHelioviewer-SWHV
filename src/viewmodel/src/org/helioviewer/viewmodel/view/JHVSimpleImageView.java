package org.helioviewer.viewmodel.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;

import javax.imageio.ImageIO;

import org.helioviewer.base.math.Interval;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.RegionUpdatedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ColorMask;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelShortImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.PixelBasedMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * Implementation of ImageInfoView for simple image formats.
 *
 * <p>
 * Currently, the view supports JPG and PNG images.
 *
 * <p>
 * For further informations about the behavior of this view,
 * {@link ImageInfoView} is a good start to get into the concept.
 *
 * @author Andreas Hoelzl
 *
 */
public class JHVSimpleImageView extends AbstractView implements ViewportView, RegionView, MetaDataView, SubimageDataView, ImageInfoView {

    protected URI uri;
    protected Viewport viewport;
    protected Region region;
    protected ImageData subImageData;
    protected BufferedImage bufferedImage;
    protected PixelBasedMetaData pixelBasedMetaData;
    private final Interval<Date> range;

    /**
     * Constructor which loads the corresponding image from given URI.
     *
     * @param _uri
     *            URI where the source image is located
     * @throws MalformedURLException
     *             thrown, if the location is not valid
     * @throws IOException
     *             thrown, if the image is not readable
     */
    public JHVSimpleImageView(URI _uri, Interval<Date> range) throws MalformedURLException, IOException {
        this.range = range;
        uri = _uri;
        bufferedImage = ImageIO.read(uri.toURL());

        initSimpleImageView();
    }

    /**
     * Constructor which uses a given buffered image.
     *
     * @param image
     *            Buffered image object which contains the image data.
     * @param uri
     *            Specifies the location of the simple image file.
     */
    public JHVSimpleImageView(BufferedImage image, URI uri, Interval<Date> range) {
        this.range = range;
        this.uri = uri;
        bufferedImage = image;

        initSimpleImageView();
    }

    /**
     * Initializes global variables.
     */
    private void initSimpleImageView() {
        if (bufferedImage.getColorModel().getPixelSize() <= 8) {
            subImageData = new SingleChannelByte8ImageData(bufferedImage, new ColorMask());
        } else if (bufferedImage.getColorModel().getPixelSize() <= 16) {
            subImageData = new SingleChannelShortImageData(bufferedImage.getColorModel().getPixelSize(), bufferedImage, new ColorMask());
        } else {
            subImageData = new ARGBInt32ImageData(bufferedImage, new ColorMask());
        }

        pixelBasedMetaData = new PixelBasedMetaData(bufferedImage.getWidth(), bufferedImage.getHeight());

        region = StaticRegion.createAdaptedRegion(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

        viewport = StaticViewport.createAdaptedViewport(100, 100);

        updateImageData(new ChangeEvent());
    }

    public BufferedImage getSimpleImage() {
        return bufferedImage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Viewport getViewport() {
        return viewport;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setViewport(Viewport v, ChangeEvent event) {
        // check if viewport has changed
        if (viewport != null && v != null && viewport.getWidth() == v.getWidth() && viewport.getHeight() == v.getHeight())
            return false;

        viewport = v;
        event.addReason(new ViewportChangedReason(this, v));
        notifyViewListeners(event);

        return true;
    }

    /**
     * Recalculates the image data by copying the desired region out of the full
     * image.
     *
     * @param event
     *            ChangeEvent to fire after the new data is available
     */
    protected void updateImageData(ChangeEvent event) {
        int width = (int) (bufferedImage.getWidth() * region.getWidth() / pixelBasedMetaData.getPhysicalImageSize().getX());
        int height = (int) (bufferedImage.getHeight() * region.getHeight() / pixelBasedMetaData.getPhysicalImageSize().getY());
        int x = (int) ((region.getCornerX() - pixelBasedMetaData.getPhysicalLowerLeft().getX()) / pixelBasedMetaData.getPhysicalImageWidth() * bufferedImage.getWidth());
        int y = (int) ((region.getCornerY() - pixelBasedMetaData.getPhysicalLowerLeft().getY()) / pixelBasedMetaData.getPhysicalImageHeight() * bufferedImage.getHeight());
        if (width > 0 && height > 0) {
            BufferedImage bI = new BufferedImage(width, height, bufferedImage.getType());
            bI.getGraphics().drawImage(bufferedImage.getSubimage(x, bufferedImage.getHeight() - height - y, width, height), 0, 0, null);

            if (bI.getColorModel().getPixelSize() <= 8) {
                subImageData = new SingleChannelByte8ImageData(bI, new ColorMask());
            } else if (bI.getColorModel().getPixelSize() <= 16) {
                subImageData = new SingleChannelShortImageData(bI.getColorModel().getPixelSize(), bI, new ColorMask());
            } else {
                subImageData = new ARGBInt32ImageData(bI, new ColorMask());
            }
        } else {
            subImageData = null;
        }

        event.addReason(new SubImageDataChangedReason(this));
        notifyViewListeners(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends View> T getAdapter(Class<T> c) {
        if (c.isInstance(this)) {
            return (T) this;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Region getRegion() {
        return region;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setRegion(Region r, ChangeEvent event) {

        event.addReason(new RegionUpdatedReason(this, r));

        // check if region has changed
        if ((region == r) || (region != null && r != null && region.getCornerX() == r.getCornerX() && region.getCornerY() == r.getCornerY() && region.getWidth() == r.getWidth() && region.getHeight() == r.getHeight()))
            return false;

        region = r;
        event.addReason(new RegionChangedReason(this, r));
        updateImageData(event);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaData getMetaData() {
        return pixelBasedMetaData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageData getSubimageData() {
        return subImageData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        String name = uri.getPath();
        return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getUri() {
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public URI getDownloadURI() {
        return uri;
    }

    @Override
    public Interval<Date> getDateRange() {
        return this.range;
    }

    @Override
    public MetaData getMetadata() {
        // TODO Auto-generated method stub
        return pixelBasedMetaData;
    }

    @Override
    public void setDateRange(Interval<Date> range) {
        // TODO Auto-generated method stub
    }

}
