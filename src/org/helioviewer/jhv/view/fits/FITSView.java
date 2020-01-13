package org.helioviewer.jhv.view.fits;

import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.view.BaseView;

public class FITSView extends BaseView {

    private final FITSImage image;

    public FITSView(APIRequest _request, URI _uri) throws Exception {
        super(_request, _uri);

        image = FITSImage.get(uri);
        if (image == null)
            throw new Exception("Could not read FITS: " + uri);

        metaData[0] = new XMLMetaDataContainer(image.xmlHeader).getHVMetaData(0, false);
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, double factor) {
        if (dataHandler != null) {
            ImageData data = new ImageData(image.imageBuffer, metaData[0], metaData[0].getPhysicalRegion(), viewpoint);
            dataHandler.handleData(data);
        }
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return image.xmlHeader;
    }

}
