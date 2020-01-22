package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.base.scale.GridTransform;
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
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufCoord;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLTexture;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.GLTexture;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

// has to be public for state
public class SWEKLayer extends AbstractLayer implements TimespanListener, JHVEventHandler {

    private final SWEKPopupController controller = new SWEKPopupController(JHVFrame.getGLCanvas());
    private final JPanel optionsPanel;

    private static final int DIVPOINTS = 10;
    private static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private static final double LINEWIDTH_HIGHLIGHT = 2 * LINEWIDTH;

    private static final HashMap<String, GLTexture> iconCacheId = new HashMap<>();
    private static final double ICON_ALPHA = 0.7;
    private static final double ICON_SIZE = 0.1;
    private static final double ICON_SIZE_HIGHLIGHTED = 0.16;

    private static final float[][] texCoord = {{0, 1}, {1, 1}, {0, 0}, {1, 0}};

    private boolean icons = true;

    private final GLSLLine lineEvent = new GLSLLine(true);
    private final BufVertex bufEvent = new BufVertex(512 * GLSLLine.stride); // pre-allocate
    private final GLSLLine lineThick = new GLSLLine(true);
    private final BufVertex bufThick = new BufVertex(64 * GLSLLine.stride); // pre-allocate

    private final GLSLTexture glslTexture = new GLSLTexture();
    private final BufCoord texBuf = new BufCoord(8);

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

