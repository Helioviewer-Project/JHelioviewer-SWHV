package org.helioviewer.jhv.view.simpleimageview;

import java.awt.image.BufferedImage;
import java.net.URI;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.imagedata.ARGBInt32ImageData;
import org.helioviewer.jhv.imagedata.Single8ImageData;
import org.helioviewer.jhv.imagedata.Single16ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.view.AbstractView;

public class SimpleImageView extends AbstractView {

    public SimpleImageView(URI _uri, APIRequest _req) throws Exception {
        super(_uri, _req);

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

        metaData[0] = new PixelBasedMetaData(image.getWidth(), image.getHeight(), 0);
        imageData.setRegion(metaData[0].getPhysicalRegion());
        imageData.setMetaData(metaData[0]);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

}
