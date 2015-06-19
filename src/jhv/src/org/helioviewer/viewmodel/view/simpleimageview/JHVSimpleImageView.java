package org.helioviewer.viewmodel.view.simpleimageview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import javax.imageio.ImageIO;

import org.helioviewer.base.Region;
import org.helioviewer.base.Viewport;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelShortImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.PixelBasedMetaData;
import org.helioviewer.viewmodel.view.AbstractView;

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
public class JHVSimpleImageView extends AbstractView {

    protected URI uri;
    protected Viewport viewport;
    protected Region region;
    protected BufferedImage bufferedImage;
    protected PixelBasedMetaData m;

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
    public JHVSimpleImageView(URI _uri) throws MalformedURLException, IOException {
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
    public JHVSimpleImageView(BufferedImage image, URI _uri) {
        uri = _uri;
        bufferedImage = image;
        initSimpleImageView();
    }

    /**
     * Initializes global variables.
     */
    private void initSimpleImageView() {
        if (bufferedImage.getColorModel().getPixelSize() <= 8) {
            imageData = new SingleChannelByte8ImageData(bufferedImage);
        } else if (bufferedImage.getColorModel().getPixelSize() <= 16) {
            imageData = new SingleChannelShortImageData(bufferedImage.getColorModel().getPixelSize(), bufferedImage);
        } else {
            imageData = new ARGBInt32ImageData(bufferedImage);
        }

        m = new PixelBasedMetaData(bufferedImage.getWidth(), bufferedImage.getHeight());
        region = new Region(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        viewport = new Viewport(100, 100);

        updateImageData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setViewport(Viewport v) {
        boolean changed = (viewport == null ? v == null : !viewport.equals(v));
        viewport = v;
        return changed;
    }

    /**
     * Recalculates the image data by copying the desired region out of the full
     * image.
     */
    protected void updateImageData() {
        int width = (int) (bufferedImage.getWidth() * region.getWidth() / m.getPhysicalSize().x);
        int height = (int) (bufferedImage.getHeight() * region.getHeight() / m.getPhysicalSize().y);
        int x = (int) ((region.getLowerLeftCorner().x - m.getPhysicalLowerLeft().x) / m.getPhysicalSize().x * bufferedImage.getWidth());
        int y = (int) ((region.getLowerLeftCorner().y - m.getPhysicalLowerLeft().y) / m.getPhysicalSize().y * bufferedImage.getHeight());
        if (width > 0 && height > 0) {
            BufferedImage bI = new BufferedImage(width, height, bufferedImage.getType());
            bI.getGraphics().drawImage(bufferedImage.getSubimage(x, bufferedImage.getHeight() - height - y, width, height), 0, 0, null);

            if (bI.getColorModel().getPixelSize() <= 8) {
                imageData = new SingleChannelByte8ImageData(bI);
            } else if (bI.getColorModel().getPixelSize() <= 16) {
                imageData = new SingleChannelShortImageData(bI.getColorModel().getPixelSize(), bI);
            } else {
                imageData = new ARGBInt32ImageData(bI);
            }
            region = new Region(-1.5, -1.5, 3., 3.);
            imageData.setRegion(region);
            imageData.setMetaData(m);
        } else {
            imageData = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setRegion(Region r) {
        boolean changed = region == null ? r == null : !region.equals(r);
        region = r;
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaData getMetaData() {
        return m;
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

    @Override
    public URI getDownloadURI() {
        return uri;
    }

}
