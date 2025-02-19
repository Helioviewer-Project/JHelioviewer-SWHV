package org.helioviewer.jhv.view.j2k.jpip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import kdu_jni.KduException;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.view.j2k.jpip.http.HTTPSocket;

// Assumes a persistent HTTP connection
public final class JPIPSocket extends HTTPSocket {

    private static final String[] cnewParams = {"cid", "transport", "host", "path", "port", "auxport"};
    private static final int mainHeaderKlass = Constants.getKlass(Constants.JPIP.MAIN_HEADER_DATA_BIN_CLASS);

    private static final int META_REQUEST_LEN = 2000000;
    // Maximum number of layers that can be requested at the same time
    private static final int MAX_REQ_LAYERS = 1;
    // The maximum length in bytes of a JPIP request
    private static final int MAX_REQUEST_LEN = (MAX_REQ_LAYERS + 1) * (1024 * 1024);

    // The jpip channel ID for the connection (persistent)
    private final String jpipChannelID;

    // The path supplied on the uri line of the HTTP message. Generally for the
    // first request it is the image path in relative terms, but the response
    // could change it. The Kakadu server seems to change it to /jpip.
    private String jpipPath;

    public JPIPSocket(URI uri, JPIPCache cache) throws KduException, IOException {
        super(uri);

        jpipPath = uri.getPath();

        JPIPResponse res = request(createQuery(512, "cnew", "http", "type", "jpp-stream", "tid", "0"), cache, 0); // deliberately short
        String cnew = res.getCNew();
        if (cnew == null)
            throw new IOException("The header 'JPIP-cnew' was not sent by the server");

        Map<String, String> map = new HashMap<>();
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
                writeRequest(createQuery(0, "cclose", jpipChannelID));
        } catch (IOException ignore) { // no problem, server may have closed the socket
        } finally {
            super.close();
        }
    }

    private static String createQuery(int len, String... values) {
        boolean isKey = true;
        StringBuilder buf = new StringBuilder();
        for (String val : values) {
            buf.append(val);
            buf.append(isKey ? '=' : '&');
            isKey = !isKey;
        }
        return buf + "len=" + len;
    }

    public static String createLayerQuery(int layer, String fSiz) {
        return createQuery(MAX_REQUEST_LEN, "stream", String.valueOf(layer), "fsiz", fSiz + ",closest", "rsiz", fSiz, "roff", "0,0");
    }

    public void init(JPIPCache cache) throws KduException, IOException {
        JPIPResponse res;
        String req = createQuery(META_REQUEST_LEN, "stream", "0", "metareq", "[*]!!");
        do {
            res = request(req, cache, 0);
        } while (!res.isResponseComplete());

        // prime first image
        req = createLayerQuery(0, "64,64");
        do {
            res = request(req, cache, 0);
        } while (!res.isResponseComplete() && !cache.isDataBinCompleted(mainHeaderKlass, 0, 0));
    }

    private void writeRequest(String queryStr) throws IOException {
        // Add a necessary JPIP request field
        if (jpipChannelID != null && !queryStr.contains("cid=") && !queryStr.contains("cclose"))
            queryStr += "&cid=" + jpipChannelID;
        write("GET " + jpipPath + '?' + queryStr + httpHeader);
    }

    public JPIPResponse request(String queryStr, JPIPCache cache, int frame) throws KduException, IOException {
        writeRequest(queryStr);

        Map<String, String> header = readHeader();
        if (!"image/jpp-stream".equals(header.get("Content-Type")))
            throw new IOException("Expected image/jpp-stream content");

        JPIPResponse jpipRes = new JPIPResponse(header.get("JPIP-cnew"));
        try (InputStream in = getInputStream(header)) {
            jpipRes.readSegments(in, cache, frame);
        }

        if ("close".equals(header.get("Connection"))) {
            super.close();
        }
        return jpipRes;
    }

}
