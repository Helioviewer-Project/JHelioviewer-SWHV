package org.helioviewer.jhv.viewmodel.view.jp2view.io.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;

import org.helioviewer.jhv.base.ProxySettings;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.LineRead;

/**
 * The class <code>HTTPSocket</code> is a simple implementation for read/write
 * HTTP messages. In this version are only supported to send requests and to
 * receive responses.
 *
 * @author Juan Pablo Garcia Ortiz
 * @version 0.1
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
     * @param uri
     * @throws IOException
     */
    protected void _connect(URI uri) throws IOException {
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
    }

    /**
     * Receives a HTTP message from the socket. Currently it is only supported
     * to receive HTTP responses.
     *
     * @return A new <code>HTTPMessage</code> object with the message read or
     *         <code>null</code> if the end of stream was reached.
     * @throws java.io.IOException
     */
    protected HTTPMessage recv() throws IOException {
        String line = LineRead.readAsciiLine(inputStream);
        String parts[] = line.split(" ", 3);
        if (parts.length != 3) {
            throw new ProtocolException("Invalid HTTP message: " + line);
        }

        if (parts[0].startsWith("HTTP/1.1")) {
            // Parses status code
            try {
                int code = Integer.parseInt(parts[1]);
                if (code != 200)
                    throw new IOException("Invalid status code returned (" + code + "): " + parts[2]);
            } catch (NumberFormatException ex) {
                throw new ProtocolException("Invalid HTTP status code format");
            }

            // Parses HTTP headers
            HTTPMessage res = new HTTPMessage();
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
            throw new ProtocolException("Requests received not supported");
        }
    }

    @Override
    public int getPort() {
        return lastUsedPort;
    }

    protected String getHost() {
        return lastUsedHost;
    }

}
