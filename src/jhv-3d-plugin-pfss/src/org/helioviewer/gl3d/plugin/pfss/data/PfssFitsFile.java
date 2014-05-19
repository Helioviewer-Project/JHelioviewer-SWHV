package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.media.opengl.GL;

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
            URL u = new URL("http://soleil.i4ds.ch/sol-win/2013-09-01_12-04-00.000_pfss_field_data.fits");
            // URL u = new URL(url);
            URLConnection uc = u.openConnection();
            int contentLength = uc.getContentLength();
            InputStream raw = uc.getInputStream();
            in = new BufferedInputStream(raw);

            gzipFitsFile = new byte[contentLength];

            int bytesRead = 0;
            int offset = 0;
            while (offset < contentLength) {
                bytesRead = in.read(gzipFitsFile, offset, gzipFitsFile.length - offset);
                if (bytesRead == -1)
                    break;
                offset += bytesRead;
            }
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
    public void clear(GL gl) {
        if (data != null) {
            this.data.clear(gl);
            this.data = null;
        }
    }

}
