package org.helioviewer.jhv.plugins.swek;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.FloatArray;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.base.scale.Transform;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.JHVEventCache;
import org.helioviewer.jhv.events.JHVEventHandler;
import org.helioviewer.jhv.events.JHVEventParameter;
import org.helioviewer.jhv.events.JHVPositionInformation;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLPolyline;
import org.helioviewer.jhv.opengl.GLSLTexture;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.GLTexture;
import org.helioviewer.jhv.position.Position;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

// has to be public for state
public class SWEKLayer extends AbstractLayer implements TimespanListener, JHVEventHandler {

    private final SWEKPopupController controller = new SWEKPopupController(ImageViewerGui.getGLComponent());
    private final JPanel optionsPanel;

    private static final int DIVPOINTS = 10;
    private static final double LINEWIDTH = 0.002;
    private static final double LINEWIDTH_HIGHLIGHT = 2 * LINEWIDTH;
    private static final double LINEWIDTH_CACTUS = LINEWIDTH;

    private static final HashMap<String, GLTexture> iconCacheId = new HashMap<>();
    private static final double ICON_SIZE = 0.1;
    private static final double ICON_SIZE_HIGHLIGHTED = 0.16;

    private boolean icons = true;

    private final GLSLPolyline glslLine = new GLSLPolyline();
    private final GLSLTexture glslTexture = new GLSLTexture();

    public SWEKLayer(JSONObject jo) {
        if (jo != null)
            icons = jo.optBoolean("icons", icons);
        else
            setEnabled(true);
        optionsPanel = optionsPanel();
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("icons", icons);
    }

