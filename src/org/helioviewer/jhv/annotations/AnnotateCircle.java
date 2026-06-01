package org.helioviewer.jhv.annotations;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.FastFormat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

import org.json.JSONObject;

final class AnnotateCircle extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 90;

    private final List<Vec3> vertices = fixedSizeVertices(SUBDIVISIONS + 1);
    private double diameter = Double.NaN;
    private String diameterStr = null;

    AnnotateCircle(JSONObject jo) {
        super(jo);
    }

    private void drawCircle(MapView mv, Viewport vp, MapScale scale, Vec3 bp, Vec3 ep, byte[] color, BufVertex vexBuf) {
        double cosf = Vec3.dot(bp, ep);
        double r = Math.sqrt(1 - cosf * cosf);
        // P = center + r cos(A) (bp x ep) + r sin(A) ep

        double d = 2 * r;
        if (d != diameter) {
            diameter = d;
            if (d < 0.2 * Sun.Radius)
                diameterStr = "Dann: " + FastFormat.fixed2(d * (Sun.RadiusMeter / 1e6), 7, false) + "Mm";
            else
                diameterStr = "Dann: " + FastFormat.fixed2(d, 7, false) + "R☉";
        }

        Vec3 center = new Vec3(bp.x * cosf, bp.y * cosf, bp.z * cosf);
        Vec3 u = Vec3.cross(bp, ep);
        Vec3 v = Vec3.cross(bp, u);

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double t = i * 2. * Math.PI / SUBDIVISIONS;
            double cosr = Math.cos(t);
            double sinr = Math.sin(t);
            vertices.set(i, new Vec3(
                    center.x + cosr * u.x + sinr * v.x,
                    center.y + cosr * u.y + sinr * v.y,
                    center.z + cosr * u.z + sinr * v.z));
        }
        mv.emitMapLine(vp, scale, vertices, ANNOTATION_RADIUS, color, vexBuf);
    }

    @Override
    public void draw(MapView mv, Viewport vp, MapScale scale, boolean active, BufVertex vexBuf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = color(dragged, active);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;

        drawCircle(mv, vp, scale, p0, p1, color, vexBuf);
    }

    @Override
    protected Vec3 computeDragPoint(Viewport vp, int x, int y) {
        return mouseToSurface(vp, x, y);
    }

    @Nullable
    @Override
    public Object getData() {
        return diameterStr;
    }

}
