package org.helioviewer.jhv.view.simpleimage;

import java.net.URI;

import javax.annotation.Nonnull;

//import org.helioviewer.jhv.base.XMLUtils;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.DecodeExecutor;

public class SimpleImageView extends BaseView {

    private final String xml;
    private final ImageBuffer imageBuffer;

    public SimpleImageView(DecodeExecutor _executor, APIRequest _request, URI _uri) throws Exception {
        super(_executor, _request, _uri);

        MetaData m;
        String readXml = SimpleImage.getXML(uri);
        if (readXml == null) {
            xml = "<meta/>";
            m = new PixelBasedMetaData(100, 100, 0, uri);
        } else {
            xml = readXml;
            m = new XMLMetaDataContainer(xml).getHVMetaData(0, true);
        }

        imageBuffer = SimpleImage.getImageBuffer(uri);
        metaData[0] = m;
    }

    @Override
    public void decode(Position viewpoint, double pixFactor, double factor) {
        if (dataHandler != null) {
            ImageData data = new ImageData(imageBuffer, metaData[0], metaData[0].getPhysicalRegion(), viewpoint);
            dataHandler.handleData(data);
        }
    }

    @Nonnull
    @Override
    public String getXMLMetaData() {
        return xml;
    }


}
