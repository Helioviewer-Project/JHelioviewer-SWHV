package org.helioviewer.jhv.view.uri;

import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.io.DataUri;

final class URIDecodeKey {

    private final DataUri uri;
    private final ImageFilter.Type filter;
    private final int hash;

    URIDecodeKey(DataUri _uri, ImageFilter.Type _filter) {
        uri = _uri;
        filter = _filter;
        hash = 31 * System.identityHashCode(uri) + filter.hashCode();
    }

    DataUri uri() {
        return uri;
    }

    ImageFilter.Type filter() {
        return filter;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof URIDecodeKey other
                && uri == other.uri
                && filter == other.filter;
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
