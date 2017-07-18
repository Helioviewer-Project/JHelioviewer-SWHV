package org.helioviewer.jhv.plugins.pfss.data;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.File;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.io.DownloadStream;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssSettings;
import org.helioviewer.jhv.time.JHVDate;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;

class PfssDataLoader implements Runnable {

    private static final int BUFSIZ = 65536;
    private final String url;
    private final long time;

    PfssDataLoader(String _url, long _time) {
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

        try (Fits fits = new Fits(new BufferedInputStream(new DownloadStream(remote).getInput(), BUFSIZ))) {
            PfssData pfssData = getPfssData(fits, time);
            EventQueue.invokeLater(() -> PfssPlugin.getPfsscache().addData(pfssData));

            if (!loadFromFile)
                fits.write(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PfssData getPfssData(Fits fits, long time) throws Exception {
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
