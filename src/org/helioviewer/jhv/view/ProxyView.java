package org.helioviewer.jhv.view;

import java.net.URI;

public class ProxyView extends AbstractView {

    private final URI uri;

    public ProxyView(URI _uri) {
        uri = _uri;
    }

    @Override
    public String getName() {
        String name = uri.getPath();
        return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public String getXMLMetaData() {
        return "<meta/>";
    }

}
