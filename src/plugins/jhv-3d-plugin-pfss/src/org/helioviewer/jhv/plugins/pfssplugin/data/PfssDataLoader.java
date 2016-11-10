package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.DownloadStream;
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
        String cacheFileName = JHVDirectory.PLUGINSCACHE.getPath() + url.replace('/', '_');
        File f = new File(cacheFileName);

        String remote;
        boolean loadFromFile;
        if (f.exists() && !f.isDirectory() && f.canRead()) {
            loadFromFile = true;
            remote = f.toURI().toString();
        } else {
            loadFromFile = false;
            remote = PfssSettings.baseURL + url;
        }

        try (InputStream in = new BufferedInputStream(new DownloadStream(new URL(remote)).getInput(), BUFSIZ)) {
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
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f), BUFSIZ)) {
                    buffer.writeTo(out);
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
