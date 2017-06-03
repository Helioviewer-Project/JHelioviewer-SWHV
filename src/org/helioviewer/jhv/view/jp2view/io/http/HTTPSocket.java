package org.helioviewer.jhv.view.jp2view.io.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;

import org.helioviewer.jhv.base.ProxySettings;
import org.helioviewer.jhv.view.jp2view.io.LineRead;

public class HTTPSocket extends Socket {

    private static final int TIMEOUT_CONNECT = 20000;
    private static final int TIMEOUT_READ = 20000;
    private static final int PORT = 80;

    private final int lastUsedPort;
    private final String lastUsedHost;

    protected InputStream inputStream;

    protected HTTPSocket(URI uri) throws IOException {
        super(ProxySettings.proxy);

        int port = uri.getPort();
        lastUsedPort = port <= 0 ? PORT : port;
        lastUsedHost = uri.getHost();

        setReceiveBufferSize(Math.max(262144 * 8, 2 * getReceiveBufferSize()));
        setTrafficClass(0x10);
        setSoTimeout(TIMEOUT_READ);
        setKeepAlive(true);
        setTcpNoDelay(true);
        connect(new InetSocketAddress(lastUsedHost, lastUsedPort), TIMEOUT_CONNECT);
        inputStream = new BufferedInputStream(getInputStream(), 65536);
    }

    /**
     * Receives a HTTP message from the socket. Currently it is only supported
     * to receive HTTP responses.
     *
     * @return A new <code>HTTPMessage</code> object with the message read or
     *         <code>null</code> if the end of stream was reached.
     * @throws IOException
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
                    return res;

                parts = line.split(": ", 2);
                if (parts.length != 2)
                    throw new ProtocolException("Invalid HTTP header format");

                res.setHeader(parts[0], parts[1]);
            }
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
