package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.util.List;
import java.util.Optional;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.astronomy.SpiceMath;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.GaiaClient;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.json.JSONObject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jogamp.opengl.GL2;

import org.jastronomy.jsofa.JSOFA;

public final class StarLayer extends AbstractLayer implements TimeListener.Change, GaiaClient.ReceiverStars {

    private final Cache<JHVTime, Optional<BufVertex>> cache = Caffeine.newBuilder().softValues().build();

    private static final float SIZE_POINT = 0.05f;
    private final GLSLShape points = new GLSLShape(true);

    private static final double SEARCH_CONE = 4;
    private static final double SEARCH_MAG = 9;

    @Override
    public void serialize(JSONObject jo) {
    }

    public StarLayer(JSONObject jo) {
    }

    @Override
    public void timeChanged(long milli) {
        JHVTime time = new JHVTime(milli);
        if (cache.getIfPresent(time) != null) // avoid repeated calls
            return;
        cache.put(time, Optional.empty()); // promise

        String sc = "STEREO AHEAD";
        double[] search = Spice.getPositionRad(sc, "SUN", "J2000", time); // HCRS in fact
        double ra = Math.toDegrees(search[1]);
        double dec = Math.toDegrees(search[2]);

        GaiaClient.submitSearch(this, time, sc, new GaiaClient.StarRequest(ra, dec, SEARCH_CONE, SEARCH_MAG));
    }

    private static void putVertex(BufVertex pointsBuf, double Tx, double Ty, double dist, float size, byte[] color) {
        double x = dist * Math.tan(Tx);
        double y = dist * Math.tan(Ty);
        pointsBuf.putVertex((float) x, (float) y, 0, size, color);
    }

    private static void putPlanet(String sc, String planet, JHVTime time, double[][] mat, double[] scPos, BufVertex pointsBuf) {
        double[] v = Spice.getPositionRect(sc, planet, "J2000", time);
        v = SpiceMath.mxv(mat, v); // to Carrington
        v = SpiceMath.recrad(v);

        double[] theta = new double[2];
        calcProj(0, v[1], v[2], scPos[1], scPos[2], theta);
        putVertex(pointsBuf, theta[0], theta[1], scPos[0], 2 * SIZE_POINT, Colors.Green);
    }

    @Override
    public void setStars(JHVTime time, String sc, List<GaiaClient.Star> stars) {
        double dyr = (time.milli / 1000. - GaiaClient.EPOCH) / 86400. / 365.25;
        //double mas_dyr = dyr / 1000. / 3600.;

        double[][] mat = Spice.j2000ToSun.get(time);
        double[] scPos = Spice.getPositionRad(sc, "SUN", "SOLO_IAU_SUN_2009", time);

        double[] ssb = Spice.getState("SSB", sc, "J2000", time);
        ssb[0] *= Sun.MeanEarthDistanceInv; // au
        ssb[1] *= Sun.MeanEarthDistanceInv;
        ssb[2] *= Sun.MeanEarthDistanceInv;
        double[] vel = new double[]{ssb[3], ssb[4], ssb[5]}; // c
        double bm1 = Math.sqrt(1 - vel[0] * vel[0] + vel[1] * vel[1] + vel[2] * vel[2]);

        double[] sun = Spice.getPositionRect("SUN", sc, "J2000", time);
        sun[0] /= scPos[0];
        sun[1] /= scPos[0];
        sun[2] /= scPos[0];
        double auDist = scPos[0] * Sun.MeanEarthDistanceInv; // au

        int num = stars.size();
        BufVertex pointsBuf = new BufVertex((num + 3) * GLSLShape.stride);

        putPlanet(sc, "MERCURY", time, mat, scPos, pointsBuf);
        putPlanet(sc, "VENUS", time, mat, scPos, pointsBuf);
        putPlanet(sc, "MARS BARYCENTER", time, mat, scPos, pointsBuf);

        for (int i = 0; i < num; i++) {
            GaiaClient.Star star = stars.get(i);
            double ra = Math.toRadians(star.ra());
            double dec = Math.toRadians(star.dec());

            // http://mingus.mmto.arizona.edu/~bjw/mmt/spectro_standards.html
            //double pmra = Math.toRadians(star.pmra() * mas_dyr);
            //double pmdec = Math.toRadians(star.pmdec() * mas_dyr);
            //ra += pmra / Math.cos(dec);
            //dec += pmdec;

            double[] s;
            // Proper motion and parallax
            s = JSOFA.jauPmpx(ra, dec, Math.toRadians(star.pmra() / (1000. * 3600.)) / Math.cos(dec), Math.toRadians(star.pmdec() / (1000. * 3600.)), star.px() / 1000., star.rv(), dyr, ssb);
            // Deflection of starlight by the Sun
            s = JSOFA.jauLdsun(s, sun, auDist);
            // Apply stellar aberration (natural direction to proper direction)
            s = JSOFA.jauAb(s, vel, auDist, bm1);

            if (Double.isFinite(s[0]) && Double.isFinite(s[1]) && Double.isFinite(s[2])) {
                s = SpiceMath.mxv(mat, s);
                s = SpiceMath.recrad(s);

                double[] theta = new double[2];
                calcProj(0, s[1], s[2], scPos[1], scPos[2], theta);
                putVertex(pointsBuf, theta[0], theta[1], scPos[0], 2 * SIZE_POINT, Colors.Blue);
            }
        }

        cache.put(time, Optional.of(pointsBuf));
        MovieDisplay.display();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        Position viewpoint = camera.getViewpoint();
        Optional<BufVertex> optBuf = cache.getIfPresent(viewpoint.time);
        if (optBuf == null || optBuf.isEmpty())
            return;

        points.setVertexRepeatable(gl, optBuf.get());

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());
        points.renderPoints(gl, CameraHelper.getPixelFactor(camera, vp));
        Transform.popView();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);

        if (enabled) {
            Movie.addTimeListener(this);
        } else {
            Movie.removeTimeListener(this);
        }
    }

    @Override
    public void init(GL2 gl) {
        points.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        points.dispose(gl);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return "Stars";
    }

    // https://celestialscenes.com/alma/convert/coordconvert.pdf
    private static void calcProj(double P, double alpha, double delta, double alpha0, double delta0, double[] theta) {
        double phi = Math.atan2(Math.sin(alpha - alpha0), Math.tan(delta) * Math.cos(delta0) - Math.sin(delta0) * Math.cos(alpha - alpha0));

        double num = Math.hypot(Math.cos(delta) * Math.sin(alpha - alpha0), Math.cos(delta0) * Math.sin(delta) - Math.sin(delta0) * Math.cos(delta) * Math.cos(alpha - alpha0));
        double den = Math.sin(delta) * Math.sin(delta0) + Math.cos(delta) * Math.cos(delta0) * Math.cos(alpha - alpha0);
        double rho = Math.atan2(num, den);

        theta[0] = Math.atan(-num / den * Math.sin(phi - P));
        theta[1] = Math.asin(Math.sin(rho) * Math.cos(phi - P));
    }

    private static void calcProjSimple(double P, double alpha, double delta, double alpha0, double delta0, double[] theta) {
        double sP = Math.sin(P);
        double cP = Math.cos(P);
        double alphaCosDelta = (alpha - alpha0) * Math.cos(delta0);
        theta[0] = -alphaCosDelta * cP + (delta - delta0) * sP;
        theta[1] = alphaCosDelta * sP + (delta - delta0) * cP;
    }

}
