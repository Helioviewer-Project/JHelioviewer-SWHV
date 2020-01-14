package org.helioviewer.jhv.view.simpleimage;

import java.net.URI;

import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.URIView;

public class SimpleImageView extends URIView {

    public SimpleImageView(DecodeExecutor _executor, APIRequest _request, URI _uri) throws Exception {
        super(_executor, _request, _uri);

        reader = new SimpleImage();

        MetaData m;
        String readXml = reader.readXML(uri);
        if (readXml == null) {
            xml = "<meta/>";
            m = new PixelBasedMetaData(100, 100, 0, uri);
        } else {
            xml = readXml;
            m = new XMLMetaDataContainer(xml).getHVMetaData(0, true);
        }
        metaData[0] = m;
    }

}
