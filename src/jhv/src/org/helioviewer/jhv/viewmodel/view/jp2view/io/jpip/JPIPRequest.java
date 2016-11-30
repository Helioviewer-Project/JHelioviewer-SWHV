package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPMessage;

/**
 * A glorified HTTP request object.
 * 
 * @author caplins
 * @author Juan Pablo
 */
public class JPIPRequest extends HTTPMessage {

    public enum Method {
        GET, POST
    }

    private final Method method;

    /** The query in string form. */
    private String query = null;

    public JPIPRequest(Method _method) {
        method = _method;
    }

    /**
     * This constructor allows to specify directly the initial query.
     * 
     * @param _method
     * @param _query
     */
    public JPIPRequest(Method _method, Object _query) {
        this(_method);
        query = _query.toString();
    }

    public Method getMethod() {
        return method;
    }

    /**
     * Gets the query string.
     * 
     * @return Query String
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the query string.
     * 
     * @param _query
     */
    public void setQuery(Object _query) {
        query = _query.toString();
    }

}
