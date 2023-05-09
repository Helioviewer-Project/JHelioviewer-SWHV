package org.helioviewer.jhv.view.j2k.jpip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

import kdu_jni.KduException;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.view.j2k.jpip.http.HTTPMessage;
import org.helioviewer.jhv.view.j2k.jpip.http.HTTPSocket;

// Assumes a persistent HTTP connection
public class JPIPSocket extends HTTPSocket {

    private static final String[] cnewParams = {"cid", "transport", "host", "path", "port", "auxport"};

    // The jpip channel ID for the connection (persistent)
    private final String jpipChannelID;

    // The path supplied on the uri line of the HTTP message. Generally for the
    // first request it is the image path in relative terms, but the response
    // could change it. The Kakadu server seems to change it to /jpip.
    private String jpipPath;

    public JPIPSocket(URI uri, JPIPCache cache) throws KduException, IOException {
        super(uri);

        jpipPath = uri.getPath();

        JPIPResponse res = request(JPIPQuery.create(512, "cnew", "http", "type", "jpp-stream", "tid", "0"), cache, 0); // deliberately short
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

        try {
            if (jpipChannelID != null)
                writeRequest(JPIPQuery.create(0, "cclose", jpipChannelID));
        } catch (IOException ignore) { // no problem, server may have closed the socket
        } finally {
            super.close();
        }
    }

    private void writeRequest(String queryStr) throws IOException {
        // Add a necessary JPIP request field
        if (jpipChannelID != null && !queryStr.contains("cid=") && !queryStr.contains("cclose"))
            queryStr += "&cid=" + jpipChannelID;
        write("GET " + jpipPath + '?' + queryStr + httpHeader);
    }

    public JPIPResponse request(String queryStr, JPIPCache cache, int frame) throws KduException, IOException {
        writeRequest(queryStr);
        HTTPMessage res = readHeader();
        if (!"image/jpp-stream".equals(res.getHeader("Content-Type")))
            throw new IOException("Expected image/jpp-stream content");

        JPIPResponse jpipRes = new JPIPResponse(res.getHeader("JPIP-cnew"));
        try (InputStream in = getInputStream(res)) {
            jpipRes.readSegments(in, cache, frame);
        }

        if ("close".equals(res.getHeader("Connection"))) {
            super.close();
        }
        return jpipRes;
    }

}
