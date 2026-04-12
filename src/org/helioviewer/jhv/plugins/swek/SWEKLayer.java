package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridScale;
import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.JHVEventCache;
import org.helioviewer.jhv.events.JHVEventListener;
import org.helioviewer.jhv.events.JHVEventParameter;
import org.helioviewer.jhv.events.JHVPositionInformation;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.imagedata.nio.NativeImageFactory;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufCoord;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLTexture;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.GLTexture;
import org.helioviewer.jhv.time.TimeListener;

import org.json.JSONObject;

// has to be public for state
public final class SWEKLayer extends AbstractLayer implements JHVEventListener.Handle, TimeListener.Range {
    private record CactusArcParams(double angularWidthDegree, double principalAngleDegree, double distSun) {
    }

    private final SWEKPopupController controller = new SWEKPopupController(JHVFrame.getRenderComponent());
    private final JPanel optionsPanel;

    private static final int DIVPOINTS = 10;
    private static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private static final double LINEWIDTH_HIGHLIGHT = 2 * LINEWIDTH;
    private static final double POLYGON_RADIUS = Sun.Radius * 1.01;
    private static final double DIST_SUN_BEGIN = 2.4;

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

    private static void bindTexture(SWEKGroup group) {
        String key = group.getName();
        GLTexture tex = iconCacheId.get(key);
        if (tex == null) {
            ImageIcon icon = group.getIcon();
            BufferedImage bi = NativeImageFactory.createRGBAPremultipliedImage(icon.getIconWidth(), icon.getIconHeight());
            try {
                Graphics g = bi.createGraphics();
                try {
                    icon.paintIcon(null, g, 0, 0);
                } finally {
                    g.dispose();
                }

                tex = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.THREE);
                tex.bind();

                ByteBuffer data = NativeImageFactory.getByteBuffer(bi);
                GLTexture.copyByteImage(bi.getWidth(), bi.getHeight(), GL.LINEAR, data);
            } finally {
                NativeImageFactory.free(bi);
            }
            iconCacheId.put(key, tex);
        }
        tex.bind();
    }

    private static void drawInterpolated(int mres, double r_start, double r_end, double t_start, double t_end, Quat q, byte[] color, BufVertex vexBuf) {
        int steps = Math.max(1, mres);
        for (int i = 0; i <= steps; i++) {
            double alpha = 1. - i / (double) steps;
            double r = alpha * r_start + (1 - alpha) * r_end;
            double theta = alpha * t_start + (1 - alpha) * t_end;

            Vec3 res = q.rotateInverseVector(PolarBasis.vec3(r, theta));

            if (i == 0) {
                vexBuf.putVertex(res, Colors.Null);
            }
            vexBuf.putVertex(res, color);
        }
        vexBuf.repeatVertex(Colors.Null);
    }

    private static CactusArcParams cactusArcParams(JHVEvent evt, long timestamp) {
        double angularWidthDegree = SWEKData.readCMEAngularWidthDegree(evt);
        double principalAngleDegree = SWEKData.readCMEPrincipalAngleDegree(evt);
        double speed = SWEKData.readCMESpeed(evt);
        double distSun = DIST_SUN_BEGIN + speed * (timestamp - evt.start) / Sun.RadiusMeter;
        return new CactusArcParams(angularWidthDegree, principalAngleDegree, distSun);
    }

    private void drawCactusArc(JHVRelatedEvents evtr, JHVEvent evt, long timestamp) {
        CactusArcParams params = cactusArcParams(evt, timestamp);
        double angularWidthDegree = params.angularWidthDegree();
        double angularWidth = Math.toRadians(angularWidthDegree);
        double principalAngleDegree = params.principalAngleDegree();
        double principalAngle = Math.toRadians(principalAngleDegree);
        double distSun = params.distSun();
        int lineResolution = 2;
        int angularResolution = (int) (angularWidthDegree / 4);

        Quat q = evt.getPositionInformation().getEarth().toQuat();
        double thetaStart = principalAngle - angularWidth / 2.;
        double thetaEnd = principalAngle + angularWidth / 2.;

        BufVertex vexBuf = evtr.isHighlighted() ? bufThick : bufEvent;
        byte[] color = Colors.bytes(evtr.getColor());

        drawInterpolated(angularResolution, distSun, distSun, thetaStart, principalAngle, q, color, vexBuf);
        drawInterpolated(angularResolution, distSun, distSun, principalAngle, thetaEnd, q, color, vexBuf);
        drawInterpolated(lineResolution, DIST_SUN_BEGIN, distSun + 0.05, thetaStart, thetaStart, q, color, vexBuf);
        drawInterpolated(lineResolution, DIST_SUN_BEGIN, distSun + 0.05, principalAngle, principalAngle, q, color, vexBuf);
        drawInterpolated(lineResolution, DIST_SUN_BEGIN, distSun + 0.05, thetaEnd, thetaEnd, q, color, vexBuf);

        if (icons) {
            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            for (float[] el : texCoord) {
                double deltatheta = sz / distSun * (el[0] * 2 - 1);
                double deltar = sz * (el[1] * 2 - 1);
                double r = distSun - deltar;
                double theta = principalAngle - deltatheta;

                texBuf.putCoord(q.rotateInverseVector(PolarBasis.vec3(r, theta)), el);
            }
        }
    }

    private void drawPolygon(MapContext ctx, JHVRelatedEvents evtr, JHVEvent evt) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        float[] points = pi.getBoundBox();
        if (points.length == 0) {
            return;
        }

        BufVertex vexBuf = evtr.isHighlighted() ? bufThick : bufEvent;
        byte[] color = Colors.bytes(evtr.getColor());

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
                    Vec3 pt = new Vec3(xnew / r, ynew / r, znew / r);
                    previous = Display.mode.emitMapVertex(ctx, pt, previous, j == 0, j == DIVPOINTS, POLYGON_RADIUS, color, vexBuf);
                }
            }
            oldBoundaryPoint3d = new float[]{points[3 * i], points[3 * i + 1], points[3 * i + 2]};
        }
    }

    private void drawImage3d(double x, double y, double z, double width, double height) {
        Vec3 targetDir = new Vec3(x, y, z);
        Quat q = Quat.rotate(Quat.createAxisY(Math.atan2(x, z)), Quat.createAxisX(-Math.asin(y / targetDir.length())));

        double width2 = width / 2.;
        double height2 = height / 2.;
        Vec3 r0 = q.rotateVector(new Vec3(-width2, -height2, 0));
        Vec3 r1 = q.rotateVector(new Vec3(width2, -height2, 0));
        Vec3 r2 = q.rotateVector(new Vec3(-width2, height2, 0));
        Vec3 r3 = q.rotateVector(new Vec3(width2, height2, 0));
        Vec3 p0 = new Vec3(r0.x + targetDir.x, r0.y + targetDir.y, r0.z + targetDir.z);
        Vec3 p1 = new Vec3(r1.x + targetDir.x, r1.y + targetDir.y, r1.z + targetDir.z);
        Vec3 p2 = new Vec3(r2.x + targetDir.x, r2.y + targetDir.y, r2.z + targetDir.z);
        Vec3 p3 = new Vec3(r3.x + targetDir.x, r3.y + targetDir.y, r3.z + targetDir.z);

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

    private void drawIconScale(MapContext ctx, JHVRelatedEvents evtr, JHVEvent evt) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        Vec3 pt = pi.centralPoint();
        if (pt != null) {
            Vec2 tf = Display.mode.projectToScreen(ctx, pt);
            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            drawImageScale(tf.x, tf.y, sz, sz);
        }
    }

    private void drawCactusArcScale(Viewport vp, JHVRelatedEvents evtr, JHVEvent evt, long timestamp, GridScale scale) {
        CactusArcParams params = cactusArcParams(evt, timestamp);
        double angularWidthDegree = params.angularWidthDegree();
        double principalAngleDegree = params.principalAngleDegree();
        double distSun = params.distSun();

        double thetaStart = MathUtils.mapTo0To360(principalAngleDegree - angularWidthDegree / 2.);
        double thetaEnd = MathUtils.mapTo0To360(principalAngleDegree + angularWidthDegree / 2.);

        BufVertex vexBuf = evtr.isHighlighted() ? bufThick : bufEvent;
        byte[] color = Colors.bytes(evtr.getColor());

        float x = (float) (scale.getXValueInv(thetaStart) * vp.aspect);
        float y = (float) scale.getYValueInv(DIST_SUN_BEGIN);
        vexBuf.putVertex(x, y, 0, 1, Colors.Null);
        vexBuf.repeatVertex(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        vexBuf.putVertex(x, y, 0, 1, color);
        vexBuf.repeatVertex(Colors.Null);

        x = (float) (scale.getXValueInv(principalAngleDegree) * vp.aspect);
        y = (float) scale.getYValueInv(DIST_SUN_BEGIN);
        vexBuf.putVertex(x, y, 0, 1, Colors.Null);
        vexBuf.repeatVertex(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        vexBuf.putVertex(x, y, 0, 1, color);
        vexBuf.repeatVertex(Colors.Null);

        x = (float) (scale.getXValueInv(thetaEnd) * vp.aspect);
        y = (float) scale.getYValueInv(DIST_SUN_BEGIN);
        vexBuf.putVertex(x, y, 0, 1, Colors.Null);
        vexBuf.repeatVertex(color);

        y = (float) scale.getYValueInv(distSun + 0.05);
        vexBuf.putVertex(x, y, 0, 1, color);
        vexBuf.repeatVertex(Colors.Null);

        y = (float) scale.getYValueInv(distSun);
        vexBuf.putVertex(x, y, 0, 1, Colors.Null);
        vexBuf.repeatVertex(color);

        x = (float) (scale.getXValueInv(thetaStart) * vp.aspect);
        vexBuf.putVertex(x, y, 0, 1, color);
        vexBuf.repeatVertex(Colors.Null);

        if (icons) {
            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            drawImageScale(scale.getXValueInv(principalAngleDegree) * vp.aspect, scale.getYValueInv(distSun), sz, sz);
        }
    }

    private static final int MOUSE_OFFSET_X = 25;
    private static final int MOUSE_OFFSET_Y = 25;

    private void drawText(Viewport vp, JHVRelatedEvents mouseOverJHVEvent, int x, int y) {
        List<String> txts = new ArrayList<>();
        for (JHVEventParameter p : mouseOverJHVEvent.getClosestTo(controller.currentTime).getSimpleVisibleEventParameters()) {
            String name = p.getParameterName();
            if (name != "event_description" && name != "event_title") { // interned
                txts.add(p.getParameterDisplayName() + " : " + p.getSimpleDisplayParameterValue());
            }
        }
        GLText.drawTextFloat(vp, txts, x + MOUSE_OFFSET_X, y + MOUSE_OFFSET_Y);
    }

    private void renderEvents(Viewport vp) {
        lineEvent.setVertex(bufEvent);
        lineThick.setVertex(bufThick);
        lineEvent.renderLine(vp.aspect, LINEWIDTH);
        lineThick.renderLine(vp.aspect, LINEWIDTH_HIGHLIGHT);
    }

    private void renderIcons(List<JHVRelatedEvents> evs) {
        glslTexture.setCoord(texBuf);
        int idx = 0;
        for (JHVRelatedEvents evtr : evs) {
            JHVEvent evt = evtr.getClosestTo(controller.currentTime);
            if (Display.mode.isLatitudinal() && evt.isCactus())
                continue;
            bindTexture(evtr.getSupplier().getGroup());
            glslTexture.renderTexture(GL.TRIANGLE_STRIP, Colors.floats(evtr.getColor(), ICON_ALPHA), idx, 4);
            idx += 4;
        }
    }

    @Override
    public void render(Camera camera, Viewport vp) {
        if (!isVisible[vp.idx])
            return;
        List<JHVRelatedEvents> evs = SWEKData.getActiveEvents(controller.currentTime);
        if (evs.isEmpty())
            return;
        MapContext ctx = new MapContext(camera.getViewpoint(), vp, Display.gridType);

        for (JHVRelatedEvents evtr : evs) {
            JHVEvent evt = evtr.getClosestTo(controller.currentTime);
            if (evt.isCactus()) {
                drawCactusArc(evtr, evt, controller.currentTime);
            } else {
                drawPolygon(ctx, evtr, evt);
                if (icons) {
                    drawIcon(evtr, evt);
                }
            }
        }
        renderEvents(vp);
        if (icons) {
            renderIcons(evs);
        }
    }

    @Override
    public void renderScale(Camera camera, Viewport vp) {
        if (!isVisible[vp.idx])
            return;
        List<JHVRelatedEvents> evs = SWEKData.getActiveEvents(controller.currentTime);
        if (evs.isEmpty())
            return;
        MapContext ctx = new MapContext(camera.getViewpoint(), vp, Display.gridType);

        for (JHVRelatedEvents evtr : evs) {
            JHVEvent evt = evtr.getClosestTo(controller.currentTime);
            if (evt.isCactus() && (Display.mode.isPolar() || Display.mode.isLogPolar())) {
                drawCactusArcScale(vp, evtr, evt, controller.currentTime, Display.mode.scale);
            } else {
                drawPolygon(ctx, evtr, evt);
                if (icons) {
                    drawIconScale(ctx, evtr, evt);
                }
            }
        }
        renderEvents(vp);
        if (icons) {
            renderIcons(evs);
        }
    }

    @Override
    public void renderFullFloat(Camera camera, Viewport vp) {
        if (!enabled)
            return;
        if (SWEKPopupController.mouseOverJHVEvent != null) {
            drawText(vp, SWEKPopupController.mouseOverJHVEvent, SWEKPopupController.mouseOverX, SWEKPopupController.mouseOverY);
        }
    }

    @Override
    public void remove() {
        setEnabled(false);
        dispose();
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
            JHVEventCache.registerHandler(this);
            Movie.addTimeRangeListener(this);
            Movie.addTimeListener(controller);
            JHVFrame.getInputController().addPlugin(controller);
            requestEvents(true, Movie.getStartTime(), Movie.getEndTime());
        } else {
            controller.resetHover();
            JHVFrame.getInputController().removePlugin(controller);
            Movie.removeTimeListener(controller);
            Movie.removeTimeRangeListener(this);
            JHVEventCache.unregisterHandler(this);
        }
    }

    @Override
    public void init() {
        lineEvent.init();
        lineThick.init();
        glslTexture.init();
    }

    @Override
    public void dispose() {
        lineEvent.dispose();
        lineThick.dispose();
        glslTexture.dispose();
        iconCacheId.values().forEach(GLTexture::delete);
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
    public void timeRangeChanged(long start, long end) {
        requestEvents(false, start, end);
    }

    @Override
    public void newEventsReceived() {
        if (enabled)
            MovieDisplay.display();
    }

    @Override
    public void cacheUpdated() {
        if (!enabled)
            return;
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

        return panel;
    }

}
