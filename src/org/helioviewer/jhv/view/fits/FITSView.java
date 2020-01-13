package org.helioviewer.jhv.view.fits;

import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.BaseView;

public class FITSView extends BaseView {

    private final FITSImage image;

    public FITSView(APIRequest _request, URI _uri) throws Exception {
        super(_request, _uri);

        image = FITSImage.get(uri);
        if (image == null)
            throw new Exception("Could not read FITS: " + uri);

        HelioviewerMetaData m = new XMLMetaDataContainer(image.xmlHeader).getHVMetaData(0, false);
        imageData = new ImageData(image.imageBuffer);
        imageData.setRegion(m.getPhysicalRegion());
        imageData.setMetaData(m);
        metaData[0] = m;
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return image.xmlHeader;
    }

}
