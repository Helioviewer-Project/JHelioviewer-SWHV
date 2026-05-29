package org.helioviewer.jhv.annotations;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

import org.json.JSONObject;

final class AnnotateLoop extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 45;

    private final List<Vec3> vertices = fixedSizeVertices(SUBDIVISIONS + 1);
    private double cachedHeight = Double.NaN;
    private String heightStr = null;

    AnnotateLoop(JSONObject jo) {
        super(jo);
    }

    // Draw the loop as a semicircle whose feet are exactly bp and ep.
    private void drawLoop(MapView mv, Viewport vp, MapScale scale, Vec3 bp, Vec3 ep, byte[] color, BufVertex vexBuf) {
        Vec3 center = new Vec3(0.5 * (bp.x + ep.x), 0.5 * (bp.y + ep.y), 0.5 * (bp.z + ep.z));
        Vec3 u = new Vec3(0.5 * (bp.x - ep.x), 0.5 * (bp.y - ep.y), 0.5 * (bp.z - ep.z));
        double centerLen = center.length();
        if (centerLen < 1e-12) // reject antipodal drawing
            return;

        double radiusLen = u.length();
        double height = centerLen + radiusLen - Sun.Radius;
        if (height != cachedHeight) {
            cachedHeight = height;
            heightStr = height < 0.2 * Sun.Radius ? String.format("Hann: %7.2fMm", height * (Sun.RadiusMeter / 1e6)) : String.format("Hann: %7.2fR\u2609", height);
        }

        double centerScale = radiusLen / centerLen;
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double t = i * Math.PI / SUBDIVISIONS;
            double cosr = Math.cos(t);
            double sinr = Math.sin(t) * centerScale;
            vertices.set(i, new Vec3(
                    center.x + cosr * u.x + sinr * center.x,
                    center.y + cosr * u.y + sinr * center.y,
                    center.z + cosr * u.z + sinr * center.z));
        }
        mv.emitMapLine(vp, scale, vertices, ANNOTATION_RADIUS, color, vexBuf);
    }

    @Override
    public void draw(MapView mv, Viewport vp, MapScale scale, boolean active, BufVertex vexBuf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = dragged ? dragColor : (active ? activeColor : baseColor);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;

        drawLoop(mv, vp, scale, p0, p1, color, vexBuf);
    }

    @Override
    protected Vec3 computeDragPoint(Viewport vp, int x, int y) {
        return mouseToSurface(vp, x, y);
    }

    @Nullable
    @Override
    public Object getData() {
        return heightStr;
    }

}
