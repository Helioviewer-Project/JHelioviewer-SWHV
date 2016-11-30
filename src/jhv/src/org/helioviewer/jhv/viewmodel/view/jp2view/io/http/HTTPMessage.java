package org.helioviewer.jhv.viewmodel.view.jp2view.io.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The class <code>HTTPMessage</code> defines the basic body of a HTTP message.
 * 
 * @author Juan Pablo Garcia Ortiz
 * @see HTTPRequest
 * @see HTTPResponse
 * @see HTTPSocket
 * @version 0.1
 * 
 */
public abstract class HTTPMessage {

    /** A hash table with the headers of the message */
    private final HashMap<String, String> headers = new HashMap<>();

    /** Returns <code>true</code> if the message is a request. */
    public abstract boolean isRequest();

    /** Returns <code>true</code> if the message is a response. */
    public final boolean isResponse() {
        return !isRequest();
    }

    /**
     * Returns the value of a message header.
     * 
     * @param key
     *            The header name.
     * @return The value of the specified header or <code>null</code> if it was
     *         not found.
     */
    public final String getHeader(String key) {
        return headers.get(key);
    }

    public final String getHeader(HTTPHeaderKey key) {
        return headers.get(key.toString());
    }

    /**
     * Sets a new value for a specific HTTP message header. If the header does
     * not exists, it will be added to the header list of the message.
     * 
     * @param key
     *            Header name.
     * @param val
     *            Header value.
     */
    public final void setHeader(String key, String val) {
        headers.put(key, val);
    }

    public final void addHeader(HTTPHeaderKey key, String val) {
        headers.putIfAbsent(key.toString(), val);
    }

    /**
     * Returns a <code>Set<String></code> with all the headers keys. This set
     * backs the headers, so don't change anything.
     */
    public final Set<String> getHeaders() {
        return headers.keySet();
    }

}
