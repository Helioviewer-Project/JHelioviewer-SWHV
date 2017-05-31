package org.helioviewer.jhv.viewmodel.view.fitsview;

import java.net.URI;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.viewmodel.view.AbstractView;

public class FITSView extends AbstractView {

    private final String xml;
    private final URI uri;

    public FITSView(URI _uri) throws Exception {
        uri = _uri;
        FITSImage fits = new FITSImage(uri);
        if (fits.imageData == null)
            throw new Exception("Could not read FITS: " + uri);

        xml = fits.xml;
        XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer();
        hvMetaData.parseXML(xml);
        HelioviewerMetaData m = new HelioviewerMetaData(hvMetaData, 0);
        hvMetaData.destroyXML();

        _metaData = m;
        imageData = fits.imageData;

        int w = m.getPixelWidth();
        int h = m.getPixelHeight();
        imageData.setRegion(m.roiToRegion(new SubImage(0, 0, w, h, w, h), 1, 1));
        imageData.setMetaData(m);
    }

    @Override
    public String getXMLMetaData() {
        return xml;
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

    @Override
    public LUT getDefaultLUT() {
        if (_metaData instanceof HelioviewerMetaData) {
            return LUT.get((HelioviewerMetaData) _metaData);
        }
        return null;
    }

}
