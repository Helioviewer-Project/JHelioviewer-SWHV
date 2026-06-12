package org.helioviewer.jhv.view.j2k;

import org.helioviewer.jhv.image.ImageFilter;

final class J2KDecodeKey {

    private final J2KParams.Decode params;
    private final ImageFilter.Type filter;
    private final int serial;
    private final int hash;

    J2KDecodeKey(int _serial, J2KParams.Decode _params, ImageFilter.Type _filter) {
        params = _params;
        filter = _filter;
        serial = _serial;

        int ret = 17;
        ret = 31 * ret + serial;
        ret = 31 * ret + params.hashCode();
        ret = 31 * ret + filter.hashCode();
        hash = ret;
    }

    J2KParams.Decode params() {
        return params;
    }

    int serial() {
        return serial;
    }

    ImageFilter.Type filter() {
        return filter;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof J2KDecodeKey other
                && serial == other.serial
                && params.equals(other.params)
                && filter == other.filter;
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
