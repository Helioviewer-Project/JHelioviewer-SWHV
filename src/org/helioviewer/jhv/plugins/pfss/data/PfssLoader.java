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
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.TableHDU;

class PfssLoader {

    static void submit(long time, URI uri) {
        EventQueueCallbackExecutor.pool.submit(new DataLoader(time, uri), new Callback(uri));
        PfssPlugin.downloads++;
    }

    private static int findColumn(TableHDU<?> hdu, String name) throws Exception {
        int col = hdu.findColumn(name);
        if (col < 0)
            throw new Exception("Column not found: " + name);
        return col;
    }

    private record DataLoader(long time, URI uri) implements Callable<PfssData> {
        @Override
        public PfssData call() throws Exception {
            try (NetClient nc = NetClient.of(uri); Fits fits = new Fits(nc.getStream())) {
                BasicHDU<?>[] hdus = fits.read();
                if (hdus == null || hdus.length < 2 || !(hdus[1] instanceof TableHDU<?> hdu))
                    throw new Exception("Could not read FITS");

                Header header = hdu.getHeader();
                String dateFits = header.getStringValue("DATE-OBS");
                if (dateFits == null)
                    throw new Exception("DATE-OBS not found");
                JHVTime date = new JHVTime(dateFits);
                if (time != date.milli)
                    throw new Exception("Inconsistent DATE-OBS. Expected " + new JHVTime(time) + ", got " + date);

                int points = header.getIntValue("HIERARCH.POINTS_PER_LINE");
                if (points == 0)
                    throw new Exception("POINTS_PER_LINE not found");

                int colX = findColumn(hdu, "FIELDLINEx");
                int colY = findColumn(hdu, "FIELDLINEy");
                int colZ = findColumn(hdu, "FIELDLINEz");
                int colS = findColumn(hdu, "FIELDLINEs");
                int rows = hdu.getNRows();

                short[] flineX = new short[rows];
                short[] flineY = new short[rows];
                short[] flineZ = new short[rows];
                short[] flineS = new short[rows];

                for (int i = 0; i < rows; i++) {
                    flineX[i] = ((short[]) hdu.getElement(i, colX))[0];
                    flineY[i] = ((short[]) hdu.getElement(i, colY))[0];
                    flineZ[i] = ((short[]) hdu.getElement(i, colZ))[0];
                    flineS[i] = ((short[]) hdu.getElement(i, colS))[0];
                }
                return new PfssData(date, flineX, flineY, flineZ, flineS, points);
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
