package org.helioviewer.jhv.layers.stars;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.astronomy.SpiceMath;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.threads.Tasks;
import org.helioviewer.jhv.time.JHVTime;

import org.jastronomy.jsofa.JSOFA;
import spice.basic.SpiceErrorException;

import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

public final class GaiaClient {

    static {
        Logger.getLogger("uk.ac.starlink").setLevel(Level.WARNING); // shut-up info logs
    }

    private GaiaClient() {
    }

    public static void submitSearch(Receiver receiver, Position viewpoint) {
        Tasks.submit("gaia", new Query(viewpoint), result -> onSuccess(receiver, viewpoint, result), (logContext, t) -> onFailure(receiver, viewpoint, t));
    }

    public interface Receiver {
        void setStars(Position viewpoint, BufVertex pointBuf);

        void setStarsFailed(Position viewpoint);
    }

    private static final float SIZE_STAR = 0.08f;
    private static final float SIZE_PLANET = 0.12f;
    private static final byte[] COLOR_PLANET = Colors.Green;
    private static final byte[] COLOR_STAR = Colors.Blue;

    private static final double JYEAR_SEC = 365.25 * 86400;
    // uk.ac.starlink.table.TimeMapper.DECIMAL_YEAR.toUnixSeconds(2015.5)
    // private static final double EPOCH_2015_5_SEC = 1435838400; // DR2
    private static final double EPOCH_2016_0_SEC = 1451606400; // DR3
    private static final double EPOCH = EPOCH_2016_0_SEC;

    private static final int SEARCH_CONE = 4;
    private static final int SEARCH_MAG = 9;

    private record StarRequest(int ra, int dec, int cone, int mag) {
    }

    private record Star(long id, double ra, double dec, double pmra, double pmdec, double px, double rv, double mag) {
    }

    private static StarRequest computeRequest(Position viewpoint) {
        double[] sc = SpiceMath.radrec(-1, -viewpoint.lon, viewpoint.lat); // sc to Sun, lon was negated
        double[] search = SpiceMath.recrad(SpiceMath.mtxv(Spice.j2000ToSun.get(viewpoint.time), sc)); // Sun -> J2000
        double ra = MathUtils.mapTo0To360(Math.toDegrees(search[1]));
        double dec = Math.toDegrees(search[2]);
        // reduce number of calls to catalog: divide the sky in 1x1deg bins, increase cone by 1deg
        return new StarRequest((int) ra, (int) dec, SEARCH_CONE + 1, SEARCH_MAG);
    }

    private static StarRequest computeRequestPrecise(String location, JHVTime time) throws SpiceErrorException {
        double[] search = SpiceMath.recrad(Spice.getPosition(location, "SUN", "J2000", time)); // HCRS
        double ra = MathUtils.mapTo0To360(Math.toDegrees(search[1]));
        double dec = Math.toDegrees(search[2]);
        // reduce number of calls to catalog: divide the sky in 1x1deg bins, increase cone by 1deg
        return new StarRequest((int) ra, (int) dec, SEARCH_CONE + 1, SEARCH_MAG);
    }

    private static void computePlanet(double[] rsc, String planet, JHVTime time, double[] sc, double[] theta, BufVertex vexBuf) throws SpiceErrorException {
        double[] pl = Spice.getPosition("SUN", planet, "SOLO_IAU_SUN_2009", time);
        pl[0] -= rsc[0]; // sc->planet = sun->planet - sun->sc
        pl[1] -= rsc[1];
        pl[2] -= rsc[2];
        pl = SpiceMath.recrad(pl);
        putPlanet(pl, sc, theta, vexBuf);
    }

    private static void computePlanetPrecise(String location, String planet, JHVTime time, double[] sc, double[] theta, BufVertex vexBuf) throws SpiceErrorException {
        double[] pl = SpiceMath.recrad(Spice.getPosition(location, planet, "SOLO_IAU_SUN_2009", time));
        putPlanet(pl, sc, theta, vexBuf);
    }

    private static BufVertex computePoints(Position viewpoint, List<Star> stars) throws SpiceErrorException {
        BufVertex vexBuf = new BufVertex(500 * GLSLShape.stride);
        JHVTime time = viewpoint.time;
        double[] sc = {viewpoint.distance, -viewpoint.lon, viewpoint.lat}; // lon was negated
        double[] theta = new double[2];

        double[] rsc = SpiceMath.radrec(sc[0], sc[1], sc[2]);
        computePlanet(rsc, "MERCURY", time, sc, theta, vexBuf);
        computePlanet(rsc, "VENUS", time, sc, theta, vexBuf);
        computePlanet(rsc, "MARS BARYCENTER", time, sc, theta, vexBuf);

        double dyr = (time.milli / 1000. - EPOCH) / JYEAR_SEC;
        double[][] carr = Spice.j2000ToSun.get(time);
        for (Star star : stars) {
            // http://mingus.mmto.arizona.edu/~bjw/mmt/spectro_standards.html
            double ra = star.ra() + star.pmra() * dyr;
            double dec = star.dec() + star.pmdec() * dyr;

            double[] st = SpiceMath.radrec(1, ra, dec);
            putStar(st, sc, carr, theta, vexBuf);
        }
        return vexBuf;
    }

