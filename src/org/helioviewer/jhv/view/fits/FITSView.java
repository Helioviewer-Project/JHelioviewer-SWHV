package org.helioviewer.jhv.view.fits;

import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.BaseView;

public class FITSView extends BaseView {

    private final String xml;

    public FITSView(APIRequest _request, URI _uri) throws Exception {
        super(_request, _uri);

        FITSImage fits = new FITSImage(uri);
        if (fits.imageData == null)
            throw new Exception("Could not read FITS: " + uri);

        xml = fits.xml;

        HelioviewerMetaData m = new XMLMetaDataContainer(xml).getHVMetaData(0, false);
        int w = m.getPixelWidth();
        int h = m.getPixelHeight();
        imageData = fits.imageData;
        imageData.setRegion(m.roiToRegion(new SubImage(0, 0, w, h, w, h), 1, 1));
        imageData.setMetaData(m);

        metaData[0] = m;
    }

    @Nonnull
    @Override
    public String getXMLMetaData(int frame) {
        return xml;
    }

}
