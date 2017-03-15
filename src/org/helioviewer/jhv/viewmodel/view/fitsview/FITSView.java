package org.helioviewer.jhv.viewmodel.view.fitsview;

import java.awt.image.BufferedImage;
import java.net.URI;

import org.helioviewer.jhv.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelShortImageData;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.view.AbstractView;

public class FITSView extends AbstractView {

    private final FITSImage fits;
    private final URI uri;

    /**
     * Constructor which loads a fits image from a given URI.
     *
     * @param _uri
     *            Specifies the location of the FITS file.
     * */
    public FITSView(URI _uri) throws Exception {
        uri = _uri;
        if (!uri.getScheme().equalsIgnoreCase("file"))
            throw new Exception("FITS does not support the " + uri.getScheme() + " protocol");

        fits = new FITSImage(uri.toURL().toString());
        HelioviewerMetaData m = new HelioviewerMetaData(fits, 0);

        BufferedImage bi = fits.getImage(0, 0, m.getPixelHeight(), m.getPixelWidth());
        if (bi == null)
            throw new Exception("Could not read FITS: " + uri);

        if (bi.getColorModel().getPixelSize() <= 8) {
            imageData = new SingleChannelByte8ImageData(bi);
        } else if (bi.getColorModel().getPixelSize() <= 16) {
            imageData = new SingleChannelShortImageData(bi.getColorModel().getPixelSize(), bi);
        } else {
            imageData = new ARGBInt32ImageData(bi);
        }

        _metaData = m;
        imageData.setRegion(_metaData.getPhysicalRegion());
        imageData.setMetaData(_metaData);
    }

    /**
     * Returns the header information as XML string.
     *
     * @return XML string including all header information.
     * */
    public String getHeaderAsXML() {
        return fits.getHeaderAsXML();
    }

    @Override
    public String getName() {
        if (_metaData instanceof HelioviewerMetaData) {
            return ((HelioviewerMetaData) _metaData).getFullName();
        } else {
            String name = uri.getPath();
            return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        }
    }

    @Override
    public URI getURI() {
        return uri;
    }

}
