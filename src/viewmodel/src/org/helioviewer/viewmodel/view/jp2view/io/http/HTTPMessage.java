package org.helioviewer.viewmodel.view.jp2view.io.http;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * 
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

    /** The maximum HTTP version supported */
    static public final double version = 1.1;

    /** The version in standard formated text */
    static public final String versionText = "HTTP/" + Double.toString(version);

    /** The array of bytes that contains the CRLF codes */
    static public final byte CRLFBytes[] = { 13, 10 };

    /** The string representation of the CRLF codes */
    static public final String CRLF = new String(CRLFBytes);

    /** A hash table with the headers of the message */
    protected Hashtable<String, String> headers = new Hashtable<String, String>();

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

    /**
     * Same as setHeader except for a map of headers.
     * 
     * @param _map
     */
    public final void setHeaders(Map<String, String> _map) {
        headers.putAll(_map);
    }

    /**
     * Returns a <code>Set<String></code> with all the headers keys. This set
     * backs the headers, so don't change anything.
     */
    public final Set<String> getHeaders() {
        return headers.keySet();
    }

    /**
     * Tests to see if a header exists.
     * 
     * @param _key
     * @return True, if the header exists, false otherwise
     */
    public final boolean headerExists(String _key) {
        return headers.containsKey(_key);
    }

    /**
     * Removes the specified message header.
     * 
     * @param _key
     */
    public final void removeHeader(String _key) {
        headers.remove(_key);
    }

    /** Removes all the message headers. */
    public final void clearHeaders() {
        headers.clear();
    }
};
