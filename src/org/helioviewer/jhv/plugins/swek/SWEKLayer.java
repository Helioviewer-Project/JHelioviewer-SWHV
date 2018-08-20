package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.base.Colors;
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
import org.helioviewer.jhv.opengl.GLSLLine;
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

    private static final HashMap<String, GLTexture> iconCacheId = new HashMap<>();
    private static final double ICON_ALPHA = 0.6;
    private static final double ICON_SIZE = 0.1;
    private static final double ICON_SIZE_HIGHLIGHTED = 0.16;

    private static final float texCoord[][] = {{0, 1}, {1, 1}, {0, 0}, {1, 0}};

    private boolean icons = true;

    private final GLSLLine glslLine = new GLSLLine();
    private final Buf lineBuf = new Buf(32 * GLSLLine.stride);
    private final GLSLTexture glslTexture = new GLSLTexture();
    private final Buf texBuf = new Buf(4 * GLSLTexture.stride);

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

            tex = new GLTexture(gl, GL2.GL_TEXTURE_2D, GLTexture.Unit.ZERO);
            tex.bind(gl);

            GLTexture.copyBufferedImage2D(gl, bi);
            iconCacheId.put(key, tex);
        }
        tex.bind(gl);
    }

    private void interPolatedDraw(int mres, double r_start, double r_end, double t_start, double t_end, Quat q, byte[] color) {
        Vec3 v = new Vec3();
        for (int i = 0; i <= mres; i++) {
            double alpha = 1. - i / (double) mres;
            double r = alpha * r_start + (1 - alpha) * r_end;
            double theta = alpha * t_start + (1 - alpha) * t_end;

            v.x = r * Math.cos(theta);
            v.y = r * Math.sin(theta);
            Vec3 res = q.rotateInverseVector(v);

            if (i == 0) {
                lineBuf.put4f(res).put4b(Colors.Null);
            }
            lineBuf.put4f(res).put4b(color);
        }
        lineBuf.repeat4f().put4b(Colors.Null);
    }

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
        byte[] color = Colors.bytes(evtr.getColor());

        double thetaStart = principalAngle - angularWidth / 2.;
        double thetaEnd = principalAngle + angularWidth / 2.;

        interPolatedDraw(angularResolution, distSun, distSun, thetaStart, principalAngle, q, color);
        interPolatedDraw(angularResolution, distSun, distSun, principalAngle, thetaEnd, q, color);

        interPolatedDraw(lineResolution, distSunBegin, distSun + 0.05, thetaStart, thetaStart, q, color);
        interPolatedDraw(lineResolution, distSunBegin, distSun + 0.05, principalAngle, principalAngle, q, color);
        interPolatedDraw(lineResolution, distSunBegin, distSun + 0.05, thetaEnd, thetaEnd, q, color);

        glslLine.setData(gl, lineBuf);
        glslLine.render(gl, vp, evtr.isHighlighted() ? LINEWIDTH_HIGHLIGHT : LINEWIDTH);

        if (icons) {
            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            Vec3 v = new Vec3();
            for (float[] el : texCoord) {
                double deltatheta = sz / distSun * (el[0] * 2 - 1);
                double deltar = sz * (el[1] * 2 - 1);
                double r = distSun - deltar;
                double theta = principalAngle - deltatheta;

                v.x = r * Math.cos(theta);
                v.y = r * Math.sin(theta);
                texBuf.put4f(q.rotateInverseVector(v)).put2f(el);
            }

            bindTexture(gl, evtr.getSupplier().getGroup());
            glslTexture.setData(gl, texBuf);
            glslTexture.render(gl, GL2.GL_TRIANGLE_STRIP, Colors.floats(evtr.getColor()), 4);
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

        byte[] color = Colors.bytes(evtr.getColor());
        // draw bounds
        Vec3 pt = new Vec3();
        float[] oldBoundaryPoint3d = new float[0];
        Vec2 previous = null;
        int plen = points.length / 3;
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
                            lineBuf.put4f(x, y, z, 1).put4b(Colors.Null);
                        }
                        lineBuf.put4f(x, y, z, 1).put4b(color);
                    } else {
                        pt.x = xnew / r;
                        pt.y = ynew / r;
                        pt.z = znew / r;
                        if (j == 0) {
                            previous = GLHelper.drawVertex(camera, vp, pt, previous, lineBuf, Colors.Null);
                        }
                        previous = GLHelper.drawVertex(camera, vp, pt, previous, lineBuf, color);
                    }
                }
                lineBuf.repeat4f().put4b(Colors.Null);
            }
            oldBoundaryPoint3d = new float[]{points[3 * i], points[3 * i + 1], points[3 * i + 2]};
        }

        glslLine.setData(gl, lineBuf);
        glslLine.render(gl, vp, evtr.isHighlighted() ? LINEWIDTH_HIGHLIGHT : LINEWIDTH);
    }

    private void drawIcon(GL2 gl, JHVRelatedEvents evtr, JHVEvent evt) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        Vec3 pt = pi.centralPoint();
        if (pt != null) {
            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            bindTexture(gl, evtr.getSupplier().getGroup());
            drawImage3d(gl, pt.x, pt.y, pt.z, sz, sz, Colors.floats(evtr.getColor(), ICON_ALPHA));
        }
    }

    private void drawImageScale(GL2 gl, double theta, double r, double width, double height, float[] color) {
        double width2 = width / 4.;
        double height2 = height / 4.;

        texBuf.put4f((float) (theta - width2), (float) (r - height2), 0, 1).put2f(texCoord[0]);
        texBuf.put4f((float) (theta + width2), (float) (r - height2), 0, 1).put2f(texCoord[1]);
        texBuf.put4f((float) (theta - width2), (float) (r + height2), 0, 1).put2f(texCoord[2]);
        texBuf.put4f((float) (theta + width2), (float) (r + height2), 0, 1).put2f(texCoord[3]);

        glslTexture.setData(gl, texBuf);
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

            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            bindTexture(gl, evtr.getSupplier().getGroup());
            drawImageScale(gl, tf.x * vp.aspect, tf.y, sz, sz, Colors.floats(evtr.getColor(), ICON_ALPHA));
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

        byte[] color = Colors.bytes(evtr.getColor());

        float x = (float) (scale.getXValueInv(thetaStart) * vp.aspect);
        float y = (float) scale.getYValueInv(distSunBegin);
        lineBuf.put4f(x, y, 0, 1).put4b(Colors.Null);
        lineBuf.repeat4f().put4b(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        lineBuf.put4f(x, y, 0, 1).put4b(color);
        lineBuf.repeat4f().put4b(Colors.Null);

        x = (float) (scale.getXValueInv(principalAngleDegree) * vp.aspect);
        y = (float) scale.getYValueInv(distSunBegin);
        lineBuf.put4f(x, y, 0, 1).put4b(Colors.Null);
        lineBuf.repeat4f().put4b(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        lineBuf.put4f(x, y, 0, 1).put4b(color);
        lineBuf.repeat4f().put4b(Colors.Null);

        x = (float) (scale.getXValueInv(thetaEnd) * vp.aspect);
        y = (float) scale.getYValueInv(distSunBegin);
        lineBuf.put4f(x, y, 0, 1).put4b(Colors.Null);
        lineBuf.repeat4f().put4b(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        lineBuf.put4f(x, y, 0, 1).put4b(color);
        lineBuf.repeat4f().put4b(Colors.Null);

        y = (float) scale.getYValueInv(distSun);
        lineBuf.put4f(x, y, 0, 1).put4b(Colors.Null);
        lineBuf.repeat4f().put4b(color);

        x = (float) (scale.getXValueInv(thetaStart) * vp.aspect);
        lineBuf.put4f(x, y, 0, 1).put4b(color);
        lineBuf.repeat4f().put4b(Colors.Null);

        glslLine.setData(gl, lineBuf);
        glslLine.render(gl, vp, evtr.isHighlighted() ? LINEWIDTH_HIGHLIGHT : LINEWIDTH);

        if (icons) {
            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            bindTexture(gl, evtr.getSupplier().getGroup());
            drawImageScale(gl, scale.getXValueInv(principalAngleDegree) * vp.aspect, scale.getYValueInv(distSun), sz, sz, Colors.floats(evtr.getColor()));
        }
    }

    private void drawImage3d(GL2 gl, double x, double y, double z, double width, double height, float[] color) {
        y = -y;

        double width2 = width / 2.;
        double height2 = height / 2.;

        Vec3 targetDir = new Vec3(x, y, z);

        Quat q = Quat.rotate(Quat.createRotation(Math.atan2(x, z), Vec3.YAxis), Quat.createRotation(-Math.asin(y / targetDir.length()), Vec3.XAxis));
        Vec3 p0 = q.rotateVector(new Vec3(-width2, -height2, 0));
        Vec3 p1 = q.rotateVector(new Vec3(width2, -height2, 0));
        Vec3 p2 = q.rotateVector(new Vec3(-width2, height2, 0));
        Vec3 p3 = q.rotateVector(new Vec3(width2, height2, 0));
        p0.add(targetDir);
        p1.add(targetDir);
        p2.add(targetDir);
        p3.add(targetDir);

        texBuf.put4f(p0).put2f(texCoord[0]);
        texBuf.put4f(p1).put2f(texCoord[1]);
        texBuf.put4f(p2).put2f(texCoord[2]);
        texBuf.put4f(p3).put2f(texCoord[3]);

        glslTexture.setData(gl, texBuf);
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
