package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import javax.media.opengl.GL2;

/**
 * Class to load the fitsfile with a http-request and store them in a byte[]
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssFitsFile {
    private PfssData data = null;
    private byte[] gzipFitsFile;
    private boolean loaded = false;

    /**
     * Function to load the data and write them into a byte[]
     * 
     * @param url
     */
    public synchronized void loadFile(String url) {
        InputStream in = null;
        try {
            URL u = new URL(url);
            URLConnection uc = u.openConnection();
            int contentLength = uc.getContentLength();
            InputStream raw;
            if (uc.getHeaderField("Content-Encoding") != null && uc.getHeaderField("Content-Encoding").equals("gzip")) {
                raw = new GZIPInputStream(uc.getInputStream());
            } else {
                raw = uc.getInputStream();
            }
            in = new BufferedInputStream(raw);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();

            this.gzipFitsFile = buffer.toByteArray();
            loaded = true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * @return PfssData -> prepared data for the visualization
     */
    public PfssData getData() {
        if (data == null && loaded) {
            this.data = new PfssData(gzipFitsFile);
        }
        return data;
    }

    /**
     * Function to clear the VBO and the object data
     * 
     * @param gl
     */
    public void clear(GL2 gl) {
        if (data != null) {
            this.data.clear(gl);
            this.data = null;
        }
    }

}
