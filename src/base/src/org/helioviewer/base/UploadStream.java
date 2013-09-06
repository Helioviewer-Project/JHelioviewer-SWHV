package org.helioviewer.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * General connection class to save data to a given URI/URL
 * 
 * To use it: - Create a new UploadStream object - Connect with the current
 * parameters .connect(), automatically done if used getInput() - Get output
 * stream .getOutput()
 * 
 * To get data @see DownloadStream TODO Look how to be integrated within url
 * schema
 * 
 * @author Helge Dietert
 */
public class UploadStream {
    /**
     * Used uri to connect
     */
    private URI uri;
    /**
     * Created output stream
     */
    private OutputStream out = null;

    /**
     * Creates a uploadStream object to given uri, assuming file if nothing
     * given
     * 
     * @param uri
     *            uri to connect
     */
    public UploadStream(URI uri) throws MalformedURLException, URISyntaxException {
        if (!uri.isAbsolute()) {
            uri = new URI("file:" + uri.toString());
        }
        this.uri = uri;
    }

    /**
     * Connects go a given url
     * 
     * @throws IOException
     *             Error to get connection to save file
     */
    public void connect() throws IOException {
        if (uri.getScheme().equals("file")) {
            File file = new File(uri);
            if (file.exists()) {
                file.delete();
            }
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            out = new FileOutputStream(file);
        } else {
            throw new IllegalArgumentException("Unsupported URI scheme: " + uri.getScheme());
        }
    }

    /**
     * Gets the output stream and opens if necessary
     * 
     * @return output stream to given uri
     * @throws IOException
     *             if connecting to the file
     */
    public OutputStream getOutput() throws IOException {
        if (out == null)
            connect();
        return out;
    }
}
