package org.helioviewer.jhv.plugins.swek;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.event.EventCache;
import org.helioviewer.jhv.event.EventGeometry;
import org.helioviewer.jhv.event.EventListener;
import org.helioviewer.jhv.event.ObservationGroup;
import org.helioviewer.jhv.event.SWEKDownloader;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.image.nio.NativeImageFactory;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.movie.Player;
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
public final class SWEKLayer extends AbstractLayer implements EventListener.Handle, TimeListener.Range {
    private record CactusArcParams(double angularWidthDegree, double principalAngleDegree, double distSun) {}

    private static final int DIVPOINTS = 10;
    private static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private static final double LINEWIDTH_HIGHLIGHT = 2 * LINEWIDTH;
    private static final double POLYGON_RADIUS = Sun.Radius * 1.01;

    private static final HashMap<String, GLTexture> iconCacheId = new HashMap<>();
    private static final double ICON_ALPHA = 0.7;
    private static final double ICON_SIZE = 0.1;
    private static final double ICON_SIZE_HIGHLIGHTED = 0.16;

    private static final float[][] texCoord = {{0, 1}, {1, 1}, {0, 0}, {1, 0}};

    private SWEKContext swekContext;
    private boolean icons = true;

    private final GLSLLine lineEvent = new GLSLLine(true);
    private final BufVertex bufEvent = new BufVertex();
    private final GLSLLine lineThick = new GLSLLine(true);
    private final BufVertex bufThick = new BufVertex();

    private final GLSLTexture glslTexture = new GLSLTexture();
    private final BufCoord texBuf = new BufCoord(4 * 8);

    private long cachedEventsTime = Long.MIN_VALUE;
    private List<ObservationGroup> cachedActiveEvents = List.of();

    public SWEKLayer(JSONObject jo) {
        if (jo != null) {
            icons = jo.optBoolean("icons", icons);
            SWEKPlugin.restoreLayer(this);
        }
    }

    void setContext(SWEKContext _swekContext) {
        swekContext = _swekContext;
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("icons", icons);
    }

    private static void bindTexture(SWEKGroup group) {
        String key = group.getName();
        GLTexture tex = iconCacheId.get(key);
        if (tex == null) {
            ImageIcon icon = SWEKIconBank.getIcon(group.getIconKey());
            BufferedImage bi = NativeImageFactory.createRGBAPremultipliedImage(icon.getIconWidth(), icon.getIconHeight());
            try {
                Graphics g = bi.createGraphics();
                try {
                    icon.paintIcon(null, g, 0, 0);
                } finally {
                    g.dispose();
                }

                tex = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.THREE);

                ByteBuffer data = NativeImageFactory.getByteBuffer(bi);
                tex.upload2D(GLTexture.Format.RGBA8, bi.getWidth(), bi.getHeight(), GL.LINEAR, data);
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

            if (i == 0)
                vexBuf.startLine(res, color);
            else
                vexBuf.putVertex(res, color);
        }
        vexBuf.endLine();
    }

    private static CactusArcParams cactusArcParams(SolarEvent evt, long timestamp) {
        double angularWidthDegree = evt.getCMEParameters().angularWidthDegree();
        double principalAngleDegree = evt.getCMEParameters().principalAngleDegree();
        double distSun = SWEKData.cactusDistance(evt, timestamp);
        return new CactusArcParams(angularWidthDegree, principalAngleDegree, distSun);
    }

