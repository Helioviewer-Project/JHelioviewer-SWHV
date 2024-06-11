package org.helioviewer.jhv.layers;

import java.net.URI;
import java.util.ArrayList;
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

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.votable.VOTableBuilder;

import com.google.common.util.concurrent.FutureCallback;

class Vizier {

    static {
        Logger.getLogger("uk.ac.starlink").setLevel(Level.WARNING); // shut-up info logs
    }

    static void submitSearch(ReceiverStars receiver, JHVTime time, String sc, double dist, StarRequest request) {
        EDTCallbackExecutor.pool.submit(new QueryVizier(request), new CallbackVizier(receiver, time, sc, dist));
    }

    interface ReceiverStars {
        void setStars(JHVTime time, String sc, double dist, List<Star> list);
    }

    record StarRequest(double ra, double dec, double cone, double mag) {
    }

    record Star(double ra, double dec, int id, double pmra, double pmdec, double mag) {
    }

    private static final UriTemplate queryTemplate = new UriTemplate("https://tapvizier.cds.unistra.fr/TAPVizieR/tap/sync",
            UriTemplate.vars().set("REQUEST", "doQuery").set("LANG", "ADQL").set("FORMAT", "votable"));

    private static String adqlSearch(double ra, double dec, double cone, double mag) {
        return "SELECT ra,dec,source_id,pmra,pmdec,phot_g_mean_mag FROM \"I/345/gaia2\" WHERE " +
                String.format("1=CONTAINS(POINT('ICRS',ra,dec), CIRCLE('ICRS',%f,%f,%f)) AND phot_g_mean_mag<%f", ra, dec, cone, mag);
    }

    private static List<Star> getStars(StarRequest req) throws Exception {
        String adql = adqlSearch(req.ra, req.dec, req.cone, req.mag);
        URI uri = new URI(queryTemplate.expand(UriTemplate.vars().set("QUERY", adql)));

        int col_ra = 0;
        int col_dec = 1;
        int col_id = 2;
        int col_pmra = 3;
        int col_pmdec = 4;
        int col_mag = 5;

        try (NetClient nc = NetClient.of(uri);
             StarTable table = new StarTableFactory().makeStarTable(nc.getStream(), new VOTableBuilder())) {
            List<Star> stars = new ArrayList<>();
            try (RowSequence rseq = table.getRowSequence()) {
                while (rseq.next()) {
                    stars.add(new Star(
                            ((Number) rseq.getCell(col_ra)).doubleValue(),
                            ((Number) rseq.getCell(col_dec)).doubleValue(),
                            ((Number) rseq.getCell(col_id)).intValue(),
                            ((Number) rseq.getCell(col_pmra)).doubleValue(),
                            ((Number) rseq.getCell(col_pmdec)).doubleValue(),
                            ((Number) rseq.getCell(col_mag)).doubleValue()));
                }
            }
            Log.info("Found " + stars.size() + " stars with " + adql);
            return stars;
        }
    }

    private record QueryVizier(StarRequest request) implements Callable<List<Star>> {
        @Override
        public List<Star> call() throws Exception {
            return getStars(request);
        }
    }

    private record CallbackVizier(ReceiverStars receiver, JHVTime time, String sc,
                                  double dist) implements FutureCallback<List<Star>> {
        @Override
        public void onSuccess(@Nonnull List<Star> result) {
            receiver.setStars(time, sc, dist, result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred querying the server", t.getMessage());
        }
    }

}
