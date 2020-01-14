package org.helioviewer.jhv.view.fits;

import java.awt.EventQueue;
import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.view.BaseView;

public class FITSView extends BaseView {

    private static final FITSDecodeExecutor executor = new FITSDecodeExecutor();

    private final String xml;

    public FITSView(APIRequest _request, URI _uri) throws Exception {
        super(_request, _uri);

        xml = FITSImage.getHeader(uri);
        if (xml == null)
            throw new Exception("Could not read FITS: " + uri);
        metaData[0] = new XMLMetaDataContainer(xml).getHVMetaData(0, false);
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, double factor) {
        executor.decode(this, viewpoint);
    }

    void setDataFromDecoder(ImageBuffer imageBuffer, Position viewpoint) {
        ImageData data = new ImageData(imageBuffer, metaData[0], metaData[0].getPhysicalRegion(), viewpoint);
        EventQueue.invokeLater(() -> {
            if (dataHandler != null)
                dataHandler.handleData(data);
        });
    }

    @Override
    public void abolish() {
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xml;
    }

}
