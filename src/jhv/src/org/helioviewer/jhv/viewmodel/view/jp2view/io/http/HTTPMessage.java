package org.helioviewer.jhv.viewmodel.view.jp2view.io.http;

import java.util.HashMap;
import java.util.Map;

/**
 * The class <code>HTTPMessage</code> defines the basic body of a HTTP message.
 * 
 * @author Juan Pablo Garcia Ortiz
 */
public class HTTPMessage {

    /** A hash table with the headers of the message */
    private final HashMap<String, String> headers = new HashMap<>();

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

    public final void addHeader(String key, String val) {
        headers.putIfAbsent(key, val);
    }

    public final String toString() {
        StringBuilder str = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            str.append(entry.getKey()).append(": ").append(entry.getValue()).append(HTTPConstants.CRLF);
        }
        return str.toString();
    }

}
