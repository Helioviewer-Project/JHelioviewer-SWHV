package org.helioviewer.jhv.view.j2k.jpip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.view.j2k.jpip.http.HTTPSocket;

// One JPIP channel over a persistent HTTP connection. Responses arrive in request order.
public final class JPIPSocket extends HTTPSocket {

    private static final int META_REQUEST_LEN = 2000000;
    private static final int FRAME_RESPONSE_LIMIT = 2 * 1024 * 1024;

    private final byte[] chunk = new byte[65536];
    private ByteBuffer body = ByteBuffer.allocateDirect(chunk.length);

    // The jpip channel ID for the connection (persistent)
    private String jpipChannelID;

    // The path supplied on the uri line of the HTTP message. Generally for the
    // first request it is the image path in relative terms, but the response
    // could change it. The Kakadu server seems to change it to /jpip.
    private String jpipPath;

    // Asks for a channel; the first response received opens it.
    public JPIPSocket(URI uri) throws IOException {
        super(uri);
        jpipPath = uri.getPath();
        try {
            writeRequest(createQuery(512, "cnew", "http", "type", "jpp-stream", "tid", "0")); // deliberately short
        } catch (IOException e) {
            super.close();
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

    private void writeRequest(String queryStr) throws IOException {
        write("GET " + jpipPath + '?' + queryStr + httpHeader);
    }

    public void sendMetadata() throws IOException {
        writeRequest(createQuery(META_REQUEST_LEN, "cid", jpipChannelID, "stream", "0", "metareq", "[*]!!"));
    }

    public void sendFrame(long stream, int width, int height, int pad) throws IOException {
        writeRequest(createQuery(FRAME_RESPONSE_LIMIT, "cid", jpipChannelID, "stream", Long.toString(stream),
                "fsiz", width + "," + height + ",closest", "rsiz", (width + pad) + "," + (height + pad)));
    }

    // The whole body of the next response, valid until the next call.
    public ByteBuffer receive() throws IOException {
        Map<String, String> header = readHeader();
        if (!"image/jpp-stream".equals(header.get("Content-Type")))
            throw new IOException("Expected image/jpp-stream content");

        body.clear();
        try (InputStream in = getInputStream(header)) {
            int count;
            while ((count = in.read(chunk)) >= 0) {
                if (count > body.remaining()) {
                    ByteBuffer larger = ByteBuffer.allocateDirect(Math.max(2 * body.capacity(), body.position() + count));
                    body = larger.put(body.flip());
                }
                body.put(chunk, 0, count);
            }
        }

        if (jpipChannelID == null)
            openChannel(header.get("JPIP-cnew"));
        if ("close".equals(header.get("Connection"))) {
            super.close();
        }
        return body.flip();
    }

    private void openChannel(String cnew) throws IOException {
        if (cnew == null)
            throw new IOException("The header 'JPIP-cnew' was not sent by the server");

        Map<String, String> map = new HashMap<>();
        for (String part : cnew.split(",")) {
            int eq = part.indexOf('=');
            if (eq > 0)
                map.put(part.substring(0, eq), part.substring(eq + 1));
        }

        String path = map.get("path");
        String cid = map.get("cid");
        if (cid == null || path == null)
            throw new IOException("The server did not send a channel id and path in JPIP-cnew");
        if (!"http".equals(map.get("transport")))
            throw new IOException("The client only supports HTTP transport");

        jpipChannelID = cid;
        jpipPath = '/' + path;
    }

}
