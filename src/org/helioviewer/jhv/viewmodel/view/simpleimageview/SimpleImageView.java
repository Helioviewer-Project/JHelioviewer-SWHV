package org.helioviewer.jhv.viewmodel.view.simpleimageview;

import java.awt.image.BufferedImage;
import java.net.URI;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.Single8ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.Single16ImageData;
import org.helioviewer.jhv.viewmodel.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.viewmodel.view.AbstractView;

public class SimpleImageView extends AbstractView {

    private final URI uri;

    public SimpleImageView(URI _uri) throws Exception {
        uri = _uri;

        BufferedImage image = ImageIO.read(uri.toURL());
        if (image == null)
            throw new Exception("Could not read image: " + uri);

        if (image.getColorModel().getPixelSize() <= 8) {
            imageData = new Single8ImageData(image);
        } else if (image.getColorModel().getPixelSize() <= 16) {
            imageData = new Single16ImageData(1, image);
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

    @Override
    public String getXMLMetaData() {
        return "<meta/>";
    }

}
