package org.helioviewer.jhv.viewmodel.view.simpleimageview;

import java.awt.image.BufferedImage;
import java.net.URI;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelShortImageData;
import org.helioviewer.jhv.viewmodel.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.viewmodel.view.AbstractView;

public class SimpleImageView extends AbstractView {

    private final URI uri;

    /**
     * Constructor which loads the corresponding image from given URI.
     *
     * @param _uri
     *            URI where the source image is located
     * @throws Exception
     *            if the image is not readable
     */
    public SimpleImageView(URI _uri) throws Exception {
        uri = _uri;

        BufferedImage image = ImageIO.read(uri.toURL());
        if (image == null)
            throw new Exception("Could not read image: " + uri);

        if (image.getColorModel().getPixelSize() <= 8) {
            imageData = new SingleChannelByte8ImageData(image);
        } else if (image.getColorModel().getPixelSize() <= 16) {
            imageData = new SingleChannelShortImageData(image.getColorModel().getPixelSize(), image);
        } else {
            imageData = new ARGBInt32ImageData(image);
        }

        _metaData = new PixelBasedMetaData(image.getWidth(), image.getHeight(), 0);
        imageData.setRegion(_metaData.getPhysicalRegion());
        imageData.setMetaData(_metaData);
    }

    @Override
    public String getName() {
        String name = uri.getPath();
        return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
    }

    @Override
    public URI getURI() {
        return uri;
    }

}
