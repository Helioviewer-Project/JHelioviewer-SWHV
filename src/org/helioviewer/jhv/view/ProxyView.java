package org.helioviewer.jhv.view;

import java.net.URI;

public class ProxyView extends AbstractView {

    public ProxyView(URI _uri) {
        uri = _uri;
    }

    @Override
    public String getXMLMetaData() {
        return "<meta/>";
    }

}
