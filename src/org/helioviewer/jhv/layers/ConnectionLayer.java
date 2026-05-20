package org.helioviewer.jhv.layers;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.ProjectionScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.connect.LoadConnectivity;
import org.helioviewer.jhv.layers.connect.LoadConnectivity.Connectivity;
import org.helioviewer.jhv.layers.connect.LoadFootpoint;
import org.helioviewer.jhv.layers.connect.LoadHCS;
import org.helioviewer.jhv.layers.connect.LoadSunJSON;
import org.helioviewer.jhv.layers.connect.SunJSONTypes;
import org.helioviewer.jhv.math.SphericalPoint;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;

import org.json.JSONObject;

public final class ConnectionLayer extends AbstractLayer implements LoadConnectivity.Receiver, LoadFootpoint.Receiver, LoadHCS.Receiver, LoadSunJSON.Receiver {

    private static final double LINEWIDTH = 2 * GLSLLine.LINEWIDTH_BASIC;
    private static final double ORTHO_RADIUS = 1.01;
    private static final float SIZE_POINT = 0.02f;

    private final byte[] sswColor = Colors.bytes(164, 48, 42);
    private final byte[] fswColor = Colors.bytes(74, 136, 92);
    private final byte[] mColor = Colors.bytes(240, 145, 53);
    private final GLSLShape connectivityCenter = new GLSLShape(true);
    private final BufVertex connectivityBuf = new BufVertex(96 * GLSLShape.stride);

    private final byte[] hcsColor = Colors.bytes(223, 62, 48);
    private final GLSLLine hcsLine = new GLSLLine(true); // TBD
    private final BufVertex hcsBuf = new BufVertex(512 * GLSLLine.stride);

    private final byte[] footpointColor = Colors.White;
    private final GLSLLine footpointLine = new GLSLLine(true);
    private final BufVertex footpointBuf = new BufVertex(12 * GLSLLine.stride);

    private final GLSLLine geometryLine = new GLSLLine(true);
    private final GLSLShape geometryPoint = new GLSLShape(true);

    private Connectivity connectivity;
    private List<Vec3> hcs;
    private TimeMap<Position.Cartesian> footpointMap;
    private final TimeMap<SunJSONTypes.GeometryCollection> geometryMap = new TimeMap<>();

    private JHVTime lastTimestamp;

    private void updateTimestamp(JHVTime timestamp) {
        if (!timestamp.equals(lastTimestamp)) {
            lastTimestamp = timestamp;
            Layers.fireTimeUpdated(this);
        }
    }

    @Override
    public void serialize(JSONObject jo) {}

    public ConnectionLayer(JSONObject ignoredJo) {}

    @Override
    public void render(MapContext ctx, Viewport vp, ProjectionScale scale) {
        if (!isVisible[vp.idx])
            return;
        Camera camera = ctx.camera();
        if (connectivity != null)
            drawConnectivity(ctx, vp, scale, camera);
        if (hcs != null)
            drawHCS(ctx, vp, scale);
        if (footpointMap != null)
            drawFootpointInterpolated(ctx, vp, scale);

        if (!geometryMap.isEmpty()) {
            SunJSONTypes.GeometryCollection g = geometryMap.nearestValue(ctx.viewpoint().time);
            updateTimestamp(g.time());
            g.render(geometryLine, geometryPoint, vp, CameraHelper.getPixelFactor(camera, vp));
        }
    }

    @Override
    public void renderScale(MapContext ctx, Viewport vp, ProjectionScale scale) {
        render(ctx, vp, scale);
    }

    private void drawConnectivity(MapContext ctx, Viewport vp, ProjectionScale scale, Camera camera) {
        putConnectivity(ctx, vp, scale, connectivity.SSW, sswColor, connectivityBuf);
        putConnectivity(ctx, vp, scale, connectivity.FSW, fswColor, connectivityBuf);
        putConnectivity(ctx, vp, scale, connectivity.M, mColor, connectivityBuf);

        connectivityCenter.setVertex(connectivityBuf);
        connectivityCenter.renderPoints(CameraHelper.getPixelFactor(camera, vp));
    }

