package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPMessage;

public class JPIPRequest extends HTTPMessage {

    private String query = null;

    public String getQuery() {
        return query;
    }

    public void setQuery(String _query) {
        query = _query;
    }

}
