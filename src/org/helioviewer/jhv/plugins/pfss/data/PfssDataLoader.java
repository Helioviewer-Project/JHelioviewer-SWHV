package org.helioviewer.jhv.plugins.pfss.data;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssSettings;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;

class PfssDataLoader implements Runnable {

    private static final int BUFSIZ = 65536;
    private final String url;
    private final long time;

    public PfssDataLoader(String _url, long _time) {
        url = _url;
        time = _time;
    }

    @Override
    public void run() {
        String cacheFileName = JHVDirectory.PLUGINSCACHE.getPath() + url.replace('/', '_');
        File f = new File(cacheFileName);

        String remote;
        boolean loadFromFile;
        if (f.canRead() && !f.isDirectory()) {
            loadFromFile = true;
            remote = f.toURI().toString();
        } else {
            loadFromFile = false;
            remote = PfssSettings.baseURL + url;
        }

        try (InputStream in = new BufferedInputStream(new DownloadStream(remote).getInput(), BUFSIZ)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[BUFSIZ];
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            PfssData pfssData = getPfssData(buffer.toByteArray(), time);
            EventQueue.invokeLater(() -> PfssPlugin.getPfsscache().addData(pfssData));

            if (!loadFromFile)
                try (OutputStream out = FileUtils.newBufferedOutputStream(f)) {
                    buffer.writeTo(out);
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PfssData getPfssData(byte[] fitsFile, long time) throws Exception {
        try (Fits fits = new Fits(new ByteArrayInputStream(fitsFile))) {
            BasicHDU<?> hdus[] = fits.read();
            if (hdus == null || hdus.length < 2 || !(hdus[1] instanceof BinaryTableHDU))
                throw new Exception("Could not read FITS");

            BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];
            short[] fieldlinex = (short[]) bhdu.getColumn("FIELDLINEx");
            short[] fieldliney = (short[]) bhdu.getColumn("FIELDLINEy");
            short[] fieldlinez = (short[]) bhdu.getColumn("FIELDLINEz");
            short[] fieldlines = (short[]) bhdu.getColumn("FIELDLINEs");

            String dateFits = bhdu.getHeader().getStringValue("DATE-OBS");
            if (dateFits == null)
                throw new Exception("DATE-OBS not found");
            return new PfssData(new JHVDate(dateFits), fieldlinex, fieldliney, fieldlinez, fieldlines, time);
        }
    }
}
