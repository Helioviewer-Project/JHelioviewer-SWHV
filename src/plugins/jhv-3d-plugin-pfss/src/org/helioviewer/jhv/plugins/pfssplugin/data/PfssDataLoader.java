package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;

public class PfssDataLoader implements Runnable {

    private final String url;
    private final long time;

    public PfssDataLoader(String url, long time) {
        this.url = url;
        this.time = time;
    }

    @Override
    public void run() {
        InputStream in = null;
        try {
            String cacheFileName = JHVDirectory.PLUGINSCACHE.getPath() + url.replace('/', '_');
            boolean loadFromFile = false;
            File f = new File(cacheFileName);
            if (f.exists() && !f.isDirectory() && f.canRead()) {
                in = new BufferedInputStream(new FileInputStream(f), 65536);
                loadFromFile = true;
            } else {
                URL u = new URL(PfssSettings.baseURL + url);
                URLConnection uc = u.openConnection();
                in = new BufferedInputStream(uc.getInputStream(), 65536);
                String encoding = uc.getHeaderField("Content-Encoding");
                if ("gzip".equals(encoding)) {
                    in = new GZIPInputStream(in);
                }
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            FileOutputStream out = null;
            if (!loadFromFile) {
                out = new FileOutputStream(cacheFileName);
            }

            int nRead;
            byte[] data = new byte[16384];

            try {
                while ((nRead = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                    if (out != null) {
                        out.write(data, 0, nRead);
                    }
                }
                buffer.flush();
            } finally {
                if (out != null) {
                    out.close();
                }
            }

            PfssData pfssData = new PfssData(buffer.toByteArray(), time);
            EventQueue.invokeLater(() -> PfssPlugin.getPfsscache().addData(pfssData));
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
