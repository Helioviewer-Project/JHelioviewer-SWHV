package org.helioviewer.jhv.viewmodel.view.fitsview;

import java.net.URI;

import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.viewmodel.view.AbstractView;

public class FITSView extends AbstractView {

    private final String xml;
    private final URI uri;

    public FITSView(URI _uri) throws Exception {
        uri = _uri;
        if (!uri.getScheme().equalsIgnoreCase("file"))
            throw new Exception("FITS does not support the " + uri.getScheme() + " protocol");

        FITSImage fits = new FITSImage(uri.toURL().toString());
        xml = fits.xml;

        XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer();
        hvMetaData.parseXML(xml);
        HelioviewerMetaData m = new HelioviewerMetaData(hvMetaData, 0);
        hvMetaData.destroyXML();

        _metaData = m;
        imageData = fits.imageData;
        imageData.setRegion(_metaData.getPhysicalRegion());
        imageData.setMetaData(_metaData);
    }

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

}
