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
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.time.JHVTime;

import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

import com.google.common.util.concurrent.FutureCallback;

public class GaiaClient {

    static {
        Logger.getLogger("uk.ac.starlink").setLevel(Level.WARNING); // shut-up info logs
    }

    // uk.ac.starlink.table.TimeMapper.DECIMAL_YEAR.toUnixSeconds(2015.5)
    // public static final double EPOCH_2015_5_SEC = 1435838400; // DR2
    private static final double EPOCH_2016_0_SEC = 1451606400; // DR3
    public static final double EPOCH = EPOCH_2016_0_SEC;

    public static void submitSearch(ReceiverStars receiver, JHVTime time, StarRequest request) {
        EDTCallbackExecutor.pool.submit(new QueryTap(request), new CallbackTap(receiver, time));
    }

    public interface ReceiverStars {
        void setStars(JHVTime time, List<Star> list);
    }

    public record StarRequest(double ra, double dec, double cone, double mag) {
    }

    public record Star(int id, double ra, double dec, double pmra, double pmdec, double px, double rv, double mag) {
    }

    private static final UriTemplate queryTemplate = new UriTemplate("https://gea.esac.esa.int/tap-server/tap/sync",
            UriTemplate.vars().set("REQUEST", "doQuery").set("LANG", "ADQL").set("FORMAT", "fits"));

    private static String adqlSearch(double ra, double dec, double cone, double mag) {
        return "SELECT source_id,ra,dec,pmra,pmdec,parallax,radial_velocity,phot_g_mean_mag FROM gaiadr3.gaia_source_lite WHERE " +
                String.format("1=CONTAINS(POINT('ICRS',ra,dec), CIRCLE('ICRS',%f,%f,%f)) AND phot_g_mean_mag<%f", ra, dec, cone, mag);
    }

    private record QueryTap(StarRequest req) implements Callable<List<Star>> {
        @Override
        public List<Star> call() throws Exception {
            // return Collections.emptyList();
            String adql = adqlSearch(req.ra, req.dec, req.cone, req.mag);

            URI uri = new URI(queryTemplate.expand(UriTemplate.vars().set("QUERY", adql)));
            try (NetClient nc = NetClient.of(uri);
                 StarTable table = new StarTableFactory().makeStarTable(nc.getStream(), new FitsTableBuilder())) {
                List<Star> stars = new ArrayList<>();
                try (RowSequence rseq = table.getRowSequence()) {
                    while (rseq.next()) {
                        stars.add(new Star(
                                ((Number) rseq.getCell(0)).intValue(),
                                ((Number) rseq.getCell(1)).doubleValue(),
                                ((Number) rseq.getCell(2)).doubleValue(),
                                ((Number) rseq.getCell(3)).doubleValue(),
                                ((Number) rseq.getCell(4)).doubleValue(),
                                ((Number) rseq.getCell(5)).doubleValue(),
                                ((Number) rseq.getCell(6)).doubleValue(),
                                ((Number) rseq.getCell(7)).doubleValue()));
                    }
                }
                Log.info("Found " + stars.size() + " stars with " + adql);
                return stars;
            }
        }
    }

    private record CallbackTap(ReceiverStars receiver, JHVTime time) implements FutureCallback<List<Star>> {
        @Override
        public void onSuccess(@Nonnull List<Star> result) {
            receiver.setStars(time, result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred querying the server", t.getMessage());
        }
    }

}
