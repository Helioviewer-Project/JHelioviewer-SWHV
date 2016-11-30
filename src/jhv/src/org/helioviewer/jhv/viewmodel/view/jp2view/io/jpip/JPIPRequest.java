package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPMessage;

public class JPIPRequest extends HTTPMessage {

    public enum Method {
        GET, POST
    }

    private final Method method;

    private String query = null;

    public JPIPRequest(Method _method) {
        method = _method;
    }

    public Method getMethod() {
        return method;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String _query) {
        query = _query;
    }

}
