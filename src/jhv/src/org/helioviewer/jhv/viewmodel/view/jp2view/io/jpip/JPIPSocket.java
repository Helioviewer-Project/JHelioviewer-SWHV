package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.ChunkedInputStream;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.FixedSizedInputStream;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.TransferInputStream;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPMessage;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPSocket;

/**
 * Assumes a persistent HTTP connection.
 *
 * @author caplins
 *
 */
public class JPIPSocket extends HTTPSocket {
    /**
     * The jpip channel ID for the connection (persistent)
     */
    private String jpipChannelID;

    /**
     * The path supplied on the uri line of the HTTP message. Generally for the
     * first request it is the image path in relative terms, but the response
     * could change it. The Kakadu server seems to change it to /jpip.
     */
    private String jpipPath;

    /** Amount of data (bytes) of the last response */
    private int receivedData = 0;

    /** Time when received the last reply text */
    private long replyTextTm = 0;

    /** Time when received the last reply data */
    private long replyDataTm = 0;

    private static final String[] cnewParams = { "cid", "transport", "host", "path", "port", "auxport" };

    /**
     * Connects to the specified URI. The second parameter only serves to
     * distinguish it from the super classes connect method (I want to return
     * something and the super class has a return type of void).
     *
     * @param uri
     * @return The first response of the server when connecting.
     * @throws IOException
     */
    @Override
    public Object connect(URI uri) throws IOException {
        super.connect(uri);

        jpipPath = uri.getPath();

        JPIPRequest req = new JPIPRequest();
        JPIPQuery query = new JPIPQuery(JPIPRequestField.CNEW.toString(), "http",
                                        JPIPRequestField.TYPE.toString(), "jpp-stream",
                                        JPIPRequestField.TID.toString(), "0",
                                        JPIPRequestField.LEN.toString(), "512"); // deliberately small
        req.setQuery(query.toString());

        JPIPResponse res = null;
        while (res == null && isConnected()) {
            send(req);
            res = receive();
        }
        if (res == null)
            throw new IOException("The server did not send a response after connection");

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

        return res;
    }

    // Closes the JPIPSocket
    @Override
    public void close() throws IOException {
        if (isClosed())
            return;

        try {
            if (jpipChannelID != null) {
                JPIPRequest req = new JPIPRequest();
                JPIPQuery query = new JPIPQuery(JPIPRequestField.CCLOSE.toString(), jpipChannelID,
                                                JPIPRequestField.LEN.toString(), "0");
                req.setQuery(query.toString());
                send(req);
            }
        } finally {
            super.close();
        }
    }

    // Sends a JPIPRequest
    public void send(JPIPRequest req) throws IOException {
        // Add some default headers
        req.setHeader("User-Agent", JHVGlobals.getUserAgent());
        req.setHeader("Connection", "Keep-Alive");
        req.setHeader("Accept-Encoding", "gzip");
        req.setHeader("Cache-Control", "no-cache");
        req.setHeader("Host", getHost() + ':' + getPort());

        // Add a necessary JPIP request field
        String queryStr = req.getQuery();
        if (jpipChannelID != null && !queryStr.contains("cid=") && !queryStr.contains("cclose"))
            queryStr += "&cid=" + jpipChannelID;

        // Build request to send
        String res = "GET " + jpipPath + '?' + queryStr + " HTTP/1.1\r\n" + req + "\r\n";
        // Writes the result to the output stream
        getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
    }

    // Receives a JPIPResponse returning null if EOS reached
    public JPIPResponse receive() throws IOException {
        // long tini = System.currentTimeMillis();
        HTTPMessage res = recv();
        if (!"image/jpp-stream".equals(res.getHeader("Content-Type")))
            throw new IOException("Expected image/jpp-stream content");

        replyTextTm = System.currentTimeMillis();

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
        JPIPDataInputStream jpip = new JPIPDataInputStream(input);
        try {
            JPIPDataSegment seg;
            while ((seg = jpip.readSegment()) != null)
                jpipRes.addJpipDataSegment(seg);
        } finally {
            input.close(); // make sure the stream is exhausted
        }

        if ("close".equals(res.getHeader("Connection"))) {
            super.close();
        }
        replyDataTm = System.currentTimeMillis();
        receivedData = transferInput.getTotalLength();

        // System.out.format("Bandwidth: %.2f KB/seg.\n", (double)(receivedData
        // * 1.0) / (double)(replyDataTm - tini));

        return jpipRes;
    }

    /**
     * Returns the time when received the last reply text
     */
    public long getReplyTextTime() {
        return replyTextTm;
    }

    /**
     * Returns the time when received the last reply data
     */
    public long getReplyDataTime() {
        return replyDataTm;
    }

    /**
     * Returns the amount of data (bytes) of the last response.
     */
    public int getReceivedData() {
        return receivedData;
    }

}