    private void drawCactusArc(ObservationGroup evtr, SolarEvent evt, long timestamp) {
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
        drawInterpolated(lineResolution, SWEKData.CACTUS_START_RADIUS, distSun + 0.05, thetaStart, thetaStart, q, color, vexBuf);
        drawInterpolated(lineResolution, SWEKData.CACTUS_START_RADIUS, distSun + 0.05, principalAngle, principalAngle, q, color, vexBuf);
        drawInterpolated(lineResolution, SWEKData.CACTUS_START_RADIUS, distSun + 0.05, thetaEnd, thetaEnd, q, color, vexBuf);

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

    private void drawPolygon(MapView mv, Viewport vp, ObservationGroup evtr, SolarEvent evt) {
        EventGeometry pi = evt.getPositionInformation();
        if (pi == null)
            return;

        float[] points = pi.getBoundBox();
        if (points.length == 0) {
            return;
        }

        BufVertex vexBuf = evtr.isHighlighted() ? bufThick : bufEvent;
        byte[] color = Colors.bytes(evtr.getColor());

        // draw bounds
        int plen = points.length / 3;
        for (int i = 1; i < plen; i++) {
            int previous = 3 * (i - 1), current = 3 * i;
            for (int j = 0; j <= DIVPOINTS; j++) {
                double alpha = 1. - j / (double) DIVPOINTS;
                double xnew = alpha * points[previous] + (1 - alpha) * points[current];
                double ynew = alpha * points[previous + 1] + (1 - alpha) * points[current + 1];
                double znew = alpha * points[previous + 2] + (1 - alpha) * points[current + 2];
                double r = Math.sqrt(xnew * xnew + ynew * ynew + znew * znew);
                vertices.set(j, new Vec3(xnew / r, ynew / r, znew / r));
            }
            mv.emitMapLine(vp, vertices, POLYGON_RADIUS, color, vexBuf);
        }
    }

    private final List<Vec3> vertices = fixedSizeVertices(DIVPOINTS + 1);

    private static List<Vec3> fixedSizeVertices(int size) {
        List<Vec3> vertices = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            vertices.add(Vec3.ZERO);
        return vertices;
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

    private void drawIcon(ObservationGroup evtr, SolarEvent evt) {
        EventGeometry pi = evt.getPositionInformation();
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

    private void drawIconScale(MapView mv, Viewport vp, ObservationGroup evtr, SolarEvent evt) {
        EventGeometry pi = evt.getPositionInformation();
        if (pi == null)
            return;

        Vec3 pt = pi.centralPoint();
        if (pt != null) {
            Vec2 tf = mv.projectToScreen(vp, pt);
            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            drawImageScale(tf.x, tf.y, sz, sz);
        }
    }

    private static void putLineScale(BufVertex vexBuf, float x0, float y0, float x1, float y1, byte[] color) {
        vexBuf.startLine(x0, y0, 0, 1, color);
        vexBuf.putVertex(x1, y1, 0, 1, color);
        vexBuf.endLine();
    }

    private void drawCactusArcScale(Viewport vp, ObservationGroup evtr, SolarEvent evt, long timestamp, MapScale scale) {
        CactusArcParams params = cactusArcParams(evt, timestamp);
        double angularWidthDegree = params.angularWidthDegree();
        double principalAngleDegree = params.principalAngleDegree();
        double distSun = params.distSun();

        double thetaStart = MathUtils.mapTo0To360(principalAngleDegree - angularWidthDegree / 2.);
        double thetaEnd = MathUtils.mapTo0To360(principalAngleDegree + angularWidthDegree / 2.);

        BufVertex vexBuf = evtr.isHighlighted() ? bufThick : bufEvent;
        byte[] color = Colors.bytes(evtr.getColor());

        float x = (float) ((scale.toUnitX(thetaStart) - 0.5) * vp.aspect);
        float y = (float) (scale.toUnitY(SWEKData.CACTUS_START_RADIUS) - 0.5);
        putLineScale(vexBuf, x, y, x, (float) (scale.toUnitY(distSun + 0.05) - 0.5), color);

        x = (float) ((scale.toUnitX(principalAngleDegree) - 0.5) * vp.aspect);
        y = (float) (scale.toUnitY(SWEKData.CACTUS_START_RADIUS) - 0.5);
        putLineScale(vexBuf, x, y, x, (float) (scale.toUnitY(distSun + 0.05) - 0.5), color);

        x = (float) ((scale.toUnitX(thetaEnd) - 0.5) * vp.aspect);
        y = (float) (scale.toUnitY(SWEKData.CACTUS_START_RADIUS) - 0.5);
        putLineScale(vexBuf, x, y, x, (float) (scale.toUnitY(distSun + 0.05) - 0.5), color);

        y = (float) (scale.toUnitY(distSun) - 0.5);
        putLineScale(vexBuf, x, y, (float) ((scale.toUnitX(thetaStart) - 0.5) * vp.aspect), y, color);

        if (icons) {
            double sz = evtr.isHighlighted() ? ICON_SIZE_HIGHLIGHTED : ICON_SIZE;
            drawImageScale((scale.toUnitX(principalAngleDegree) - 0.5) * vp.aspect,
                    scale.toUnitY(distSun) - 0.5, sz, sz);
        }
    }

    private static final int MOUSE_OFFSET_X = 25;
    private static final int MOUSE_OFFSET_Y = 25;

    private void drawText(Viewport vp, ObservationGroup mouseOverGroup, int x, int y, long currentTime) {
        GLText.drawTextFloat(vp, SWEKData.visibleParameterLines(mouseOverGroup.getClosestTo(currentTime)), x + MOUSE_OFFSET_X, y + MOUSE_OFFSET_Y);
    }

    private void renderEvents(Viewport vp) {
        lineEvent.uploadAndClear(bufEvent);
        lineThick.uploadAndClear(bufThick);
        lineEvent.renderLine(vp, LINEWIDTH);
        lineThick.renderLine(vp, LINEWIDTH_HIGHLIGHT);
    }

    private void renderIcons(MapView mv, List<ObservationGroup> evs, long currentTime) {
        glslTexture.setCoord(texBuf);
        int idx = 0;
        for (ObservationGroup evtr : evs) {
            SolarEvent evt = evtr.getClosestTo(currentTime);
            if (mv.isLatitudinal() && evt.isCactus())
                continue;
            if (!evt.isCactus()) {
                EventGeometry pi = evt.getPositionInformation();
                if (pi == null || pi.centralPoint() == null)
                    continue;
            }
            bindTexture(evt.getSupplier().group());
            glslTexture.renderTexture(GL.TRIANGLE_STRIP, Colors.floats(evtr.getColor(), ICON_ALPHA), idx, 4);
            idx += 4;
        }
    }

    List<ObservationGroup> activeEvents(long time) {
        if (time != cachedEventsTime) {
            cachedEventsTime = time;
            cachedActiveEvents = EventCache.getEvents(time, time);
        }
        return cachedActiveEvents;
    }

    private void invalidateActiveEvents() {
        cachedEventsTime = Long.MIN_VALUE;
    }

    @Override
    public void render(MapView mv, Viewport vp) {
        if (!isVisible[vp.idx])
            return;
        long currentTime = mv.viewpoint().time.milli;
        List<ObservationGroup> evs = activeEvents(currentTime);
        if (evs.isEmpty())
            return;

        for (ObservationGroup evtr : evs) {
            SolarEvent evt = evtr.getClosestTo(currentTime);
            if (evt.isCactus()) {
                drawCactusArc(evtr, evt, currentTime);
            } else {
                drawPolygon(mv, vp, evtr, evt);
                if (icons) {
                    drawIcon(evtr, evt);
                }
            }
        }
        renderEvents(vp);
        if (icons) {
            renderIcons(mv, evs, currentTime);
        }
    }

    @Override
    public void renderScale(MapView mv, Viewport vp) {
        if (!isVisible[vp.idx])
            return;
        long currentTime = mv.viewpoint().time.milli;
        List<ObservationGroup> evs = activeEvents(currentTime);
        if (evs.isEmpty())
            return;

        MapScale scale = mv.scale(vp);
        for (ObservationGroup evtr : evs) {
            SolarEvent evt = evtr.getClosestTo(currentTime);
            if (evt.isCactus() && mv.isRectWarp()) {
                drawCactusArcScale(vp, evtr, evt, currentTime, scale);
            } else {
                drawPolygon(mv, vp, evtr, evt);
                if (icons) {
                    drawIconScale(mv, vp, evtr, evt);
                }
            }
        }
        renderEvents(vp);
        if (icons) {
            renderIcons(mv, evs, currentTime);
        }
    }

    @Override
    public void renderFullFloat(Viewport vp) {
        if (!enabled)
            return;
        if (swekContext != null && swekContext.mouseOverGroup() != null) {
            drawText(vp, swekContext.mouseOverGroup(), swekContext.mouseOverX(), swekContext.mouseOverY(), swekContext.mouseOverTime());
        }
    }

    @Override
    public void remove() {
        setEnabled(false);
        dispose();
    }

    @Override
    public String getName() {
        return "SWEK Events";
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);

        if (enabled) {
            EventCache.registerHandler(this);
            Player.addTimeRangeListener(this);
            requestEvents(true, Player.getStartTime(), Player.getEndTime());
        } else {
            invalidateActiveEvents();
            EventCache.highlight(null);
            Player.removeTimeRangeListener(this);
            EventCache.unregisterHandler(this);
        }
        SWEKPlugin.layerStateChanged(this);
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

    private long startTime = Player.getStartTime();
    private long endTime = Player.getEndTime();

    private void requestEvents(boolean force, long start, long end) {
        if (force || start < startTime || end > endTime) {
            startTime = start;
            endTime = end;
            SWEKDownloader.requestForInterval(start, end);
        }
    }

    @Override
    public void timeRangeChanged(long start, long end) {
        invalidateActiveEvents();
        requestEvents(false, start, end);
    }

    @Override
    public void cacheUpdated() {
        if (!enabled)
            return;
        invalidateActiveEvents();
        requestEvents(true, Player.getStartTime(), Player.getEndTime());
        DisplayController.display();
    }

    boolean isIcons() {
        return icons;
    }

    void setIcons(boolean _icons) {
        icons = _icons;
        DisplayController.display();
    }

}
