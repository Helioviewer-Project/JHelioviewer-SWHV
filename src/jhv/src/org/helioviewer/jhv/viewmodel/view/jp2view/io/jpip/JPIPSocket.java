package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JPIPCache;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.ChunkedInputStream;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.FixedSizedInputStream;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.TransferInputStream;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPMessage;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPSocket;

// Assumes a persistent HTTP connection.
public class JPIPSocket extends HTTPSocket {

    // The jpip channel ID for the connection (persistent)
    private final String jpipChannelID;

    /**
     * The path supplied on the uri line of the HTTP message. Generally for the
     * first request it is the image path in relative terms, but the response
     * could change it. The Kakadu server seems to change it to /jpip.
     */
    private String jpipPath;

    private static final String[] cnewParams = { "cid", "transport", "host", "path", "port", "auxport" };

    public JPIPSocket(URI uri, JPIPCache cache) throws IOException {
        connect(uri);

        jpipPath = uri.getPath();

        send(JPIPQuery.create(512, "cnew", "http", "type", "jpp-stream", "tid", "0")); // deliberately short
        JPIPResponse res = receive(cache);

        String cnew = res.getCNew();
        if (cnew == null)
            throw new IOException("The header 'JPIP-cnew' was not sent by the server");

        HashMap<String, String> map = new HashMap<>();
        String[] parts = cnew.split(",");
        for (String part : parts)
            for (String cnewParam : cnewParams)
                if (part.startsWith(cnewParam + '='))
                    map.put(cnewParam, part.substring(cnewParam.length() + 1));

        jpipPath = '/' + map.get("path");
        jpipChannelID = map.get("cid");
        if (jpipChannelID == null)
            throw new IOException("The channel id was not sent by the server");

        if (!"http".equals(map.get("transport")))
            throw new IOException("The client only supports HTTP transport");
    }

    // Closes the JPIPSocket
    @Override
    public synchronized void close() throws IOException {
        if (isClosed())
            return;

        try {
            if (jpipChannelID != null) {
                send(JPIPQuery.create(0, "cclose", jpipChannelID));
            }
        } catch (IOException ignore) {
            // no problem, server may have closed the socket
        } finally {
            super.close();
        }
    }

    // Sends a JPIP request
    public void send(String queryStr) throws IOException {
        // Add a necessary JPIP request field
        if (jpipChannelID != null && !queryStr.contains("cid=") && !queryStr.contains("cclose"))
            queryStr += "&cid=" + jpipChannelID;

        // Build request to send
        HTTPMessage req = new HTTPMessage();
        req.setHeader("User-Agent", JHVGlobals.getUserAgent());
        req.setHeader("Connection", "Keep-Alive");
        req.setHeader("Accept-Encoding", "gzip");
        req.setHeader("Cache-Control", "no-cache");
        req.setHeader("Host", getHost() + ':' + getPort());
        String res = "GET " + jpipPath + '?' + queryStr + " HTTP/1.1\r\n" + req + "\r\n";

        // Writes the result to the output stream
        getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
    }

    // Receives a JPIPResponse returning null if EOS reached
    public JPIPResponse receive(JPIPCache cache) throws IOException {
        HTTPMessage res = recv();
        if (!"image/jpp-stream".equals(res.getHeader("Content-Type")))
            throw new IOException("Expected image/jpp-stream content");

        TransferInputStream transferInput;
        String head = res.getHeader("Transfer-Encoding");
        String transferEncoding = head == null ? "" : head.toLowerCase();
        switch (transferEncoding) {
            case "":
            case "identity":
                String contentLength = res.getHeader("Content-Length");
                try {
                    transferInput = new FixedSizedInputStream(inputStream, Integer.parseInt(contentLength));
                } catch (Exception e) {
                    throw new IOException("Invalid Content-Length header: " + contentLength);
                }
                break;
            case "chunked":
                transferInput = new ChunkedInputStream(inputStream);
                break;
            default:
                throw new IOException("Unsupported transfer encoding: " + transferEncoding);
        }

        InputStream input = transferInput;
        head = res.getHeader("Content-Encoding");
        String contentEncoding = head == null ? "" : head.toLowerCase();
        switch (contentEncoding) {
            case "":
            case "identity":
                break;
            case "gzip":
                input = new GZIPInputStream(input);
                break;
            case "deflate":
                input = new InflaterInputStream(input);
                break;
            default:
                throw new IOException("Unknown content encoding: " + contentEncoding);
        }

        JPIPResponse jpipRes = new JPIPResponse(res.getHeader("JPIP-cnew"));
        try {
            jpipRes.readSegments(input, cache);
        } finally {
            input.close(); // make sure the stream is exhausted
        }

        if ("close".equals(res.getHeader("Connection"))) {
            super.close();
        }

        return jpipRes;
    }

}
