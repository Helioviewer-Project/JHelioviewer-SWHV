package org.helioviewer.jhv.view.fitsview;

import java.net.URI;

import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.AbstractView;

public class FITSView extends AbstractView {

    private final String xml;

    public FITSView(URI _uri, APIRequest _request) throws Exception {
        super(_uri, _request);

        FITSImage fits = new FITSImage(uri);
        if (fits.imageData == null)
            throw new Exception("Could not read FITS: " + uri);

        xml = fits.xml;
        HelioviewerMetaData m = new XMLMetaDataContainer(xml).getHVMetaData(0);
        metaData[0] = m;

        int w = m.getPixelWidth();
        int h = m.getPixelHeight();
        imageData = fits.imageData;
        imageData.setRegion(m.roiToRegion(new SubImage(0, 0, w, h, w, h), 1, 1));
        imageData.setMetaData(m);
    }

    @Override
    public String getXMLMetaData() {
        return xml;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

}
