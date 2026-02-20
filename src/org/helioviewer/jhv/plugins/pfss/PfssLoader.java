package org.helioviewer.jhv.plugins.pfss;

import java.awt.EventQueue;
import java.net.URI;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import com.google.common.util.concurrent.FutureCallback;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.TableHDU;
import okio.BufferedSource;

class PfssLoader {

    record Data(JHVTime dateObs, float[] lineX, float[] lineY, float[] lineZ, float[] lineS, int points) {
    }

    static void submitList(long start, long end) {
        EDTCallbackExecutor.pool.submit(new ListLoader(start, end), new CallbackList(start));
        PfssPlugin.downloads++;
    }

    static void submitData(long time, URI uri) {
        EDTCallbackExecutor.pool.submit(new DataLoader(time, uri), new CallbackData(uri));
        PfssPlugin.downloads++;
    }

    private record ListLoader(long start, long end) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            Calendar cal = Calendar.getInstance();

            cal.setTimeInMillis(start);
            int startYear = cal.get(Calendar.YEAR);
            int startMonth = cal.get(Calendar.MONTH);

            cal.setTimeInMillis(end + 31 * TimeUtils.DAY_IN_MILLIS);
            int endYear = cal.get(Calendar.YEAR);
            int endMonth = cal.get(Calendar.MONTH);

            do {
                String m = startMonth < 9 ? "0" + (startMonth + 1) : Integer.toString(startMonth + 1);
                URI listUri = new URI(PfssSettings.BASE_URL + startYear + '/' + m + "/list.txt");
                HashMap<Long, URI> uris = new HashMap<>();

                // may come from http cache
                try (NetClient nc = NetClient.of(listUri); BufferedSource source = nc.getSource()) {
                    String line;
                    while ((line = source.readUtf8Line()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty())
                            continue;

                        String[] splitted = Regex.MultiSpace.split(trimmed);
                        if (splitted.length != 2) {
                            Log.warn("Skipping invalid PFSS list line: " + line);
                            continue;
                        }
                        try {
                            uris.put(TimeUtils.parse(splitted[0]), new URI(PfssSettings.BASE_URL + splitted[1]));
                        } catch (Exception e) {
                            Log.warn("Skipping malformed PFSS list entry: " + line, e);
                        }
                    }
                } catch (Exception e) { // continue in case of list error
                    Log.warn("PFSS list error", e);
                }
                EventQueue.invokeLater(() -> PfssPlugin.getPfsscache().put(uris));

                if (startMonth == 11) {
                    startMonth = 0;
                    startYear++;
                } else {
                    startMonth++;
                }
            } while (startYear < endYear || (startYear == endYear && startMonth <= endMonth));
            return null;
        }

    }

    private record CallbackList(long start) implements FutureCallback<Void> {
        @Override
        public void onSuccess(Void result) {
            PfssPlugin.downloads--;
            PfssPlugin.getPfsscache().getNearestData(start); // preload first
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            PfssPlugin.downloads--;
            Log.error(t);
        }
    }

    private static int findColumn(TableHDU<?> hdu, String name) throws Exception {
        int col = hdu.findColumn(name);
        if (col < 0)
            throw new Exception("Column not found: " + name);
        return col;
    }

    private static double decodeShort(short v) {
        return (v + 32768.) * (2. / 65535.) - 1.;
    }

    private record DataLoader(long time, URI uri) implements Callable<Data> {
        @Override
        public Data call() throws Exception {
            try (NetClient nc = NetClient.of(uri); Fits fits = new Fits(nc.getStream())) {
                BasicHDU<?>[] hdus = fits.read();
                if (hdus == null || hdus.length < 2 || !(hdus[1] instanceof TableHDU<?> hdu))
                    throw new Exception("Could not read FITS");

                Header header = hdu.getHeader();
                String dateFits = header.getStringValue("DATE-OBS");
                if (dateFits == null)
                    throw new Exception("DATE-OBS not found");
                JHVTime dateObs = new JHVTime(dateFits);
                if (time != dateObs.milli)
                    throw new Exception("Inconsistent DATE-OBS. Expected " + new JHVTime(time) + ", got " + dateObs);

                int points = header.getIntValue("HIERARCH.POINTS_PER_LINE");
                if (points == 0)
                    throw new Exception("POINTS_PER_LINE not found");

                int colX = findColumn(hdu, "FIELDLINEx");
                int colY = findColumn(hdu, "FIELDLINEy");
                int colZ = findColumn(hdu, "FIELDLINEz");
                int colS = findColumn(hdu, "FIELDLINEs");
                int rows = hdu.getNRows();

                float[] lineX = new float[rows];
                float[] lineY = new float[rows];
                float[] lineZ = new float[rows];
                float[] lineS = new float[rows];

                double elon = Sun.getEarth(dateObs).lon;
                double cphi = Math.cos(elon);
                double sphi = Math.sin(elon);

                for (int i = 0; i < rows; i++) {
                    double x = 3 * decodeShort(((short[]) hdu.getElement(i, colX))[0]);
                    double y = 3 * decodeShort(((short[]) hdu.getElement(i, colY))[0]);
                    double z = 3 * decodeShort(((short[]) hdu.getElement(i, colZ))[0]);
                    double s = decodeShort(((short[]) hdu.getElement(i, colS))[0]);

                    lineX[i] = (float) (cphi * x + sphi * y);
                    lineY[i] = (float) (-sphi * x + cphi * y);
                    lineZ[i] = (float) z;
                    lineS[i] = (float) Math.clamp(s, -1, 1);
                }
                return new Data(dateObs, lineX, lineY, lineZ, lineS, points);
            }
        }
    }

    private record CallbackData(URI uri) implements FutureCallback<Data> {
        @Override
        public void onSuccess(Data result) {
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