    private static void bindTexture(GL2 gl, SWEKGroup group) {
        String key = group.getName();
        GLTexture tex = iconCacheId.get(key);
        if (tex == null) {
            ImageIcon icon = group.getIcon();
            BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics graph = bi.createGraphics();
            icon.paintIcon(null, graph, 0, 0);
            graph.dispose();

            tex = new GLTexture(gl);
            tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);

            GLTexture.copyBufferedImage2D(gl, bi);
            iconCacheId.put(key, tex);
        }
        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
    }

    private static void interPolatedDraw(int mres, double r_start, double r_end, double t_start, double t_end, Quat q, FloatArray pos, FloatArray col, float[] color) {
        Vec3 v = new Vec3();
        for (int i = 0; i <= mres; i++) {
            double alpha = 1. - i / (double) mres;
            double r = alpha * r_start + (1 - alpha) * r_end;
            double theta = alpha * t_start + (1 - alpha) * t_end;

            v.x = r * Math.cos(theta);
            v.y = r * Math.sin(theta);
            Vec3 res = q.rotateInverseVector(v);

            if (i == 0) {
                pos.put4f(res);
                col.put4f(BufferUtils.colorNull);
            }
            pos.put4f(res);
            col.put4f(color);
        }
        pos.repeat4f();
        col.put4f(BufferUtils.colorNull);
    }

    private final float texCoord[][] = {{0, 1}, {1, 1}, {0, 0}, {1, 0}};
    private final FloatBuffer texBuf = BufferUtils.newFloatBuffer(8);
    private final FloatBuffer vexBuf = BufferUtils.newFloatBuffer(16);

    private void drawCactusArc(Viewport vp, GL2 gl, JHVRelatedEvents evtr, JHVEvent evt, long timestamp) {
        double angularWidthDegree = SWEKData.readCMEAngularWidthDegree(evt);
        double angularWidth = Math.toRadians(angularWidthDegree);
        double principalAngleDegree = SWEKData.readCMEPrincipalAngleDegree(evt);
        double principalAngle = Math.toRadians(principalAngleDegree);
        double speed = SWEKData.readCMESpeed(evt);
        double factor = Sun.RadiusMeter;
        double distSunBegin = 2.4;
        double distSun = distSunBegin + speed * (timestamp - evt.start) / factor;
        int lineResolution = 2;
        int angularResolution = (int) (angularWidthDegree / 4);

        Quat q = evt.getPositionInformation().getEarth().toQuat();
        Color c = evtr.getColor();
        float[] color = {c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1};

        double thetaStart = principalAngle - angularWidth / 2.;
        double thetaEnd = principalAngle + angularWidth / 2.;

        FloatArray pos = new FloatArray();
        FloatArray col = new FloatArray();

        interPolatedDraw(angularResolution, distSun, distSun, thetaStart, principalAngle, q, pos, col, color);
        interPolatedDraw(angularResolution, distSun, distSun, principalAngle, thetaEnd, q, pos, col, color);

        interPolatedDraw(lineResolution, distSunBegin, distSun + 0.05, thetaStart, thetaStart, q, pos, col, color);
        interPolatedDraw(lineResolution, distSunBegin, distSun + 0.05, principalAngle, principalAngle, q, pos, col, color);
        interPolatedDraw(lineResolution, distSunBegin, distSun + 0.05, thetaEnd, thetaEnd, q, pos, col, color);

        glslLine.setData(gl, pos.toBuffer(), col.toBuffer());
        glslLine.render(gl, vp, LINEWIDTH_CACTUS);

        if (icons) {
            bindTexture(gl, evtr.getSupplier().getGroup());

            double sz = ICON_SIZE;
            if (evtr.isHighlighted()) {
                sz = ICON_SIZE_HIGHLIGHTED;
            }

            Vec3 v = new Vec3();
            for (float[] el : texCoord) {
                double deltatheta = sz / distSun * (el[0] * 2 - 1);
                double deltar = sz * (el[1] * 2 - 1);
                double r = distSun - deltar;
                double theta = principalAngle - deltatheta;

                v.x = r * Math.cos(theta);
                v.y = r * Math.sin(theta);
                Vec3 res = q.rotateInverseVector(v);
                BufferUtils.put4f(vexBuf, res);
            }
            vexBuf.rewind();

            glslTexture.setData(gl, vexBuf, texBuf);
            glslTexture.render(gl, GL2.GL_TRIANGLE_STRIP, color, 4);
        }
    }

    private void drawPolygon(Camera camera, Viewport vp, GL2 gl, JHVRelatedEvents evtr, JHVEvent evt) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        float[] points = pi.getBoundBox();
        if (points.length == 0) {
            return;
        }

        Color c = evtr.getColor();
        float[] color = {c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1};

        // draw bounds
        Vec3 pt = new Vec3();
        float[] oldBoundaryPoint3d = new float[0];
        Vec2 previous = null;
        int plen = points.length / 3;

        FloatArray pos = new FloatArray();
        FloatArray col = new FloatArray();
        for (int i = 0; i < plen; i++) {
            if (oldBoundaryPoint3d.length != 0) {
                for (int j = 0; j <= DIVPOINTS; j++) {
                    double alpha = 1. - j / (double) DIVPOINTS;
                    double xnew = alpha * oldBoundaryPoint3d[0] + (1 - alpha) * points[3 * i];
                    double ynew = alpha * oldBoundaryPoint3d[1] + (1 - alpha) * points[3 * i + 1];
                    double znew = alpha * oldBoundaryPoint3d[2] + (1 - alpha) * points[3 * i + 2];
                    double r = Math.sqrt(xnew * xnew + ynew * ynew + znew * znew);

                    if (Display.mode == Display.DisplayMode.Orthographic) {
                        float x = (float) (xnew / r);
                        float y = -(float) (ynew / r);
                        float z = (float) (znew / r);
                        if (j == 0) {
                            pos.put4f(x, y, z, 1);
                            col.put4f(BufferUtils.colorNull);
                        }
                        pos.put4f(x, y, z, 1);
                        col.put4f(color);
                    } else {
                        pt.x = xnew / r;
                        pt.y = ynew / r;
                        pt.z = znew / r;
                        if (j == 0) {
                            previous = GLHelper.drawVertex(camera, vp, pt, previous, pos, col, BufferUtils.colorNull);
                        }
                        previous = GLHelper.drawVertex(camera, vp, pt, previous, pos, col, color);
                    }
                }
                pos.repeat4f();
                col.put4f(BufferUtils.colorNull);
            }
            oldBoundaryPoint3d = new float[]{points[3 * i], points[3 * i + 1], points[3 * i + 2]};
        }

        glslLine.setData(gl, pos.toBuffer(), col.toBuffer());
        glslLine.render(gl, vp, evtr.isHighlighted() ? LINEWIDTH_HIGHLIGHT : LINEWIDTH);
    }

    private void drawIcon(GL2 gl, JHVRelatedEvents evtr, JHVEvent evt) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        Vec3 pt = pi.centralPoint();
        if (pt != null) {
            Color c = evtr.getColor();
            float alpha = 0.6f;
            float[] color = {c.getRed() / 255f * alpha, c.getGreen() / 255f * alpha, c.getBlue() / 255f * alpha, alpha};

            bindTexture(gl, evtr.getSupplier().getGroup());
            if (evtr.isHighlighted()) {
                drawImage3d(gl, pt.x, pt.y, pt.z, ICON_SIZE_HIGHLIGHTED, ICON_SIZE_HIGHLIGHTED, color);
            } else {
                drawImage3d(gl, pt.x, pt.y, pt.z, ICON_SIZE, ICON_SIZE, color);
            }
        }
    }

    private void drawImageScale(GL2 gl, double theta, double r, double width, double height, float[] color) {
        double width2 = width / 4.;
        double height2 = height / 4.;

        BufferUtils.put4f(vexBuf, (float) (theta - width2), (float) (r - height2), 0, 1);
        BufferUtils.put4f(vexBuf, (float) (theta + width2), (float) (r - height2), 0, 1);
        BufferUtils.put4f(vexBuf, (float) (theta - width2), (float) (r + height2), 0, 1);
        BufferUtils.put4f(vexBuf, (float) (theta + width2), (float) (r + height2), 0, 1);
        vexBuf.rewind();

        glslTexture.setData(gl, vexBuf, texBuf);
        glslTexture.render(gl, GL2.GL_TRIANGLE_STRIP, color, 4);
    }

    private void drawIconScale(Camera camera, Viewport vp, GL2 gl, JHVRelatedEvents evtr, JHVEvent evt, GridScale scale, Transform xform) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        Vec3 pt = pi.centralPoint();
        if (pt != null) {
            Position viewpoint = camera.getViewpoint();
            pt = viewpoint.toQuat().rotateVector(pt);
            Vec2 tf = xform.transform(viewpoint, pt, scale);

            Color c = evtr.getColor();
            float alpha = 0.6f;
            float[] color = {c.getRed() / 255f * alpha, c.getGreen() / 255f * alpha, c.getBlue() / 255f * alpha, alpha};

            bindTexture(gl, evtr.getSupplier().getGroup());
            if (evtr.isHighlighted()) {
                drawImageScale(gl, tf.x * vp.aspect, tf.y, ICON_SIZE_HIGHLIGHTED, ICON_SIZE_HIGHLIGHTED, color);
            } else {
                drawImageScale(gl, tf.x * vp.aspect, tf.y, ICON_SIZE, ICON_SIZE, color);
            }
        }
    }

    private void drawCactusArcScale(Viewport vp, GL2 gl, JHVRelatedEvents evtr, JHVEvent evt, long timestamp, GridScale scale) {
        double angularWidthDegree = SWEKData.readCMEAngularWidthDegree(evt);
        double principalAngleDegree = SWEKData.readCMEPrincipalAngleDegree(evt) - 90;
        double speed = SWEKData.readCMESpeed(evt);
        double factor = Sun.RadiusMeter;
        double distSunBegin = 2.4;
        double distSun = distSunBegin + speed * (timestamp - evt.start) / factor;

        double thetaStart = MathUtils.mapTo0To360(principalAngleDegree - angularWidthDegree / 2.);
        double thetaEnd = MathUtils.mapTo0To360(principalAngleDegree + angularWidthDegree / 2.);

        Color c = evtr.getColor();
        float[] color = {c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1};
        float x, y;

        FloatArray pos = new FloatArray();
        FloatArray col = new FloatArray();

        x = (float) (scale.getXValueInv(thetaStart) * vp.aspect);
        y = (float) scale.getYValueInv(distSunBegin);
        pos.put4f(x, y, 0, 1);
        col.put4f(BufferUtils.colorNull);
        pos.repeat4f();
        col.put4f(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        pos.put4f(x, y, 0, 1);
        col.put4f(color);
        pos.repeat4f();
        col.put4f(BufferUtils.colorNull);

        x = (float) (scale.getXValueInv(principalAngleDegree) * vp.aspect);
        y = (float) scale.getYValueInv(distSunBegin);
        pos.put4f(x, y, 0, 1);
        col.put4f(BufferUtils.colorNull);
        pos.repeat4f();
        col.put4f(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        pos.put4f(x, y, 0, 1);
        col.put4f(color);
        pos.repeat4f();
        col.put4f(BufferUtils.colorNull);

        x = (float) (scale.getXValueInv(thetaEnd) * vp.aspect);
        y = (float) scale.getYValueInv(distSunBegin);
        pos.put4f(x, y, 0, 1);
        col.put4f(BufferUtils.colorNull);
        pos.repeat4f();
        col.put4f(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        pos.put4f(x, y, 0, 1);
        col.put4f(color);
        pos.repeat4f();
        col.put4f(BufferUtils.colorNull);

        y = (float) scale.getYValueInv(distSun);
        pos.put4f(x, y, 0, 1);
        col.put4f(BufferUtils.colorNull);
        pos.repeat4f();
        col.put4f(color);

        x = (float) (scale.getXValueInv(thetaStart) * vp.aspect);
        pos.put4f(x, y, 0, 1);
        col.put4f(color);
        pos.repeat4f();
        col.put4f(BufferUtils.colorNull);

        glslLine.setData(gl, pos.toBuffer(), col.toBuffer());
        glslLine.render(gl, vp, LINEWIDTH_CACTUS);

        if (icons) {
            bindTexture(gl, evtr.getSupplier().getGroup());
            if (evtr.isHighlighted()) {
                drawImageScale(gl, scale.getXValueInv(principalAngleDegree) * vp.aspect, scale.getYValueInv(distSun), ICON_SIZE_HIGHLIGHTED, ICON_SIZE_HIGHLIGHTED, color);
            } else {
                drawImageScale(gl, scale.getXValueInv(principalAngleDegree) * vp.aspect, scale.getYValueInv(distSun), ICON_SIZE, ICON_SIZE, color);
            }
        }
    }

    private void drawImage3d(GL2 gl, double x, double y, double z, double width, double height, float[] color) {
        y = -y;

        double width2 = width / 2.;
        double height2 = height / 2.;

        Vec3 targetDir = new Vec3(x, y, z);

        Quat q = Quat.rotate(Quat.createRotation(Math.atan2(x, z), Vec3.YAxis), Quat.createRotation(-Math.asin(y / targetDir.length()), Vec3.XAxis));
        Vec3 p0 = q.rotateVector(new Vec3(-width2, -height2, 0));
        Vec3 p1 = q.rotateVector(new Vec3( width2, -height2, 0));
        Vec3 p2 = q.rotateVector(new Vec3(-width2,  height2, 0));
        Vec3 p3 = q.rotateVector(new Vec3( width2,  height2, 0));
        p0.add(targetDir);
        p1.add(targetDir);
        p2.add(targetDir);
        p3.add(targetDir);

        BufferUtils.put4f(vexBuf, p0);
        BufferUtils.put4f(vexBuf, p1);
        BufferUtils.put4f(vexBuf, p2);
        BufferUtils.put4f(vexBuf, p3);
        vexBuf.rewind();

        glslTexture.setData(gl, vexBuf, texBuf);
        glslTexture.render(gl, GL2.GL_TRIANGLE_STRIP, color, 4);
    }

    private static final int MOUSE_OFFSET_X = 25;
    private static final int MOUSE_OFFSET_Y = 25;

    private void drawText(Viewport vp, JHVRelatedEvents mouseOverJHVEvent, int x, int y) {
        ArrayList<String> txts = new ArrayList<>();
        for (JHVEventParameter p : mouseOverJHVEvent.getClosestTo(controller.currentTime).getSimpleVisibleEventParameters()) {
            String name = p.getParameterName();
            if (name != "event_description" && name != "event_title") { // interned
                txts.add(p.getParameterDisplayName() + " : " + p.getSimpleDisplayParameterValue());
            }
        }
        GLText.drawText(vp, txts, x + MOUSE_OFFSET_X, y + MOUSE_OFFSET_Y);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (isVisible[vp.idx]) {
            for (JHVRelatedEvents evtr : SWEKData.getActiveEvents(controller.currentTime)) {
                JHVEvent evt = evtr.getClosestTo(controller.currentTime);
                if (evt.isCactus()) {
                    drawCactusArc(vp, gl, evtr, evt, controller.currentTime);
                } else {
                    drawPolygon(camera, vp, gl, evtr, evt);
                    if (icons) {
                        gl.glDisable(GL2.GL_DEPTH_TEST);
                        drawIcon(gl, evtr, evt);
                        gl.glEnable(GL2.GL_DEPTH_TEST);
                    }
                }
            }
        }
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        if (isVisible[vp.idx]) {
            for (JHVRelatedEvents evtr : SWEKData.getActiveEvents(controller.currentTime)) {
                JHVEvent evt = evtr.getClosestTo(controller.currentTime);
                if (evt.isCactus() && (Display.mode == Display.DisplayMode.LogPolar || Display.mode == Display.DisplayMode.Polar)) {
                    drawCactusArcScale(vp, gl, evtr, evt, controller.currentTime, Display.mode.scale);
                } else {
                    drawPolygon(camera, vp, gl, evtr, evt);
                    if (icons) {
                        gl.glDisable(GL2.GL_DEPTH_TEST);
                        drawIconScale(camera, vp, gl, evtr, evt, Display.mode.scale, Display.mode.xform);
                        gl.glEnable(GL2.GL_DEPTH_TEST);
                    }
                }
            }
        }
    }

    @Override
    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        if (SWEKPopupController.mouseOverJHVEvent != null) {
            drawText(vp, SWEKPopupController.mouseOverJHVEvent, SWEKPopupController.mouseOverX, SWEKPopupController.mouseOverY);
        }
    }

    @Override
    public void remove(GL2 gl) {
        setEnabled(false);
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "SWEK Events";
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);

        if (enabled) {
            Movie.addTimespanListener(this);
            cacheUpdated();

            Movie.addTimeListener(controller);
            controller.timeChanged(Movie.getTime().milli);
            ImageViewerGui.getInputController().addPlugin(controller);
        } else {
            ImageViewerGui.getInputController().removePlugin(controller);
            Movie.removeTimeListener(controller);
            Movie.removeTimespanListener(this);
        }
    }

    @Nullable
    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
        for (float[] tc : texCoord)
            texBuf.put(tc);
        texBuf.rewind();
        glslLine.init(gl);
        glslTexture.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        glslLine.dispose(gl);
        glslTexture.dispose(gl);
        for (GLTexture el : iconCacheId.values())
            el.delete(gl);
        iconCacheId.clear();
    }

    private long startTime = Movie.getStartTime();
    private long endTime = Movie.getEndTime();

    private void requestEvents(boolean force, long start, long end) {
        if (force || start < startTime || end > endTime) {
            startTime = start;
            endTime = end;
            JHVEventCache.requestForInterval(start, end, this);
        }
    }

    @Override
    public void timespanChanged(long start, long end) {
        requestEvents(false, start, end);
    }

    @Override
    public void newEventsReceived() {
        if (enabled)
            Display.display();
    }

    @Override
    public void cacheUpdated() {
        requestEvents(true, Movie.getStartTime(), Movie.getEndTime());
    }

    private JPanel optionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JCheckBox check = new JCheckBox("Icons", icons);
        check.setHorizontalTextPosition(SwingConstants.LEFT);
        check.addActionListener(e -> {
            icons = !icons;
            Display.display();
        });

        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.CENTER;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        panel.add(check, c0);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