    private static BufVertex computePointsPrecise(String location, JHVTime time, List<Star> stars) throws SpiceErrorException {
        BufVertex vexBuf = new BufVertex(500 * GLSLShape.stride);
        double[] sc = SpiceMath.recrad(Spice.getPosition(location, "SUN", "SOLO_IAU_SUN_2009", time));
        double[] theta = new double[2];

        computePlanetPrecise(location, "MERCURY", time, sc, theta, vexBuf);
        computePlanetPrecise(location, "VENUS", time, sc, theta, vexBuf);
        computePlanetPrecise(location, "MARS BARYCENTER", time, sc, theta, vexBuf);

        double[] ssb = Spice.getPosition("SSB", location, "J2000", time);
        ssb[0] *= Sun.MeanEarthDistanceInv; // [au]
        ssb[1] *= Sun.MeanEarthDistanceInv;
        ssb[2] *= Sun.MeanEarthDistanceInv;

        double[] sun = Spice.getPosition("SUN", location, "J2000", time);
        sun[0] /= sc[0]; // normalize
        sun[1] /= sc[0];
        sun[2] /= sc[0];
        double auDist = sc[0] * Sun.MeanEarthDistanceInv; // [au]

        double dyr = (time.milli / 1000. - EPOCH) / JYEAR_SEC;
        double[][] carr = Spice.j2000ToSun.get(time);
        for (Star star : stars) {
            double[] st;
            // Proper motion and parallax
            st = JSOFA.jauPmpx(star.ra(), star.dec(), star.pmra(), star.pmdec(), star.px(), star.rv(), dyr, ssb);
            // Deflection of starlight by the Sun
            st = JSOFA.jauLdsun(st, sun, auDist);
            // Stellar aberration correction not needed for JHV
            putStar(st, sc, carr, theta, vexBuf);
        }
        return vexBuf;
    }

    private static void putStar(double[] st /* rec */, double[] sc /* rad */, double[][] carr, double[] theta, BufVertex vexBuf) {
        st = SpiceMath.mxv(carr, st); // to Carrington
        st = SpiceMath.recrad(st);
        calcProj3(0, st[1], st[2], sc[1], sc[2], theta);
        putVertex(theta[0], theta[1], sc[0], SIZE_STAR, COLOR_STAR, vexBuf);
    }

    private static void putPlanet(double[] pl /* rad */, double[] sc /* rad */, double[] theta, BufVertex vexBuf) {
        calcProj3(0, pl[1], pl[2], sc[1], sc[2], theta);
        putVertex(theta[0], theta[1], sc[0], SIZE_PLANET, COLOR_PLANET, vexBuf);
    }

    private static void putVertex(double Tx, double Ty, double dist, float size, byte[] color, BufVertex vexBuf) {
        double x = dist * Math.tan(Tx);
        double y = dist * Math.tan(Ty);
        vexBuf.putVertex((float) x, (float) y, 0, size, color);
    }

// --Commented out by Inspection START (23/06/2024, 23:15):
//    // https://celestialscenes.com/alma/convert/coordconvert.pdf
//    // small angles
//    private static void calcProj1(double P, double alpha, double delta, double alpha0, double delta0, double[] theta) {
//        double sP = Math.sin(P);
//        double cP = Math.cos(P);
//        double alphaCosDelta = (alpha - alpha0) * Math.cos(delta0);
//        theta[0] = -alphaCosDelta * cP + (delta - delta0) * sP;
//        theta[1] = alphaCosDelta * sP + (delta - delta0) * cP;
//    }
// --Commented out by Inspection STOP (23/06/2024, 23:15)

    // big angles
    private static void calcProj3(double P, double alpha, double delta, double alpha0, double delta0, double[] theta) {
        double phi = Math.atan2(Math.sin(alpha - alpha0), Math.tan(delta) * Math.cos(delta0) - Math.sin(delta0) * Math.cos(alpha - alpha0));

        double clamp = Math.clamp(Math.cos(delta) * Math.cos(delta0) * Math.cos(alpha - alpha0) + Math.sin(delta) * Math.sin(delta0), -1, 1);
        double rho = Math.acos(clamp);

        theta[0] = Math.atan(-Math.tan(rho) * Math.sin(phi - P));

        clamp = Math.clamp(Math.sin(rho) * Math.cos(phi - P), -1, 1);
        theta[1] = Math.asin(clamp);
    }

// --Commented out by Inspection START (23/06/2024, 23:15):
//    // all angles
//    private static void calcProj5(double P, double alpha, double delta, double alpha0, double delta0, double[] theta) {
//        double phi = Math.atan2(Math.sin(alpha - alpha0), Math.tan(delta) * Math.cos(delta0) - Math.sin(delta0) * Math.cos(alpha - alpha0));
//
//        double num = Math.hypot(Math.cos(delta) * Math.sin(alpha - alpha0), Math.cos(delta0) * Math.sin(delta) - Math.sin(delta0) * Math.cos(delta) * Math.cos(alpha
//        double den = Math.sin(delta) * Math.sin(delta0) + Math.cos(delta) * Math.cos(delta0) * Math.cos(alpha - alpha0);
//        double rho = Math.atan2(num, den);
//
//        theta[0] = Math.atan(-num / den * Math.sin(phi - P));
//        theta[1] = Math.asin(Math.sin(rho) * Math.cos(phi - P));
//    }
// --Commented out by Inspection STOP (23/06/2024, 23:15)

