package org.helioviewer.jhv.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.logging.Log;

public class DownloadStream {
    /**
     * Input stream to read the data from
     */
    private InputStream in = null;
    /**
     * Output to send as a post request
     */
    private String output = null;
    /**
     * Suggested name to save (if wanted)
     */
    private String outputName = null;

    private String contentDisposition = null;
    private int contentLength = -1;
    private boolean response400 = false;

    /**
     * Read timeout in ms
     */
    private final int readTimeout;
    /**
     * Connect timeout in ms
     */
    private final int connectTimeout;
    /**
     * Used url to connect
     */
    private final URL url;
    private final boolean ignore400;

    private DownloadStream(URL url, int connectTimeout, int readTimeout, boolean ignore400) {
        this.url = url;
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
        this.ignore400 = ignore400;
    }

    public DownloadStream(URL url, boolean ignore400) {
        this(url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout(), ignore400);
    }

    public DownloadStream(URL url) {
        this(url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout(), false);
    }

    public boolean isResponse400() {
        return response400;
    }

    private InputStream getEncodedStream(String encoding, InputStream httpStream) throws IOException {
        if (encoding != null) {
            if (encoding.equalsIgnoreCase("gzip"))
                return new GZIPInputStream(httpStream);
            else if (encoding.equalsIgnoreCase("deflate"))
                return new InflaterInputStream(httpStream);
        }
        return httpStream;
    }

    /**
     * Opens the connection with compression if the server supports
     *
     * @throws IOException
     *             From accessing the network
     */
    private void connect() throws IOException {
        //Log.debug("Connect to " + url);
        URLConnection connection = url.openConnection();
        // Set timeouts
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpC = (HttpURLConnection) connection;
            // get compression if supported
            httpC.setRequestProperty("Accept-Encoding", "gzip, deflate");
            httpC.setRequestProperty("User-Agent", JHVGlobals.getUserAgent());

            // Write post data if necessary
            if (output != null) {
                connection.setDoOutput(true);
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
                out.write(output);
                out.close();
            }
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
        } else {
            // Not an http connection
            // Write post data if necessary
            if (output != null) {
                connection.setDoOutput(true);
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
                out.write(output);
                out.close();
            }
            // Okay just normal
            in = connection.getInputStream();
        }
        contentDisposition = connection.getHeaderField("Content-Disposition");
        contentLength = connection.getContentLength();
    }

    /**
     * Gives the outstream to read the response, after calling connect. If it is
     * not already connected it will automatically connect
     *
     * @return output stream of the connection
     * @throws IOException
     *             Error from creating the connction
     */
    public InputStream getInput() throws IOException {
        if (in == null)
            connect();
        return in;
    }

    /**
     * After requesting the data the associated file name to save from
     * Content-Disposition or the url name
     *
     * @return suggested download name
     */
    public String getOutputName() {
        if (outputName == null) {
            if (contentDisposition != null) {
                Matcher m = Regex.ContentDispositionFilename.matcher(contentDisposition);
                if (m.find()) {
                    outputName = m.group(1);
                }
            }
            if (outputName == null) {
                outputName = url.getFile().replace('/', '-');
            }
        }
        return outputName;
    }

    public int getContentLength() {
        return contentLength;
    }

    /**
     * @return the read timeout
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * @return the connect timeout
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Set the output to send to the server (in HTTP as POST)
     *
     * @param output
     *            Send output to the server, null if nothing (GET in HTTP)
     */
    public void setOutput(String output) {
        this.output = output;
    }

}
