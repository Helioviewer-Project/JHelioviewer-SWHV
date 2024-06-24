package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.util.List;
import java.util.Optional;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.astronomy.SpiceMath;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.stars.GaiaClient;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;
import org.json.JSONObject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jogamp.opengl.GL2;

public final class StarLayer extends AbstractLayer implements Camera.Listener, GaiaClient.ReceiverStars {

    private final Cache<Position, Optional<BufVertex>> cache = Caffeine.newBuilder().softValues().build();

    private static final float SIZE_POINT = 0.04f;
    private final GLSLShape points = new GLSLShape(true);

    private static final int SEARCH_CONE = 4;
    private static final int SEARCH_MAG = 9;

    @Override
    public void serialize(JSONObject jo) {
    }

    public StarLayer(JSONObject jo) {
    }

    @Override
    public void viewpointChanged(Position viewpoint) {
        if (cache.getIfPresent(viewpoint) != null) // avoid repeated calls
            return;
        cache.put(viewpoint, Optional.empty()); // promise
        GaiaClient.submitSearch(this, viewpoint);
    }

    private static void putVertex(BufVertex pointsBuf, double Tx, double Ty, double dist, float size, byte[] color) {
        double x = dist * Math.tan(Tx);
        double y = dist * Math.tan(Ty);
        pointsBuf.putVertex((float) x, (float) y, 0, size, color);
    }

    private static void putPlanet(String planet, JHVTime time, double[] sc, double[] rsc, BufVertex pointsBuf) {
        double[] v = Spice.getPositionRec("SUN", planet, "SOLO_IAU_SUN_2009", time);
        v[0] -= rsc[0]; // sc->planet = sun->planet - sun->sc
        v[1] -= rsc[1];
        v[2] -= rsc[2];
        v = SpiceMath.recrad(v);

        double[] theta = new double[2];
        calcProj3(0, v[1], v[2], sc[1], sc[2], theta);
        putVertex(pointsBuf, theta[0], theta[1], sc[0], 2 * SIZE_POINT, Colors.Green);
    }

    @Override
    public void setStars(Position viewpoint, List<GaiaClient.Star> stars) {
        int num = stars.size();
        BufVertex pointsBuf = new BufVertex((num + 3) * GLSLShape.stride);

        double[] sc = new double[]{viewpoint.distance, -viewpoint.lon, viewpoint.lat}; // lon was negated
        double[] rsc = SpiceMath.radrec(sc[0], sc[1], sc[2]);

        JHVTime time = viewpoint.time;
        putPlanet("MERCURY", time, sc, rsc, pointsBuf);
        putPlanet("VENUS", time, sc, rsc, pointsBuf);
        putPlanet("MARS BARYCENTER", time, sc, rsc, pointsBuf);

        double dyr = (time.milli / 1000. - GaiaClient.EPOCH) / 86400. / 365.25;
        double mas_dyr = dyr / 1000. / 3600.;
        double[][] mat = Spice.j2000ToSun.get(time);

        double[] theta = new double[2];
        for (GaiaClient.Star star : stars) {
            double ra = Math.toRadians(star.ra());
            double dec = Math.toRadians(star.dec());

            // http://mingus.mmto.arizona.edu/~bjw/mmt/spectro_standards.html
            double pmra = Math.toRadians(star.pmra() * mas_dyr);
            double pmdec = Math.toRadians(star.pmdec() * mas_dyr);
            ra += pmra / Math.cos(dec);
            dec += pmdec;

            double[] s = SpiceMath.radrec(1, ra, dec);
            s = SpiceMath.mxv(mat, s); // to Carrington
            s = SpiceMath.recrad(s);

            calcProj3(0, s[1], s[2], sc[1], sc[2], theta);
            putVertex(pointsBuf, theta[0], theta[1], sc[0], 2 * SIZE_POINT, Colors.Blue);
        }

        cache.put(viewpoint, Optional.of(pointsBuf));
        MovieDisplay.display();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        Position viewpoint = camera.getViewpoint();
        Optional<BufVertex> optBuf = cache.getIfPresent(viewpoint);
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
            Display.getCamera().addListener(this);
        } else {
            Display.getCamera().removeListener(this);
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
        double rho = Math.acos(Math.cos(delta) * Math.cos(delta0) * Math.cos(alpha - alpha0) + Math.sin(delta) * Math.sin(delta0));

        theta[0] = Math.atan(-Math.tan(rho) * Math.sin(phi - P));
        theta[1] = Math.asin(Math.sin(rho) * Math.cos(phi - P));
    }

// --Commented out by Inspection START (23/06/2024, 23:15):
//    // all angles
//    private static void calcProj5(double P, double alpha, double delta, double alpha0, double delta0, double[] theta) {
//        double phi = Math.atan2(Math.sin(alpha - alpha0), Math.tan(delta) * Math.cos(delta0) - Math.sin(delta0) * Math.cos(alpha - alpha0));
//
//        double num = Math.hypot(Math.cos(delta) * Math.sin(alpha - alpha0), Math.cos(delta0) * Math.sin(delta) - Math.sin(delta0) * Math.cos(delta) * Math.cos(alpha - alpha0));
//        double den = Math.sin(delta) * Math.sin(delta0) + Math.cos(delta) * Math.cos(delta0) * Math.cos(alpha - alpha0);
//        double rho = Math.atan2(num, den);
//
//        theta[0] = Math.atan(-num / den * Math.sin(phi - P));
//        theta[1] = Math.asin(Math.sin(rho) * Math.cos(phi - P));
//    }
// --Commented out by Inspection STOP (23/06/2024, 23:15)

}
