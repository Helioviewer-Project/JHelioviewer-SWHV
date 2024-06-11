package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.json.JSONObject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jogamp.opengl.GL2;

public final class StarLayer extends AbstractLayer implements TimeListener.Change, Vizier.ReceiverStars {

    private final Cache<JHVTime, BufVertex> cache = Caffeine.newBuilder().softValues().build();

    private static final float SIZE_POINT = 0.10f;
    private final GLSLShape points = new GLSLShape(true);

    private static final double SEARCH_CONE = 4;
    private static final double SEARCH_MAG = 7;

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

        String sc = "STEREO AHEAD";
        double[] search = Spice.posRad(sc, "SUN", time); // HCRS in fact
        double dist = search[0];
        double ra = Math.toDegrees(search[1]);
        double dec = Math.toDegrees(search[2]);

        Vizier.submitSearch(this, time, sc, dist, new Vizier.StarRequest(ra, dec, SEARCH_CONE, SEARCH_MAG));
    }

    private static void putVertex(BufVertex pointsBuf, double[] radec, double dist, float size, byte[] color) {
        double x = -dist * Math.tan(radec[1]);
        double y = dist * Math.tan(radec[2]);
        pointsBuf.putVertex((float) x, (float) y, 0, size, color);
    }

    @Override
    public void setStars(JHVTime time, String sc, double dist, List<Vizier.Star> stars) {
        double[][] mat = Spice.twovecSun(sc, time);

        int num = stars.size();
        BufVertex pointsBuf = new BufVertex((num + 2) * GLSLShape.stride);

        double[] radec;

        radec = Spice.posRadM(sc, "MERCURY", time, mat);
        putVertex(pointsBuf, radec, dist, 2 * SIZE_POINT, Colors.Green);
        radec = Spice.posRadM(sc, "VENUS", time, mat);
        putVertex(pointsBuf, radec, dist, 2 * SIZE_POINT, Colors.Green);
        radec = Spice.posRadM(sc, "MARS BARYCENTER", time, mat);
        putVertex(pointsBuf, radec, dist, 2 * SIZE_POINT, Colors.Green);

        for (int i = 0; i < num; i++) {
            Vizier.Star star = stars.get(i);
            double ra = Math.toRadians(star.ra());
            double dec = Math.toRadians(star.dec());

            radec = Spice.radRotate(ra, dec, mat);
            putVertex(pointsBuf, radec, dist, 2 * SIZE_POINT, Colors.Blue);
        }
        cache.put(time, pointsBuf);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        Position viewpoint = camera.getViewpoint();
        BufVertex pointsBuf = cache.getIfPresent(viewpoint.time);
        if (pointsBuf == null)
            return;

        points.setVertexRepeatable(gl, pointsBuf);

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

}
