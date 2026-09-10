package org.helioviewer.jhv.view.j2k.jpip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.view.j2k.jpip.http.HTTPSocket;

import kdu_jni.KduException;

// Assumes a persistent HTTP connection
public final class JPIPSocket extends HTTPSocket {

    private static final String[] cnewParams = {"cid", "transport", "host", "path", "port", "auxport"};
    private static final int mainHeaderKlass = Constants.getKlass(Constants.JPIP.MAIN_HEADER_DATA_BIN_CLASS);

    private static final int META_REQUEST_LEN = 2000000;
    private static final int FRAME_RESPONSE_LIMIT = 2 * 1024 * 1024;

    public record FrameResponse(int frame, boolean complete) {}

    // Owned by the reader thread. Abort only closes the socket; it does not touch this queue.
    private final ArrayDeque<Integer> pendingFrames = new ArrayDeque<>();

    // The jpip channel ID for the connection (persistent)
    private final String jpipChannelID;

    // The path supplied on the uri line of the HTTP message. Generally for the
    // first request it is the image path in relative terms, but the response
    // could change it. The Kakadu server seems to change it to /jpip.
    private String jpipPath;

    public JPIPSocket(URI uri, JPIPCache cache) throws KduException, IOException {
        super(uri);
        try {
            jpipPath = uri.getPath();

            JPIPResponse res = requestInitialization(createQuery(512, "cnew", "http", "type", "jpp-stream", "tid", "0"), cache); // deliberately short
            String cnew = res.getCNew();
            if (cnew == null)
                throw new IOException("The header 'JPIP-cnew' was not sent by the server");

            Map<String, String> map = new HashMap<>();
            for (String part : cnew.split(","))
                for (String cnewParam : cnewParams)
                    if (part.startsWith(cnewParam + '='))
                        map.put(cnewParam, part.substring(cnewParam.length() + 1));

            String path = map.get("path");
            jpipChannelID = map.get("cid");
            if (jpipChannelID == null || path == null)
                throw new IOException("The server did not send a channel id and path in JPIP-cnew");
            if (!"http".equals(map.get("transport")))
                throw new IOException("The client only supports HTTP transport");

            jpipPath = '/' + path;
        } catch (KduException | IOException | RuntimeException | Error e) {
            try {
                super.close();
            } catch (IOException ignore) {
            }
            throw e;
        }
    }

    // Interrupt pending I/O without writing another request.
    public void abort() throws IOException {
        super.close();
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

    private static String createFrameQuery(int frame, String size) {
        return createQuery(FRAME_RESPONSE_LIMIT, "stream", String.valueOf(frame), "fsiz", size + ",closest", "rsiz", size, "roff", "0,0");
    }

    public void init(JPIPCache cache) throws KduException, IOException {
        JPIPResponse res;
        String req = createQuery(META_REQUEST_LEN, "stream", "0", "metareq", "[*]!!");
        do {
            res = requestInitialization(req, cache);
        } while (!res.isResponseComplete());

        // prime first image
        req = createFrameQuery(0, "64,64");
        do {
            res = requestInitialization(req, cache);
        } while (!res.isResponseComplete() && !cache.isDataBinCompleted(mainHeaderKlass, 0, 0));
    }

    public void sendFrame(int frame, String size) throws IOException {
        writeRequest(createFrameQuery(frame, size));
        pendingFrames.addLast(frame);
    }

    public FrameResponse receiveFrame(JPIPCache cache) throws KduException, IOException {
        int frame = pendingFrames.getFirst();
        JPIPResponse response = receive(cache, frame);
        pendingFrames.removeFirst();
        return new FrameResponse(frame, response.isResponseComplete());
    }

    public int pendingCount() {
        return pendingFrames.size();
    }

    private void writeRequest(String queryStr) throws IOException {
        // Add a necessary JPIP request field
        if (jpipChannelID != null && !queryStr.contains("cid=") && !queryStr.contains("cclose"))
            queryStr += "&cid=" + jpipChannelID;
        write("GET " + jpipPath + '?' + queryStr + httpHeader);
    }

    private JPIPResponse requestInitialization(String queryStr, JPIPCache cache) throws KduException, IOException {
        writeRequest(queryStr);
        return receive(cache, 0);
    }

    private JPIPResponse receive(JPIPCache cache, int frame) throws KduException, IOException {
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
