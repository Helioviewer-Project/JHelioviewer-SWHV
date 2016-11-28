package org.helioviewer.jhv.viewmodel.view.jp2view.io.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.helioviewer.jhv.base.ProxySettings;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.LineRead;

/**
 *
 * The class <code>HTTPSocket</code> is a simple implementation for read/write
 * HTTP messages. In this version are only supported to send requests and to
 * receive responses.
 *
 * @author Juan Pablo Garcia Ortiz
 * @see java.net.Socket
 * @see HTTPResponse
 * @see HTTPRequest
 * @version 0.1
 *
 */
public class HTTPSocket extends Socket {

    /** The last used port */
    private int lastUsedPort = 0;

    /** The last used host */
    private String lastUsedHost = null;

    /** The default port for the HTTP socket */
    private static final int PORT = 80;

    private static final int TO_CONNECT = 30000;
    private static final int TO_READ = 30000;

    protected InputStream inputStream;

    protected HTTPSocket() {
        super(ProxySettings.proxy);
    }

    /**
     * Connects to the specified host via the supplied URI.
     *
     * @param _uri
     * @throws IOException
     */
    protected Object connect(URI uri) throws IOException {
        int port = uri.getPort();
        lastUsedPort = port <= 0 ? PORT : port;
        lastUsedHost = uri.getHost();

        setReceiveBufferSize(Math.max(262144 * 8, 2 * getReceiveBufferSize()));
        setTrafficClass(0x10);
        setSoTimeout(TO_READ);
        setKeepAlive(true);
        setTcpNoDelay(true);
        connect(new InetSocketAddress(lastUsedHost, lastUsedPort), TO_CONNECT);

        inputStream = new BufferedInputStream(getInputStream(), 65536);

        return null;
    }

    /**
     * Sends a HTTP message. Only HTTP requests supported
     *
     * @param _msg
     *            A <code>HTTPMessage</code> object with the message.
     * @throws java.io.IOException
     */
    protected void send(HTTPRequest req) throws IOException {
        StringBuilder str = new StringBuilder();
        // Adds the URI line
        str.append(req.getMethod()).append(' ').append(req.getURI()).append(' ').append(HTTPConstants.versionText).append(HTTPConstants.CRLF);

        String msgBody = req.getMessageBody();
        // Sets the content length header if it's a POST
        if (req.getMethod() == HTTPRequest.Method.POST)
            req.setHeader(HTTPHeaderKey.CONTENT_LENGTH.toString(), Integer.toString(msgBody.getBytes(StandardCharsets.UTF_8).length));

        // Adds the headers
        for (String key : req.getHeaders()) {
            str.append(key).append(": ").append(req.getHeader(key)).append(HTTPConstants.CRLF);
        }
        str.append(HTTPConstants.CRLF);

        // Adds the message body if it's a POST
        if (req.getMethod() == HTTPRequest.Method.POST)
            str.append(msgBody);

        // Writes the result to the output stream
        getOutputStream().write(str.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Receives a HTTP message from the socket. Currently it is only supported
     * to receive HTTP responses.
     *
     * @return A new <code>HTTPMessage</code> object with the message read or
     *         <code>null</code> if the end of stream was reached.
     * @throws java.io.IOException
     */
    protected HTTPMessage receive() throws IOException {
        String line = LineRead.readAsciiLine(inputStream);
        String parts[] = line.split(" ", 3);
        if (parts.length != 3) {
            throw new ProtocolException("Invalid HTTP message: " + line);
        }

        if (parts[0].startsWith("HTTP/")) {
            // Parses HTTP version
            double ver;
            try {
                ver = Double.parseDouble(parts[0].substring(5));
            } catch (NumberFormatException ex) {
                throw new ProtocolException("Invalid HTTP version format");
            }
            if (ver < 1 || ver > HTTPConstants.version)
                throw new ProtocolException("HTTP version not supported");

            // Parses status code
            int code;
            try {
                code = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                throw new ProtocolException("Invalid HTTP status code format");
            }

            // Instantiates new HTTPResponse
            HTTPResponse res = new HTTPResponse(code, parts[2]);
            // Parses HTTP headers
            while (true) {
                line = LineRead.readAsciiLine(inputStream);
                if (line.isEmpty())
                    break;

                parts = line.split(": ", 2);
                if (parts.length != 2)
                    throw new ProtocolException("Invalid HTTP header format");

                res.setHeader(parts[0], parts[1]);
            }
            return res;
        } else {
            throw new ProtocolException("Requests receiving not yet supported");
        }
    }

    /** Returns the lastUsedPort */
    @Override
    public int getPort() {
        return lastUsedPort;
    }

    /** Returns the lastUsedHost */
    protected String getHost() {
        return lastUsedHost;
    }

}
