package org.helioviewer.jhv.view.fits;

import java.net.URI;

import javax.annotation.Nonnull;

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
        imageData = fits.imageData;

        imageData.setRegion(m.getPhysicalRegion());
        imageData.setMetaData(m);
        metaData[0] = m;
    }

    @Nonnull
    @Override
    public String getXMLMetaData(int frame) {
        return xml;
    }

}
