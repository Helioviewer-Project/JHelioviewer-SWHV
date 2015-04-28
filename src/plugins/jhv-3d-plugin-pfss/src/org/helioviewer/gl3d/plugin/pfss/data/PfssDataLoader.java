package org.helioviewer.gl3d.plugin.pfss.data;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;

public class PfssDataLoader implements Runnable {

    private final String url;
    private final PfssCache cache;
    private final long time;

    public PfssDataLoader(String url, long time, PfssCache cache) {
        this.url = url;
        this.cache = cache;
        this.time = time;
    }

    @Override
    public void run() {
        loadFile(url, time);
    }

    private void loadFile(String url, long time) {
        InputStream in = null;
        try {
            URL u = new URL(PfssSettings.baseUrl + url);
            URLConnection uc = u.openConnection();

            InputStream in = new BufferedInputStream(uc.getInputStream(), 65536);
            if (uc.getHeaderField("Content-Encoding") != null && uc.getHeaderField("Content-Encoding").equals("gzip")) {
                in = new GZIPInputStream(in);
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();

            byte[] gzipFitsFile = buffer.toByteArray();
            final PfssData pfssData = new PfssData(gzipFitsFile, time);

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    cache.addData(pfssData);
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
