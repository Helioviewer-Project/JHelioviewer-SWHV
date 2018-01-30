package org.helioviewer.jhv.view.jp2view.io.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.helioviewer.jhv.base.ProxySettings;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.view.jp2view.io.LineRead;

public class HTTPSocket {

    private static final int TIMEOUT_CONNECT = 30000;
    private static final int TIMEOUT_READ = 30000;
    private static final int PORT = 80;

    private final Socket socket;

    protected final int usedPort;
    protected final String usedHost;

    protected final InputStream inputStream;
    private final OutputStream outputStream;

    protected HTTPSocket(URI uri) throws IOException {
        socket = new Socket(ProxySettings.proxy);

        socket.setReceiveBufferSize(Math.max(262144 * 8, 2 * socket.getReceiveBufferSize()));
        socket.setTrafficClass(0x10);
        socket.setSoTimeout(TIMEOUT_READ);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);

        int port = uri.getPort();
        usedPort = port <= 0 ? PORT : port;
        usedHost = uri.getHost();

        socket.connect(new InetSocketAddress(usedHost, usedPort), TIMEOUT_CONNECT);

        inputStream = new BufferedInputStream(socket.getInputStream(), 65536);
        outputStream = socket.getOutputStream();
    }

    protected HTTPMessage recv() throws IOException {
        String line = LineRead.readAsciiLine(inputStream);
        if (!"HTTP/1.1 200 OK".equals(line))
            throw new ProtocolException("Invalid HTTP response: " + line);

        // Parses HTTP headers
        HTTPMessage res = new HTTPMessage();
        while (true) {
            line = LineRead.readAsciiLine(inputStream);
            if (line.isEmpty())
                return res;

            String parts[] = Regex.HttpField.split(line);
            if (parts.length != 2)
                throw new ProtocolException("Invalid HTTP header field: " + line);
            res.setHeader(parts[0], parts[1]);
        }
    }

    protected void write(String str) throws IOException {
        outputStream.write(str.getBytes(StandardCharsets.UTF_8));
    }

    protected void close() throws IOException {
        socket.close();
        inputStream.close();
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

}
