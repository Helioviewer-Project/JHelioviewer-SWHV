package org.helioviewer.jhv.io;

import java.awt.EventQueue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.helioviewer.jhv.JHVGlobals;

public class DownloadStream {

    private InputStream in;

    private boolean response400;
    private final boolean ignore400;

    private final URLConnection conn;

    private DownloadStream(URL url, int connectTimeout, int readTimeout, boolean _ignore400) throws IOException {
        ignore400 = _ignore400;

        conn = url.openConnection();
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
        conn.setRequestProperty("User-Agent", JHVGlobals.userAgent);
    }

    public DownloadStream(URL url, boolean _ignore400) throws IOException {
        this(url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout(), _ignore400);
    }

    public DownloadStream(URL url) throws IOException {
        this(url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout(), false);
    }

    public DownloadStream(String url) throws IOException {
        this(new URL(url));
    }

    public boolean isResponse400() {
        return response400;
    }

    private static InputStream getEncodedStream(String encoding, InputStream httpStream) throws IOException {
        if (encoding != null) {
            switch (encoding.toLowerCase()) {
                case "gzip":
                    return new GZIPInputStream(httpStream);
                case "deflate":
                    return new InflaterInputStream(httpStream);
            }
        }
        return httpStream;
    }

    private void connect() throws IOException {
        if (EventQueue.isDispatchThread())
            throw new IOException("Don't do that");

        if (conn instanceof HttpURLConnection) {
            HttpURLConnection httpC = (HttpURLConnection) conn;
            // Check the connection code
            int code = httpC.getResponseCode();
            if (code > 400) {
                throw new IOException("Error opening HTTP connection to " + conn.getURL() + " Response code: " + code);
            }

            if (!ignore400 && code == 400) {
                throw new IOException("Error opening HTTP connection to " + conn.getURL() + " Response code: " + code);
            }

            InputStream strm;
            if (code == 400) {
                response400 = true;
                strm = httpC.getErrorStream();
                if (strm == null)
                    strm = httpC.getInputStream();
            } else {
                strm = httpC.getInputStream();
            }

            in = getEncodedStream(httpC.getContentEncoding(), strm);
        } else { // Not a HTTP connection
            in = conn.getInputStream();
        }
    }

    public InputStream getInput() throws IOException {
        if (in == null)
            connect();
        return in;
    }

    public int getContentLength() {
        return conn.getContentLength();
    }

}