    private static final LoadingCache<String, List<Star>> starCache = Caffeine.newBuilder().maximumSize(1000)
            .build(adql -> {
                try {
                    return requestStars(adql);
                } catch (Exception e) {
                    throw new RuntimeException("Gaia query failed for " + adql, e);
                }
            });

    private static final UriTemplate queryTemplate = new UriTemplate("https://gea.esac.esa.int/tap-server/tap/sync",
            UriTemplate.vars().set("REQUEST", "doQuery").set("LANG", "ADQL").set("FORMAT", "fits"));

    private static String adqlSearch(int ra, int dec, int cone, int mag) {
        return "SELECT source_id,ra,dec,pmra,pmdec,parallax,radial_velocity,phot_g_mean_mag FROM gaiadr3.gaia_source_lite WHERE " +
                String.format("1=CONTAINS(POINT('ICRS',ra,dec), CIRCLE('ICRS',%d,%d,%d)) AND phot_g_mean_mag<%d", ra, dec, cone, mag);
    }

    private static List<Star> requestStars(String adql) throws Exception {
        String uri = queryTemplate.expand(UriTemplate.vars().set("QUERY", adql));
        try (NetClient nc = NetClient.of(new URI(uri));
             StarTable table = new StarTableFactory().makeStarTable(nc.getStream(), new FitsTableBuilder())) {
            List<Star> stars = new ArrayList<>();
            try (RowSequence rseq = table.getRowSequence()) {
                while (rseq.next()) {
                    try {
                        Object[] row = rseq.getRow();
                        // https://gea.esac.esa.int/archive/documentation/GDR3/Gaia_archive/chap_datamodel/sec_dm_main_source_catalogue/ssec_dm_gaia_source.html
                        long source_id = ((Number) row[0]).longValue();
                        double ra = Math.toRadians(((Number) row[1]).doubleValue()); // [rad]
                        double dec = Math.toRadians(((Number) row[2]).doubleValue()); // [rad]
                        double pmra = Math.toRadians(((Number) row[3]).doubleValue() / (1000. * 3600.)) / Math.cos(dec); // [rad/yr], dRA/dt instead of cos(Dec)*dRA/dt
                        double pmdec = Math.toRadians(((Number) row[4]).doubleValue() / (1000. * 3600.)); // [rad/yr]
                        double px = ((Number) row[5]).doubleValue() / 1000.; // [arcsec]
                        double rv = ((Number) row[6]).doubleValue(); // [km/s]
                        double mag = ((Number) row[7]).doubleValue();

                        pmra = Double.isFinite(pmra) ? pmra : 0;
                        pmdec = Double.isFinite(pmdec) ? pmdec : 0;
                        px = Double.isFinite(px) ? px : 0;
                        rv = Double.isFinite(rv) ? rv : 0;
                        stars.add(new Star(source_id, ra, dec, pmra, pmdec, px, rv, mag));
                    } catch (Exception e) {
                        Log.warn("Skipping malformed Gaia row", e);
                    }
                }
            }
            Log.info("Found " + stars.size() + " stars with " + adql);
            return stars;
        }
    }

    private record Query(Position viewpoint) implements Callable<BufVertex> {
        @Override
        public BufVertex call() throws SpiceErrorException {
            String location = viewpoint.getLocation();
            if (location == null) {
                StarRequest req = computeRequest(viewpoint);
                String adql = adqlSearch(req.ra, req.dec, req.cone, req.mag);
                return computePoints(viewpoint, starCache.get(adql));
            } else {
                JHVTime time = viewpoint.time;
                StarRequest req = computeRequestPrecise(location, time);
                String adql = adqlSearch(req.ra, req.dec, req.cone, req.mag);
                return computePointsPrecise(location, time, starCache.get(adql));
            }
        }
    }

    private static void onSuccess(Receiver receiver, Position viewpoint, BufVertex result) {
        receiver.setStars(viewpoint, result);
    }

    private static void onFailure(Receiver receiver, Position viewpoint, Throwable t) {
        receiver.setStarsFailed(viewpoint);
        Log.error(t);
        Message.err("An error occurred querying the server", t.getMessage());
    }

}
