package org.helioviewer.jhv.layers.fov;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.ProjectionScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportProjection;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.Transform;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONObject;

@SuppressWarnings("serial")
class FOVPlatform extends DefaultMutableTreeNode {

    static final double MIN_CENTER_ARCMIN = -60;
    static final double MAX_CENTER_ARCMIN = 60;
    private static final double LINEWIDTH_FOV = GLSLLine.LINEWIDTH_BASIC;
    private static final int SUBDIVISIONS = 180; // divisible by 4 so quarter-turn arcs align to exact step ranges
    private static final double HEMI_RADIUS = Sun.Radius + LINEWIDTH_FOV; // avoid intersecting solar surface

    private final GLSLLine hemiLine = new GLSLLine(false);
    private final GLSLLine instrumentLines = new GLSLLine(true);
    private final GLSLShape instrumentCenters = new GLSLShape(true);
    private final BufVertex lineBuf = new BufVertex(8 * (4 * (FOVShape.RECT_SUBDIVS + 1) + 2) * GLSLLine.stride);
    private final BufVertex centerBuf = new BufVertex(8 * GLSLShape.stride);

    private final String name;
    private final String observer;
    private final byte[] color;
    private final boolean isSOLO;

    private double centerX;
    private double centerY;

    FOVPlatform(String _name, String _observer, byte[] _color, JSONObject jo) {
        name = _name;
        observer = _observer;
        color = _color;
        isSOLO = "SOLO".equals(observer);

        centerX = Math.clamp(jo.optDouble("centerX", 0), MIN_CENTER_ARCMIN, MAX_CENTER_ARCMIN);
        centerY = Math.clamp(jo.optDouble("centerY", 0), MIN_CENTER_ARCMIN, MAX_CENTER_ARCMIN);
    }

    @Override
    public void add(MutableTreeNode newChild) {
        super.add(newChild);
        if (newChild instanceof FOVInstrument instrument) {
            instrument.setCenterX(control2Center(centerX));
            instrument.setCenterY(control2Center(centerY));
        }
    }

    private void putHemiLine() {
        BufVertex buf = new BufVertex(2 * (SUBDIVISIONS + 3) * GLSLLine.stride);
        GLHelper.emitCircle(HEMI_RADIUS, SUBDIVISIONS, 0, SUBDIVISIONS, null, color, Colors.White, buf);
        GLHelper.emitCircle(HEMI_RADIUS, SUBDIVISIONS, 0, SUBDIVISIONS / 2, Quat.X90, color, Colors.White, buf);
        GLHelper.emitCircle(HEMI_RADIUS, SUBDIVISIONS, SUBDIVISIONS / 4, 3 * SUBDIVISIONS / 4, Quat.Y90, color, Colors.White, buf);
        hemiLine.setVertex(buf);
    }

    void init() {
        hemiLine.init();
        putHemiLine();
        instrumentLines.init();
        instrumentCenters.init();
    }

    void dispose() {
        hemiLine.dispose();
        instrumentLines.dispose();
        instrumentCenters.dispose();
    }

    void render(MapContext ctx, Viewport vp, ProjectionScale scale) {
        if (!hasEnabled())
            return;

        JHVTime time = ctx.viewpoint().time;
        Position obsPosition = Spice.getCarrington(observer, time);
        if (obsPosition == null)
            return;

        Transform.pushView();
        Transform.rotateViewInverse(obsPosition.toQuat());

        hemiLine.renderLine(vp, LINEWIDTH_FOV);
        double[] rot;
        if (isSOLO && null != (rot = Spice.getRotationEuler("SOLO_SRF", "SOLO_IAU_SUN_2009", time))) {
            // Default SOLO pointing is normal to orbit
            // rot = Spice.getRotationEuler("SOLO_EQUAT_NORM", "SOLO_ORBIT_NORM", time)
            Transform.rotateView(Quat.createAxisZ(rot[2]));
        }

        SdfTextRenderer renderer = GLText.renderer();
        renderer.setColor(Colors.WhiteFloat);
        renderer.begin3DRendering();
        renderer.setSurfacePut();

        children().asIterator().forEachRemaining(c -> ((FOVInstrument) c).putGeometry(obsPosition.distance, color, renderer, lineBuf, centerBuf));

        instrumentCenters.setVertex(centerBuf);
        instrumentCenters.renderPoints(ViewportProjection.getPixelFactor(vp, ctx.cameraWidth(vp)));
        instrumentLines.setVertex(lineBuf);
        instrumentLines.renderLine(vp, LINEWIDTH_FOV);

        renderer.setDirectPut();
        renderer.end3DRendering();

        Transform.popView();
    }

    private boolean hasEnabled() {
        Enumeration<TreeNode> e = children();
        while (e.hasMoreElements()) {
            if (((FOVInstrument) e.nextElement()).isEnabled())
                return true;
        }
        return false;
    }

    private static double control2Center(double v) { // v in arcmin
        return Math.tan(v * (Math.PI / 180. / 60.));
    }

    double getCenterX() {
        return centerX;
    }

    double getCenterY() {
        return centerY;
    }

    void setCenterX(double centerX) {
        this.centerX = centerX;
        children().asIterator().forEachRemaining(c -> ((FOVInstrument) c).setCenterX(control2Center(centerX)));
        DisplayController.display();
    }

    void setCenterY(double centerY) {
        this.centerY = centerY;
        children().asIterator().forEachRemaining(c -> ((FOVInstrument) c).setCenterY(control2Center(centerY)));
        DisplayController.display();
    }

    JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("centerX", centerX);
        jo.put("centerY", centerY);

        Enumeration<TreeNode> e = children();
        while (e.hasMoreElements()) {
            FOVInstrument instrument = (FOVInstrument) e.nextElement();
            jo.put(instrument.toString(), instrument.isEnabled());
        }
        return jo;
    }

    @Override
    public String toString() {
        return name;
    }

}
