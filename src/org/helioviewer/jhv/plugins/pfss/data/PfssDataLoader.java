package org.helioviewer.jhv.plugins.pfss.data;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.JHVDate;

import com.google.common.util.concurrent.FutureCallback;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;

class PfssDataLoader implements Callable<PfssData> {

    static Future<PfssData> submit(long time, String url) {
        return EventQueueCallbackExecutor.pool.submit(new PfssDataLoader(time, url), new Callback(url));
    }

    private final long time;
    private final String url;

    private PfssDataLoader(long _time, String _url) {
        PfssPlugin.downloads++;
        time = _time;
        url = _url;
    }

    @Override
    public PfssData call() throws Exception {
        try (NetClient nc = NetClient.of(url); Fits fits = new Fits(nc.getStream())) {
            BasicHDU<?>[] hdus = fits.read();
            if (hdus == null || hdus.length < 2 || !(hdus[1] instanceof BinaryTableHDU))
                throw new Exception("Could not read FITS");

            BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];
            Header header = bhdu.getHeader();

            String dateFits = header.getStringValue("DATE-OBS");
            if (dateFits == null)
                throw new Exception("DATE-OBS not found");
            JHVDate date = new JHVDate(dateFits);
            if (time != date.milli)
                throw new Exception("Inconsistent DATE-OBS. Expected " + new JHVDate(time) + ", got " + date);

            int points = header.getIntValue("HIERARCH.POINTS_PER_LINE");
            if (points == 0)
                throw new Exception("POINTS_PER_LINE not found");

            short[] flinex = (short[]) bhdu.getColumn("FIELDLINEx");
            short[] fliney = (short[]) bhdu.getColumn("FIELDLINEy");
            short[] flinez = (short[]) bhdu.getColumn("FIELDLINEz");
            short[] flines = (short[]) bhdu.getColumn("FIELDLINEs");
            if (flinex.length != fliney.length || flinex.length != flinez.length || flinex.length != flines.length)
                throw new Exception("Fieldline arrays not equal " + flinex.length + ' ' + fliney.length + ' ' + flinez.length + ' ' + flinex.length);

            return new PfssData(date, flinex, fliney, flinez, flines, points);
        }
    }

    private static class Callback implements FutureCallback<PfssData> {

        private final String u;

        Callback(String _u) {
            u = _u;
        }

        @Override
        public void onSuccess(PfssData result) {
            PfssPlugin.downloads--;
            PfssPlugin.getPfsscache().putData(u, result);
            MovieDisplay.display(); //!
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            PfssPlugin.downloads--;
            Log.error("PfssDataLoader: " + u, t);
        }

    }

}
