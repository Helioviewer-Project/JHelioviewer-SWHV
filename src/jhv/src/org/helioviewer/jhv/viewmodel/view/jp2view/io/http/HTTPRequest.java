package org.helioviewer.jhv.viewmodel.view.jp2view.io.http;

/**
 * 
 * The class <code>HTTPRequest</code> identifies a HTTP request. Currently it is
 * only supported the <code>GET</code> request.
 * 
 * @author Juan Pablo Garcia Ortiz
 * @see HTTPMessage
 * @version 0.1
 * 
 */
public class HTTPRequest extends HTTPMessage {

    /** An enum identifying the 2 types of HTTPRequests supported. */
    public enum Method {
        GET, POST
    }

    /** The request type */
    private final Method method;

    /** The URI of the object */
    private String uri;

    /**
     * The message body. Since it could potentially be created piecewise I use a
     * string builder to build it incrementally.
     */
    private StringBuilder messageBody = new StringBuilder();

    /**
     * Constructs a new HTTP request indicating the request type.
     * 
     * @throws ProtocolException
     */
    public HTTPRequest(Method _method) {
        method = _method;
    }

    /** Returns the URI of the object requested. */
    public String getURI() {
        return uri;
    }

    /** Sets the URI of the object. */
    public void setURI(String _uri) {
        uri = _uri;
    }

    /** Sets a new message body. */
    public void setMessageBody(String _msg) {
        messageBody = new StringBuilder(_msg);
    }

    /** Appends the specified String to the message body */
    public void appendToMessageBody(String _msg) {
        messageBody.append(_msg);
    }

    /** Returns a String representation of the method body. */
    public String getMessageBody() {
        return messageBody.toString();
    }

    /** Returns the method of the request. */
    public Method getMethod() {
        return method;
    }

    /** This is a request message so this method always returns true. */
    public boolean isRequest() {
        return true;
    }

}
