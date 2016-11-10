package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;

public class PfssDataLoader implements Runnable {

    private static final int BUFSIZ = 65536;
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
                in = new BufferedInputStream(new FileInputStream(f), BUFSIZ);
                loadFromFile = true;
            } else {
                URL u = new URL(PfssSettings.baseURL + url);
                URLConnection uc = u.openConnection();
                in = new BufferedInputStream(uc.getInputStream(), BUFSIZ);
                String encoding = uc.getHeaderField("Content-Encoding");
                if ("gzip".equals(encoding)) {
                    in = new GZIPInputStream(in);
                }
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[BUFSIZ];
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            PfssData pfssData = new PfssData(buffer.toByteArray(), time);
            EventQueue.invokeLater(() -> PfssPlugin.getPfsscache().addData(pfssData));

            if (!loadFromFile)
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(cacheFileName), BUFSIZ)) {
                    buffer.writeTo(out);
                }
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
