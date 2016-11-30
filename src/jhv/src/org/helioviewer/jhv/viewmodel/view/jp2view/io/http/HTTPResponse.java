package org.helioviewer.jhv.viewmodel.view.jp2view.io.http;

/**
 * 
 * The class <code>HTTPResponse</code> identifies a HTTP response.
 * 
 * @author Juan Pablo Garcia Ortiz
 * @see HTTPMessage
 * @version 0.1
 * 
 */
public class HTTPResponse extends HTTPMessage {

    // The status code
    private final int code;

    // The reason phrase
    private final String reason;

    public HTTPResponse(int _code, String _reason) {
        code = _code;
        reason = _reason;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

}
