package org.helioviewer.jhv.layers.stars;

import java.net.URI;
import java.util.ArrayList;
// import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.astronomy.SpiceMath;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.time.JHVTime;

import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;

public class GaiaClient {

    static {
        Logger.getLogger("uk.ac.starlink").setLevel(Level.WARNING); // shut-up info logs
    }

    public static void submitSearch(ReceiverStars receiver, Position viewpoint) {
        EDTCallbackExecutor.pool.submit(new QueryTap(viewpoint), new CallbackTap(receiver, viewpoint));
    }

    public interface ReceiverStars {
        void setStars(Position viewpoint, List<Star> list);
    }

    // uk.ac.starlink.table.TimeMapper.DECIMAL_YEAR.toUnixSeconds(2015.5)
    // public static final double EPOCH_2015_5_SEC = 1435838400; // DR2
    private static final double EPOCH_2016_0_SEC = 1451606400; // DR3
    public static final double EPOCH = EPOCH_2016_0_SEC;
    private static final int SEARCH_CONE = 4;
    private static final int SEARCH_MAG = 9;

    private record StarRequest(int ra, int dec, int cone, int mag) {
    }

    public record Star(int id, double ra, double dec, double pmra, double pmdec, double mag) {
    }

    private static StarRequest computeRequest(Position viewpoint) {
        double[] sc = SpiceMath.radrec(-1, -viewpoint.lon, viewpoint.lat); // sc to Sun, lon was negated
        double[] search = SpiceMath.recrad(SpiceMath.mtxv(Spice.j2000ToSun.get(viewpoint.time), sc)); // Sun -> J2000
        double ra = Math.toDegrees(search[1]), dec = Math.toDegrees(search[2]);
        // reduce number of calls to catalog: divide the sky in 1x1deg, increase cone by 1deg
        return new StarRequest((int) ra, (int) dec, SEARCH_CONE + 1, SEARCH_MAG);
    }

    private static final UriTemplate queryTemplate = new UriTemplate("https://gea.esac.esa.int/tap-server/tap/sync",
            UriTemplate.vars().set("REQUEST", "doQuery").set("LANG", "ADQL").set("FORMAT", "fits"));

    private static String adqlSearch(int ra, int dec, int cone, int mag) {
        return "SELECT source_id,ra,dec,pmra,pmdec,phot_g_mean_mag FROM gaiadr3.gaia_source_lite WHERE " +
                String.format("1=CONTAINS(POINT('ICRS',ra,dec), CIRCLE('ICRS',%d,%d,%d)) AND phot_g_mean_mag<%d", ra, dec, cone, mag);
    }

    private record QueryTap(Position viewpoint) implements Callable<List<Star>> {
        @Override
        public List<Star> call() throws Exception {
            // return Collections.emptyList();
            StarRequest req = computeRequest(viewpoint);
            String adql = adqlSearch(req.ra, req.dec, req.cone, req.mag);

            URI uri = new URI(queryTemplate.expand(UriTemplate.vars().set("QUERY", adql)));
            try (NetClient nc = NetClient.of(uri);
                 StarTable table = new StarTableFactory().makeStarTable(nc.getStream(), new FitsTableBuilder())) {
                List<Star> stars = new ArrayList<>();
                try (RowSequence rseq = table.getRowSequence()) {
                    while (rseq.next()) {
                        int source_id = ((Number) rseq.getCell(0)).intValue();
                        double ra = ((Number) rseq.getCell(1)).doubleValue();
                        double dec = ((Number) rseq.getCell(2)).doubleValue();
                        double pmra = ((Number) rseq.getCell(3)).doubleValue();
                        double pmdec = ((Number) rseq.getCell(4)).doubleValue();
                        double mag = ((Number) rseq.getCell(5)).doubleValue();

                        pmra = Double.isFinite(pmra) ? pmra : 0;
                        pmdec = Double.isFinite(pmdec) ? pmdec : 0;
                        stars.add(new Star(source_id, ra, dec, pmra, pmdec, mag));
                    }
                }
                Log.info("Found " + stars.size() + " stars with " + adql);
                return stars;
            }
        }
    }

    private record CallbackTap(ReceiverStars receiver, Position viewpoint) implements FutureCallback<List<Star>> {
        @Override
        public void onSuccess(@Nonnull List<Star> result) {
            receiver.setStars(viewpoint, result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(Throwables.getStackTraceAsString(t));
            Message.err("An error occurred querying the server", t.getMessage());
        }
    }

}
