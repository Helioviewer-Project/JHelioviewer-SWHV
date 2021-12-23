package org.helioviewer.jhv.plugins.pfss.data;

import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.JHVTime;

import com.google.common.util.concurrent.FutureCallback;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;

class PfssDataLoader {

    static void submit(long time, URI uri) {
        EventQueueCallbackExecutor.pool.submit(new DataLoader(time, uri), new Callback(uri));
        PfssPlugin.downloads++;
    }

    private record DataLoader(long time, URI uri) implements Callable<PfssData> {
        @Override
        public PfssData call() throws Exception {
            try (NetClient nc = NetClient.of(uri); Fits fits = new Fits(nc.getStream())) {
                BasicHDU<?>[] hdus = fits.read();
                if (hdus == null || hdus.length < 2 || !(hdus[1] instanceof BinaryTableHDU bhdu))
                    throw new Exception("Could not read FITS");

                Header header = bhdu.getHeader();
                String dateFits = header.getStringValue("DATE-OBS");
                if (dateFits == null)
                    throw new Exception("DATE-OBS not found");
                JHVTime date = new JHVTime(dateFits);
                if (time != date.milli)
                    throw new Exception("Inconsistent DATE-OBS. Expected " + new JHVTime(time) + ", got " + date);

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

    }

    private record Callback(URI uri) implements FutureCallback<PfssData> {

        @Override
        public void onSuccess(PfssData result) {
            PfssPlugin.downloads--;
            PfssPlugin.getPfsscache().putData(uri, result);
            MovieDisplay.display(); //!
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            PfssPlugin.downloads--;
            Log.error(t);
        }

    }

}
