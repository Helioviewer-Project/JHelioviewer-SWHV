package org.helioviewer.jhv.io;

import java.awt.EventQueue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.log.Log;

public class DownloadStream {

    private InputStream in;

    private int contentLength = -1;
    private boolean response400 = false;

    // Timeouts in ms
    private final int readTimeout;
    private final int connectTimeout;

    // URL to connect
    private final URL url;
    private final boolean ignore400;

    private DownloadStream(URL _url, int _connectTimeout, int _readTimeout, boolean _ignore400) {
        url = _url;
        readTimeout = _readTimeout;
        connectTimeout = _connectTimeout;
        ignore400 = _ignore400;
    }

    public DownloadStream(URL _url, boolean _ignore400) {
        this(_url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout(), _ignore400);
    }

    public DownloadStream(URL _url) {
        this(_url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout(), false);
    }

    public DownloadStream(String _url) throws MalformedURLException {
        this(new URL(_url));
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

        //Log.debug("Connect to " + url);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpC = (HttpURLConnection) connection;
            httpC.setRequestProperty("Accept-Encoding", "gzip,deflate");
            httpC.setRequestProperty("User-Agent", JHVGlobals.userAgent);

            try {
                httpC.connect();
            } catch (IOException e) {
                Log.warn("HTTP connection failed: " + url + " " + e);
            }

            // Check the connection code
            int code = httpC.getResponseCode();
            if (code > 400) {
                throw new IOException("Error opening HTTP connection to " + url + " Response code: " + code);
            }

            if (!ignore400 && code == 400) {
                throw new IOException("Error opening HTTP connection to " + url + " Response code: " + code);
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
            in = connection.getInputStream();
        }
        contentLength = connection.getContentLength();
    }

    public InputStream getInput() throws IOException {
        if (in == null)
            connect();
        return in;
    }

    public int getContentLength() {
        return contentLength;
    }

}
