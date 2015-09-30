package org.helioviewer.viewmodel.view.fitsview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

import org.helioviewer.base.Region;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelShortImageData;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.view.AbstractView;

import com.jogamp.opengl.GL2;

public class FITSView extends AbstractView {

    protected FITSImage fits;
    private final URI uri;

    /**
     * Constructor which loads a fits image from a given URI.
     *
     * @param uri
     *            Specifies the location of the FITS file.
     * @throws IOException
     *             when an error occurred during reading the fits file.
     * */
    public FITSView(URI _uri) throws IOException {
        uri = _uri;
        if (!uri.getScheme().equalsIgnoreCase("file"))
            throw new IOException("FITS does not support the " + uri.getScheme() + " protocol");

        try {
            fits = new FITSImage(uri.toURL().toString());
        } catch (Exception e) {
            throw new IOException("FITS image data cannot be accessed.");
        }

        HelioviewerMetaData m = new HelioviewerMetaData(fits);

        BufferedImage bi = fits.getImage(0, 0, m.getPixelHeight(), m.getPixelWidth());
        if (bi.getColorModel().getPixelSize() <= 8) {
            imageData = new SingleChannelByte8ImageData(bi);
        } else if (bi.getColorModel().getPixelSize() <= 16) {
            imageData = new SingleChannelShortImageData(bi.getColorModel().getPixelSize(), bi);
        } else {
            imageData = new ARGBInt32ImageData(bi);
        }

        metaDataArray[0] = m;
        imageData.setRegion(new Region(m.getPhysicalLowerLeft(), m.getPhysicalSize()));
        imageData.setMetaData(m);
        imageData.setFrameNumber(0);
    }

    /**
     * Returns the header information as XML string.
     *
     * @return XML string including all header information.
     * */
    public String getHeaderAsXML() {
        return fits.getHeaderAsXML();
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public String getName() {
        if (metaDataArray[0] instanceof ObserverMetaData) {
            return ((ObserverMetaData) metaDataArray[0]).getFullName();
        } else {
            String name = uri.getPath();
            return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        }
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public URI getDownloadURI() {
        return uri;
    }

}
