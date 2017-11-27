package org.helioviewer.jhv.view.fitsview;

import java.net.URI;

import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.AbstractView;

public class FITSView extends AbstractView {

    private final String xml;

    public FITSView(URI _uri, APIRequest _req) throws Exception {
        super(_uri, _req);

        FITSImage fits = new FITSImage(uri);
        if (fits.imageData == null)
            throw new Exception("Could not read FITS: " + uri);

        xml = fits.xml;
        XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer();
        hvMetaData.parseXML(xml);
        HelioviewerMetaData m = new HelioviewerMetaData(hvMetaData, 0);
        hvMetaData.destroyXML();

        metaData[0] = m;
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

}
