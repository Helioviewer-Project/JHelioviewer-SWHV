package org.helioviewer.viewmodel.view.jp2view.io.jpip;

import org.helioviewer.viewmodel.view.jp2view.io.http.HTTPRequest;

/**
 * A glorified HTTP request object.
 * 
 * @author caplins
 * @author Juan Pablo
 */
public class JPIPRequest extends HTTPRequest {
    /** The query in string form. */
    private String query = null;

    /**
     * Default constructor.
     * 
     * @param _method
     * @throws ProtocolException
     */
    public JPIPRequest(Method _method) {
        super(_method);
    }

    /**
     * This constructor allows to specify directly the initial query.
     * 
     * @param _method
     * @param _query
     */
    public JPIPRequest(Method _method, Object _query) {
        super(_method);

        query = _query.toString();
    }

    /** Method does nothing. */
    public void setURI(String _uri) {
    }

    /** Method does nothing... returns null. */
    public String getURI() {
        return null;
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
