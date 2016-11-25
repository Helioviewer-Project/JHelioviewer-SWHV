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
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPHeaderKey;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPRequest;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPRequest.Method;

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
     * @param _uri
     * @return The first response of the server when connecting.
     * @throws IOException
     */
    @Override
    public Object connect(URI uri) throws IOException {
        super.connect(uri);

        jpipPath = uri.getPath();

        JPIPQuery query = new JPIPQuery(JPIPRequestField.CNEW.toString(), "http",
                                        JPIPRequestField.TYPE.toString(), "jpp-stream",
                                        JPIPRequestField.TID.toString(), "0",
                                        JPIPRequestField.LEN.toString(), "512"); // deliberately small

        JPIPRequest req = new JPIPRequest(HTTPRequest.Method.GET);
        req.setQuery(query.toString());

        JPIPResponse res = null;
        while (res == null && isConnected()) {
            send(req);
            res = receive();
        }
        if (res == null)
            throw new IOException("The server did not send a response after connection");

        HashMap<String, String> map = null;
        String cnew = res.getHeader("JPIP-cnew");
        if (cnew != null) {
            map = new HashMap<>();
            String[] parts = cnew.split(",");
            for (String part : parts)
                for (String cnewParam : cnewParams)
                    if (part.startsWith(cnewParam + '='))
                        map.put(cnewParam, part.substring(cnewParam.length() + 1));
        }
        if (map == null)
            throw new IOException("The header 'JPIP-cnew' was not sent by the server");

        jpipPath = '/' + map.get("path");

        jpipChannelID = map.get("cid");
        if (jpipChannelID == null)
            throw new IOException("The channel id was not sent by the server");

        if (!"http".equals(map.get("transport")))
            throw new IOException("The client only supports HTTP transport");

        return res;
    }

    /** Closes the JPIPSocket */
    @Override
    public void close() throws IOException {
        if (isClosed())
            return;

        try {
            if (jpipChannelID != null) {
                JPIPQuery query = new JPIPQuery(JPIPRequestField.CCLOSE.toString(), jpipChannelID,
                                                JPIPRequestField.LEN.toString(), "0");

                JPIPRequest req = new JPIPRequest(HTTPRequest.Method.GET);
                req.setQuery(query.toString());
                send(req);
            }
        } catch (IOException e) {
            // e.printStackTrace();
        } finally {
            super.close();
        }
    }

    /**
     * Sends a JPIPRequest
     *
     * @param _req
     * @throws IOException
     */
    public void send(JPIPRequest req) throws IOException {
        String queryStr = req.getQuery();

        // Adds some default headers if they were not already added.
        if (!req.headerExists(HTTPHeaderKey.USER_AGENT.toString()))
            req.setHeader(HTTPHeaderKey.USER_AGENT.toString(), JHVGlobals.getUserAgent());
        if (!req.headerExists(HTTPHeaderKey.ACCEPT_ENCODING.toString()))
            req.setHeader(HTTPHeaderKey.ACCEPT_ENCODING.toString(), "gzip");
        if (!req.headerExists(HTTPHeaderKey.CACHE_CONTROL.toString()))
            req.setHeader(HTTPHeaderKey.CACHE_CONTROL.toString(), "no-cache");
        if (!req.headerExists(HTTPHeaderKey.HOST.toString()))
            req.setHeader(HTTPHeaderKey.HOST.toString(), (getHost() + ':' + getPort()));
        // Adds a necessary JPIP request field
        if (jpipChannelID != null && !queryStr.contains("cid=") && !queryStr.contains("cclose"))
            queryStr += "&cid=" + jpipChannelID;

        if (req.getMethod() == Method.GET) {
            if (!req.headerExists(HTTPHeaderKey.CONNECTION.toString()))
                req.setHeader(HTTPHeaderKey.CONNECTION.toString(), "Keep-Alive");
        } else if (req.getMethod() == Method.POST) {
            if (!req.headerExists(HTTPHeaderKey.CONTENT_TYPE.toString()))
                req.setHeader(HTTPHeaderKey.CONTENT_TYPE.toString(), "application/x-www-form-urlencoded");
            if (!req.headerExists(HTTPHeaderKey.CONTENT_LENGTH.toString()))
                req.setHeader(HTTPHeaderKey.CONTENT_LENGTH.toString(), Integer.toString(queryStr.getBytes(StandardCharsets.UTF_8).length));
        }

        StringBuilder str = new StringBuilder();

        // Adds the URI line
        str.append(req.getMethod()).append(' ').append(jpipPath);
        if (req.getMethod() == Method.GET) {
            str.append('?').append(queryStr);
        }
        str.append(' ').append(HTTPConstants.versionText).append(HTTPConstants.CRLF);

        // Adds the headers
        for (String key : req.getHeaders()) {
            str.append(key).append(": ").append(req.getHeader(key)).append(HTTPConstants.CRLF);
        }
        str.append(HTTPConstants.CRLF);

        // Adds the message body if necessary
        if (req.getMethod() == HTTPRequest.Method.POST)
            str.append(queryStr);

        // if (!isConnected())
        //    reconnect();

        // Writes the result to the output stream
        getOutputStream().write(str.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Receives a JPIPResponse returning null if EOS reached */
    @Override
    public JPIPResponse receive() throws IOException {
        // long tini = System.currentTimeMillis();

        HTTPResponse httpRes = (HTTPResponse) super.receive();
        JPIPResponse res = new JPIPResponse(httpRes);

        if (res.getCode() != 200)
            throw new IOException("Invalid status code returned (" + res.getCode() + ')');
        if (!"image/jpp-stream".equals(res.getHeader("Content-Type")))
            throw new IOException("Expected image/jpp-stream content");

        replyTextTm = System.currentTimeMillis();

        TransferInputStream transferInput;
        String transferEncoding = res.getHeader("Transfer-Encoding") == null ? "" : res.getHeader("Transfer-Encoding");
        switch (transferEncoding.toLowerCase()) {
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
        String contentEncoding = res.getHeader("Content-Encoding") == null ? "" : res.getHeader("Content-Encoding");
        switch (contentEncoding.toLowerCase()) {
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

        JPIPDataInputStream jpip = new JPIPDataInputStream(input);
        try {
            JPIPDataSegment seg;
            while ((seg = jpip.readSegment()) != null)
                res.addJpipDataSegment(seg);
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

        return res;
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
