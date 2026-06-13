package org.helioviewer.jhv.view.j2k.jpip.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.helioviewer.jhv.app.AppInfo;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.io.ProxySettings;

public class HTTPSocket {

    private static final int TIMEOUT_CONNECT = 60000;
    private static final int TIMEOUT_READ = 60000;

    private final Socket socket;
    private final InputStream inputStream;
    protected final String httpHeader;

    protected HTTPSocket(URI uri) throws IOException {
        Socket openSocket = null;
        try {
            String host = uri.getHost();

            int port;
            switch (uri.getScheme().toLowerCase()) {
                case "jpip" -> {
                    port = uri.getPort() <= 0 ? 80 : uri.getPort();
                    openSocket = new Socket(ProxySettings.proxy);
                }
                case "jpips" -> {
                    port = uri.getPort() <= 0 ? 443 : uri.getPort();
                    openSocket = SSLSocketFactory.getDefault().createSocket();
                    if (openSocket instanceof SSLSocket sslSocket) { // obviously
                        SSLParameters parameters = sslSocket.getSSLParameters();
                        parameters.setProtocols(new String[]{"TLSv1.3"});
                        parameters.setApplicationProtocols(new String[]{"http/1.1"}); // probably useless
                        parameters.setEndpointIdentificationAlgorithm("HTTPS"); // hope this is performed
                        sslSocket.setSSLParameters(parameters);
                    }
                }
                default -> throw new IOException("JPIP scheme not supported: " + uri);
            }

            openSocket.setReceiveBufferSize(Math.max(262144 * 8, 2 * openSocket.getReceiveBufferSize()));
            openSocket.setTrafficClass(0x10);
            openSocket.setSoTimeout(TIMEOUT_READ);
            openSocket.setKeepAlive(true);
            openSocket.setTcpNoDelay(true);
            openSocket.connect(new InetSocketAddress(host, port), TIMEOUT_CONNECT);

            InputStream openInputStream = new BufferedInputStream(openSocket.getInputStream());

            HashMap<String, String> hdr = new HashMap<>();
            hdr.put("User-Agent", AppInfo.userAgent);
            hdr.put("Connection", "keep-alive");
            hdr.put("Accept-Encoding", "gzip");
            hdr.put("Cache-Control", "no-cache");
            hdr.put("Host", host + ':' + port);

            StringBuilder sb = new StringBuilder();
            hdr.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\r\n"));
            String header = " HTTP/1.1\r\n" + sb + "\r\n";

            socket = openSocket;
            inputStream = openInputStream;
            httpHeader = header;
        } catch (Exception e) { // redirect all to IOException
            if (openSocket != null)
                try {
                    openSocket.close();
                } catch (IOException ignore) {
                }
            throw new IOException(e);
        }
    }

    protected InputStream getInputStream(Map<String, String> hdr) throws IOException {
        String head = hdr.get("Transfer-Encoding");
        String transferEncoding = head == null ? "identity" : head.toLowerCase();
        head = hdr.get("Content-Encoding");
        String contentEncoding = head == null ? "identity" : head.toLowerCase();

        InputStream transferStream;
        switch (transferEncoding) {
            case "identity" -> {
                String contentLength = hdr.get("Content-Length");
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

    protected Map<String, String> readHeader() throws IOException {
        String line = LineRead.readAsciiLine(inputStream);
        if (!"HTTP/1.1 200 OK".equals(line))
            throw new IOException("Invalid HTTP response: " + line);

        // Parses HTTP headers
        Map<String, String> hdr = new HashMap<>();
        while (true) {
            line = LineRead.readAsciiLine(inputStream);
            if (line.isEmpty())
                return hdr;

            String[] parts = Regex.HttpField.split(line);
            if (parts.length != 2)
                throw new IOException("Invalid HTTP header field: " + line);
            hdr.put(parts[0], parts[1]);
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
