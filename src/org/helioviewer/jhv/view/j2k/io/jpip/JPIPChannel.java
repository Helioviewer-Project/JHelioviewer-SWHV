package org.helioviewer.jhv.view.j2k.io.jpip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;

import kdu_jni.KduException;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.view.j2k.io.ChunkedInputStream;
import org.helioviewer.jhv.view.j2k.io.FixedSizedInputStream;
import org.helioviewer.jhv.view.j2k.io.TransferInputStream;
import org.helioviewer.jhv.view.j2k.io.http.HTTPChannel;
import org.helioviewer.jhv.view.j2k.io.http.HTTPMessage;

// Assumes a persistent HTTP connection
public class JPIPChannel extends HTTPChannel {

    // The jpip channel ID for the connection (persistent)
    private final String jpipChannelID;

    // private int totalLength = 0;

    // The path supplied on the uri line of the HTTP message. Generally for the
    // first request it is the image path in relative terms, but the response
    // could change it. The Kakadu server seems to change it to /jpip.
    private String jpipPath;

    private static final String[] cnewParams = {"cid", "transport", "host", "path", "port", "auxport"};

    public JPIPChannel(URI uri, JPIPCache cache) throws KduException, IOException {
        super(uri);

        jpipPath = uri.getPath();

        JPIPResponse res = send(JPIPQuery.create(512, "cnew", "http", "type", "jpp-stream", "tid", "0"), cache, 0); // deliberately short
        String cnew = res.getCNew();
        if (cnew == null)
            throw new IOException("The header 'JPIP-cnew' was not sent by the server");

        HashMap<String, String> map = new HashMap<>();
        for (String part : Regex.Comma.split(cnew))
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

    // Closes the JPIPChannel
    @Override
    public void close() throws IOException {
        if (isClosed())
            return;

        // System.out.println(">>> total MB: " + (totalLength / (double) (1024 * 1024)));
        try {
            if (jpipChannelID != null)
                send(JPIPQuery.create(0, "cclose", jpipChannelID));
        } catch (IOException ignore) {
            // no problem, server may have closed the socket
        } finally {
            super.close();
        }
    }

    private void send(String queryStr) throws IOException {
        // Add a necessary JPIP request field
        if (jpipChannelID != null && !queryStr.contains("cid=") && !queryStr.contains("cclose"))
            queryStr += "&cid=" + jpipChannelID;

        // Build request to send
        HTTPMessage req = new HTTPMessage();
        req.setHeader("User-Agent", JHVGlobals.userAgent);
        req.setHeader("Connection", "keep-alive");
        req.setHeader("Accept-Encoding", "gzip");
        req.setHeader("Cache-Control", "no-cache");
        req.setHeader("Host", host);
        queryStr = "GET " + jpipPath + '?' + queryStr + " HTTP/1.1\r\n" + req + "\r\n";
        write(queryStr);
    }

    public JPIPResponse send(String queryStr, JPIPCache cache, int frame) throws KduException, IOException {
        send(queryStr);
        HTTPMessage res = recv();
        if (!"image/jpp-stream".equals(res.getHeader("Content-Type")))
            throw new IOException("Expected image/jpp-stream content");

        String head = res.getHeader("Transfer-Encoding");
        String transferEncoding = head == null ? "" : head.toLowerCase();
        head = res.getHeader("Content-Encoding");
        String contentEncoding = head == null ? "" : head.toLowerCase();

        TransferInputStream transferInput;
        switch (transferEncoding) {
            case "", "identity" -> {
                String contentLength = res.getHeader("Content-Length");
                try {
                    transferInput = new FixedSizedInputStream(inputStream, Integer.parseInt(contentLength));
                } catch (Exception e) {
                    throw new IOException("Invalid Content-Length header: " + contentLength);
                }
            }
            case "chunked" -> transferInput = new ChunkedInputStream(inputStream);
            default -> throw new IOException("Unsupported transfer encoding: " + transferEncoding);
        }

        InputStream input = transferInput;
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
        try (InputStream in = input) {
            jpipRes.readSegments(in, cache, frame);
        }
        // totalLength += transferInput.getTotalLength();

        if ("close".equals(res.getHeader("Connection"))) {
            super.close();
        }

        return jpipRes;
    }

}