    private static void putConnectivity(MapContext ctx, Viewport vp, ProjectionScale scale, List<Vec3> points, byte[] color, BufVertex vexBuf) {
        points.forEach(v -> ctx.emitMapPoint(vp, scale, v, SIZE_POINT, ORTHO_RADIUS, color, vexBuf));
    }

    private void drawHCS(MapContext ctx, Viewport vp, ProjectionScale scale) {
        if (hcs.isEmpty())
            return;
        Vec3 first = hcs.getFirst();
        Vec2 previous = ctx.emitMapVertex(vp, scale, first, null, true, false, ORTHO_RADIUS, hcsColor, hcsBuf);
        for (int i = 1; i < hcs.size(); i++) {
            previous = ctx.emitMapVertex(vp, scale, hcs.get(i), previous, false, false, ORTHO_RADIUS, hcsColor, hcsBuf);
        }
        ctx.emitMapVertex(vp, scale, first, previous, false, true, ORTHO_RADIUS, hcsColor, hcsBuf);

        hcsLine.setVertex(hcsBuf);
        hcsLine.renderLine(vp, LINEWIDTH);
    }

    private static SphericalPoint interpolateToSpherical(long t, Position.Cartesian prev, Position.Cartesian next) {
        long tprev = prev.milli();
        long tnext = next.milli();
        double alpha = tnext == tprev ? 1. : Math.clamp((t - tprev) / (double) (tnext - tprev), 0., 1.);
        double x = (1. - alpha) * prev.x() + alpha * next.x();
        double y = (1. - alpha) * prev.y() + alpha * next.y();
        double z = (1. - alpha) * prev.z() + alpha * next.z();

        return SphericalPoint.fromCartesian(x, y, z);
    }

    private void drawFootpointInterpolated(MapContext ctx, Viewport vp, ProjectionScale scale) {
        JHVTime time = ctx.viewpoint().time;
        updateTimestamp(time);

        SphericalPoint point = interpolateToSpherical(time.milli, footpointMap.lowerValue(time), footpointMap.higherValue(time));
        AnnotateCross.drawCross(ctx, vp, scale, point.longitude(), point.latitude(), footpointColor, footpointBuf);
        footpointLine.setVertex(footpointBuf);
        footpointLine.renderLine(vp, LINEWIDTH);
    }

    @Override
    public void init() {
        connectivityCenter.init();
        hcsLine.init();
        footpointLine.init();

        geometryLine.init();
        geometryPoint.init();
    }

    @Override
    public void dispose() {
        connectivityCenter.dispose();
        hcsLine.dispose();
        footpointLine.dispose();

        geometryLine.dispose();
        geometryPoint.dispose();
    }

    @Override
    public void remove() {
        dispose();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);
        if (!enabled) {
            lastTimestamp = null;
        }
    }

    @Override
    public String getName() {
        return "Connection";
    }

    @Nullable
    @Override
    public String getTimeString() {
        return lastTimestamp == null ? null : lastTimestamp.toString();
    }

    @Override
    public void setConnectivity(Connectivity _connectivity) {
        connectivity = _connectivity;
        MovieDisplay.display();
    }

    @Override
    public void setPositionMap(TimeMap<Position.Cartesian> _footpointMap) {
        footpointMap = _footpointMap;
        MovieDisplay.display();
    }

    @Override
    public void setHCS(List<Vec3> _hcs) {
        hcs = _hcs;
        MovieDisplay.display();
    }

    @Override
    public void setGeometry(List<SunJSONTypes.GeometryCollection> geometry) {
        if (geometry.isEmpty()) return;

        geometry.forEach(g -> geometryMap.put(g.time(), g));
        geometryMap.buildIndex();
        MovieDisplay.display();
    }

    public void clear() {
        connectivity = null;
        hcs = null;
        footpointMap = null;
        geometryMap.clear();
        lastTimestamp = null;
        MovieDisplay.display();
    }

}
