package org.helioviewer.viewmodel.view.simpleimageview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import javax.imageio.ImageIO;

import org.helioviewer.base.Region;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelShortImageData;
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
    protected BufferedImage image;

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
        image = ImageIO.read(uri.toURL());
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
    public JHVSimpleImageView(BufferedImage _image, URI _uri) {
        uri = _uri;
        image = _image;
        initSimpleImageView();
    }

    private void initSimpleImageView() {
        if (image.getColorModel().getPixelSize() <= 8) {
            imageData = new SingleChannelByte8ImageData(image);
        } else if (image.getColorModel().getPixelSize() <= 16) {
            imageData = new SingleChannelShortImageData(image.getColorModel().getPixelSize(), image);
        } else {
            imageData = new ARGBInt32ImageData(image);
        }

        metaDataArray[0] = new PixelBasedMetaData(image.getWidth(), image.getHeight());
        region = new Region(-1.5, -1.5, 3., 3.);
        imageData.setRegion(region);
        imageData.setMetaData(metaDataArray[0]);
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
