package org.helioviewer.jhv.view.j2k.jpip.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.io.ProxySettings;

public class HTTPSocket {

    private static final int TIMEOUT_CONNECT = 30000;
    private static final int TIMEOUT_READ = 30000;

    private final Socket socket;
    private final InputStream inputStream;
    protected final String httpHeader;

    protected HTTPSocket(URI uri) throws IOException {
        try {
            String host = uri.getHost();
            int port = uri.getPort() <= 0 ? 80 : uri.getPort();

            switch (uri.getScheme().toLowerCase()) {
                case "jpip" -> socket = new Socket(ProxySettings.proxy);
                case "jpips" -> {
                    socket = SSLSocketFactory.getDefault().createSocket();
                    if (socket instanceof SSLSocket sslSocket) { // obviously
                        SSLParameters parameters = sslSocket.getSSLParameters();
                        if (parameters != null) {
                            parameters.setProtocols(new String[]{"TLSv1.3"});
                            parameters.setApplicationProtocols(new String[]{"http/1.1"}); // probably useless
                            parameters.setEndpointIdentificationAlgorithm("HTTPS"); // hope this is performed
                            sslSocket.setSSLParameters(parameters);
                        }
                    }
                }
                default -> throw new IOException("JPIP scheme not supported: " + uri);
            }

            socket.setReceiveBufferSize(Math.max(262144 * 8, 2 * socket.getReceiveBufferSize()));
            socket.setTrafficClass(0x10);
            socket.setSoTimeout(TIMEOUT_READ);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_CONNECT);

            inputStream = socket.getInputStream();

            HTTPMessage msg = new HTTPMessage();
            msg.setHeader("User-Agent", JHVGlobals.userAgent);
            msg.setHeader("Connection", "keep-alive");
            msg.setHeader("Accept-Encoding", "gzip");
            msg.setHeader("Cache-Control", "no-cache");
            msg.setHeader("Host", host + ':' + port);
            httpHeader = " HTTP/1.1\r\n" + msg + "\r\n";
        } catch (Exception e) { // redirect all to IOException
            throw new IOException(e);
        }
    }

    protected InputStream getInputStream(HTTPMessage msg) throws IOException {
        String head = msg.getHeader("Transfer-Encoding");
        String transferEncoding = head == null ? "identity" : head.toLowerCase();
        head = msg.getHeader("Content-Encoding");
        String contentEncoding = head == null ? "identity" : head.toLowerCase();

        InputStream transferStream;
        switch (transferEncoding) {
            case "identity" -> {
                String contentLength = msg.getHeader("Content-Length");
                try {
                    transferStream = new FixedSizedInputStream(inputStream, Integer.parseInt(contentLength));
                } catch (Exception e) {
                    throw new IOException("Invalid Content-Length header: " + contentLength);
                }
            }
            case "chunked" -> transferStream = new ChunkedInputStream(inputStream);
            default -> throw new IOException("Unsupported transfer encoding: " + transferEncoding);
        }

        return switch (contentEncoding) {
            case "identity" -> transferStream;
            case "gzip" -> new GZIPInputStream(transferStream);
            case "deflate" -> new InflaterInputStream(transferStream);
            default -> throw new IOException("Unknown content encoding: " + contentEncoding);
        };
    }

    protected HTTPMessage readHeader() throws IOException {
        String line = LineRead.readAsciiLine(inputStream);
        if (!"HTTP/1.1 200 OK".equals(line))
            throw new IOException("Invalid HTTP response: " + line);

        // Parses HTTP headers
        HTTPMessage res = new HTTPMessage();
        while (true) {
            line = LineRead.readAsciiLine(inputStream);
            if (line.isEmpty())
                return res;

            String[] parts = Regex.HttpField.split(line);
            if (parts.length != 2)
                throw new IOException("Invalid HTTP header field: " + line);
            res.setHeader(parts[0], parts[1]);
        }
    }

    protected void write(String str) throws IOException {
        socket.getOutputStream().write(str.getBytes(StandardCharsets.US_ASCII));
    }

    protected void close() throws IOException {
        socket.close();
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

}