            GLTexture.copyBufferedImage(gl, bi);
            iconCacheId.put(key, tex);
        }
        tex.bind(gl);
    }

    private static void drawInterpolated(int mres, double r_start, double r_end, double t_start, double t_end, Quat q, BufVertex buf, byte[] color) {
        Vec3 v = new Vec3();
        for (int i = 0; i <= mres; i++) {
            double alpha = 1. - i / (double) mres;
            double r = alpha * r_start + (1 - alpha) * r_end;
            double theta = alpha * t_start + (1 - alpha) * t_end;

            v.x = r * Math.cos(theta);
            v.y = r * Math.sin(theta);
            Vec3 res = q.rotateInverseVector(v);

            if (i == 0) {
                buf.putVertex(res, Colors.Null);
            }
            buf.putVertex(res, color);
        }
        buf.repeatVertex(Colors.Null);
    }

    private void drawCactusArc(JHVRelatedEvents evtr, JHVEvent evt, long timestamp) {
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
        double thetaStart = principalAngle - angularWidth / 2.;
        double thetaEnd = principalAngle + angularWidth / 2.;

        BufVertex buf = evtr.isHighlighted() ? bufThick : bufEvent;
        byte[] color = Colors.bytes(evtr.getColor());

        drawInterpolated(angularResolution, distSun, distSun, thetaStart, principalAngle, q, buf, color);
        drawInterpolated(angularResolution, distSun, distSun, principalAngle, thetaEnd, q, buf, color);
        drawInterpolated(lineResolution, distSunBegin, distSun + 0.05, thetaStart, thetaStart, q, buf, color);
        drawInterpolated(lineResolution, distSunBegin, distSun + 0.05, principalAngle, principalAngle, q, buf, color);
        drawInterpolated(lineResolution, distSunBegin, distSun + 0.05, thetaEnd, thetaEnd, q, buf, color);

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
                texBuf.putCoord(q.rotateInverseVector(v), el);
            }
        }
    }

    private void drawPolygon(Camera camera, Viewport vp, JHVRelatedEvents evtr, JHVEvent evt) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        float[] points = pi.getBoundBox();
        if (points.length == 0) {
            return;
        }

        BufVertex buf = evtr.isHighlighted() ? bufThick : bufEvent;
        byte[] color = Colors.bytes(evtr.getColor());

        Position viewpoint = camera.getViewpoint();
        Vec3 pt = new Vec3();
        Vec2 previous = null;

        // draw bounds
        float[] oldBoundaryPoint3d = new float[0];
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
                            buf.putVertex(x, y, z, 1, Colors.Null);
                        }
                        buf.putVertex(x, y, z, 1, color);
                    } else {
                        pt.x = xnew / r;
                        pt.y = ynew / r;
                        pt.z = znew / r;
                        if (j == 0) {
                            previous = GLHelper.drawVertex(viewpoint, vp, pt, previous, buf, Colors.Null);
                        }
                        previous = GLHelper.drawVertex(viewpoint, vp, pt, previous, buf, color);
                    }
                }
                buf.repeatVertex(Colors.Null);
            }
            oldBoundaryPoint3d = new float[]{points[3 * i], points[3 * i + 1], points[3 * i + 2]};
        }
    }

    private void drawImage3d(double x, double y, double z, double width, double height) {
        y = -y;

        Vec3 targetDir = new Vec3(x, y, z);
        Quat q = Quat.rotate(Quat.createRotation(Math.atan2(x, z), Vec3.YAxis), Quat.createRotation(-Math.asin(y / targetDir.length()), Vec3.XAxis));

        double width2 = width / 2.;
        double height2 = height / 2.;
        Vec3 p0 = q.rotateVector(new Vec3(-width2, -height2, 0));
        Vec3 p1 = q.rotateVector(new Vec3(width2, -height2, 0));
        Vec3 p2 = q.rotateVector(new Vec3(-width2, height2, 0));
        Vec3 p3 = q.rotateVector(new Vec3(width2, height2, 0));
        p0.add(targetDir);
        p1.add(targetDir);
        p2.add(targetDir);
        p3.add(targetDir);

        texBuf.putCoord(p0, texCoord[0]);
        texBuf.putCoord(p1, texCoord[1]);
        texBuf.putCoord(p2, texCoord[2]);
        texBuf.putCoord(p3, texCoord[3]);
    }

    private void drawIcon(JHVRelatedEvents evtr, JHVEvent evt) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        Vec3 pt = pi.centralPoint();
        if (pt != null) {
            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            drawImage3d(pt.x, pt.y, pt.z, sz, sz);
        }
    }

    private void drawImageScale(double theta, double r, double width, double height) {
        double width2 = width / 4.;
        double height2 = height / 4.;

        texBuf.putCoord((float) (theta - width2), (float) (r - height2), 0, 1, texCoord[0]);
        texBuf.putCoord((float) (theta + width2), (float) (r - height2), 0, 1, texCoord[1]);
        texBuf.putCoord((float) (theta - width2), (float) (r + height2), 0, 1, texCoord[2]);
        texBuf.putCoord((float) (theta + width2), (float) (r + height2), 0, 1, texCoord[3]);
    }

    private void drawIconScale(Camera camera, Viewport vp, JHVRelatedEvents evtr, JHVEvent evt, GridScale scale, GridTransform xform) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        Vec3 pt = pi.centralPoint();
        if (pt != null) {
            Position viewpoint = camera.getViewpoint();
            pt = viewpoint.toQuat().rotateVector(pt);
            Vec2 tf = xform.transform(viewpoint, pt, scale);

            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            drawImageScale(tf.x * vp.aspect, tf.y, sz, sz);
        }
    }

    private void drawCactusArcScale(Viewport vp, JHVRelatedEvents evtr, JHVEvent evt, long timestamp, GridScale scale) {
        double angularWidthDegree = SWEKData.readCMEAngularWidthDegree(evt);
        double principalAngleDegree = SWEKData.readCMEPrincipalAngleDegree(evt) - 90;
        double speed = SWEKData.readCMESpeed(evt);
        double factor = Sun.RadiusMeter;
        double distSunBegin = 2.4;
        double distSun = distSunBegin + speed * (timestamp - evt.start) / factor;

        double thetaStart = MathUtils.mapTo0To360(principalAngleDegree - angularWidthDegree / 2.);
        double thetaEnd = MathUtils.mapTo0To360(principalAngleDegree + angularWidthDegree / 2.);

        BufVertex buf = evtr.isHighlighted() ? bufThick : bufEvent;
        byte[] color = Colors.bytes(evtr.getColor());

        float x = (float) (scale.getXValueInv(thetaStart) * vp.aspect);
        float y = (float) scale.getYValueInv(distSunBegin);
        buf.putVertex(x, y, 0, 1, Colors.Null);
        buf.repeatVertex(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        buf.putVertex(x, y, 0, 1, color);
        buf.repeatVertex(Colors.Null);

        x = (float) (scale.getXValueInv(principalAngleDegree) * vp.aspect);
        y = (float) scale.getYValueInv(distSunBegin);
        buf.putVertex(x, y, 0, 1, Colors.Null);
        buf.repeatVertex(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        buf.putVertex(x, y, 0, 1, color);
        buf.repeatVertex(Colors.Null);

        x = (float) (scale.getXValueInv(thetaEnd) * vp.aspect);
        y = (float) scale.getYValueInv(distSunBegin);
        buf.putVertex(x, y, 0, 1, Colors.Null);
        buf.repeatVertex(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        buf.putVertex(x, y, 0, 1, color);
        buf.repeatVertex(Colors.Null);

        y = (float) scale.getYValueInv(distSun);
        buf.putVertex(x, y, 0, 1, Colors.Null);
        buf.repeatVertex(color);

        x = (float) (scale.getXValueInv(thetaStart) * vp.aspect);
        buf.putVertex(x, y, 0, 1, color);
        buf.repeatVertex(Colors.Null);

        if (icons) {
            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            drawImageScale(scale.getXValueInv(principalAngleDegree) * vp.aspect, scale.getYValueInv(distSun), sz, sz);
        }
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

    private void renderEvents(Viewport vp, GL2 gl) {
        lineEvent.setData(gl, bufEvent);
        lineThick.setData(gl, bufThick);
        lineEvent.render(gl, vp.aspect, LINEWIDTH);
        lineThick.render(gl, vp.aspect, LINEWIDTH_HIGHLIGHT);
    }

    private void renderIcons(GL2 gl, List<JHVRelatedEvents> evs) {
        glslTexture.setData(gl, texBuf);
        int idx = 0;
        for (JHVRelatedEvents evtr : evs) {
            JHVEvent evt = evtr.getClosestTo(controller.currentTime);
            if (Display.mode == Display.DisplayMode.Latitudinal && evt.isCactus())
                continue;
            bindTexture(gl, evtr.getSupplier().getGroup());
            glslTexture.render(gl, GL2.GL_TRIANGLE_STRIP, Colors.floats(evtr.getColor(), ICON_ALPHA), idx, 4);
            idx += 4;
        }
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (isVisible[vp.idx]) {
            List<JHVRelatedEvents> evs = SWEKData.getActiveEvents(controller.currentTime);
            if (evs.isEmpty())
                return;

            for (JHVRelatedEvents evtr : evs) {
                JHVEvent evt = evtr.getClosestTo(controller.currentTime);
                if (evt.isCactus()) {
                    drawCactusArc(evtr, evt, controller.currentTime);
                } else {
                    drawPolygon(camera, vp, evtr, evt);
                    if (icons) {
                        drawIcon(evtr, evt);
                    }
                }
            }
            renderEvents(vp, gl);
            if (icons) {
                renderIcons(gl, evs);
            }
        }
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        if (isVisible[vp.idx]) {
            List<JHVRelatedEvents> evs = SWEKData.getActiveEvents(controller.currentTime);
            if (evs.isEmpty())
                return;

            for (JHVRelatedEvents evtr : evs) {
                JHVEvent evt = evtr.getClosestTo(controller.currentTime);
                if (evt.isCactus() && (Display.mode == Display.DisplayMode.LogPolar || Display.mode == Display.DisplayMode.Polar)) {
                    drawCactusArcScale(vp, evtr, evt, controller.currentTime, Display.mode.scale);
                } else {
                    drawPolygon(camera, vp, evtr, evt);
                    if (icons) {
                        drawIconScale(camera, vp, evtr, evt, Display.mode.scale, Display.mode.xform);
                    }
                }
            }
            renderEvents(vp, gl);
            if (icons) {
                renderIcons(gl, evs);
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
            Movie.addTimeListener(controller);
            JHVFrame.getInputController().addPlugin(controller);
        } else {
            JHVFrame.getInputController().removePlugin(controller);
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
        lineEvent.init(gl);
        lineThick.init(gl);
        glslTexture.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        lineEvent.dispose(gl);
        lineThick.dispose(gl);
        iconCacheId.values().forEach(el -> el.delete(gl));
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
            MovieDisplay.display();
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
            MovieDisplay.display();
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
