package org.helioviewer.jhv.view.fits;

import java.net.URI;

import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.URIView;

public class FITSView extends URIView {

    public FITSView(DecodeExecutor _executor, APIRequest _request, URI _uri) throws Exception {
        super(_executor, _request, _uri);

        reader = new FITSImage();

        xml = reader.readXML(uri);
        if (xml == null)
            throw new Exception("Could not read FITS: " + uri);

        MetaData m = new XMLMetaDataContainer(xml).getHVMetaData(0, false);
        imageRegion = m.roiToRegion(0, 0, m.getPixelWidth(), m.getPixelHeight(), 1, 1);
        metaData[0] = m;
    }

}
