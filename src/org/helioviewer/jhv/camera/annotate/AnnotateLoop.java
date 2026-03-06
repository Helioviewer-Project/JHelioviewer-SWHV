package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
import org.json.JSONObject;

public class AnnotateLoop extends AbstractAnnotateable {

    private static final int SUBDIVISIONS = 45;

    private String heightStr = null;

    public AnnotateLoop(JSONObject jo) {
        super(jo);
    }

    private void drawCircle(Quat q, Viewport vp, Vec3 bp, Vec3 ep, BufVertex buf, byte[] color) {
        double cosf = Vec3.dot(bp, ep);
        double r = Math.sqrt(1 - cosf * cosf);
        // P = center + r cos(A) (bp x ep) + r sin(A) ep

        double h = (cosf + r) * Math.sqrt(bp.x * bp.x + bp.y * bp.y + bp.z * bp.z) - Sun.Radius;
        heightStr = h < 0.2 * Sun.Radius ? String.format("Hann: %7.2fMm", h * (Sun.RadiusMeter / 1e6)) : String.format("Hann: %7.2fR\u2609", h);

        Vec3 center = Vec3.multiply(bp, cosf);
        Vec3 u = Vec3.cross(bp, ep);
        u.normalize();
        Vec3 v = Vec3.cross(bp, u);

        Vec3 vx = new Vec3();
        Vec2 previous = null;

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double t = i * Math.PI / SUBDIVISIONS;
            double cosr = Math.cos(t) * r;
            double sinr = Math.sin(t) * r;
            vx.x = center.x + cosr * v.x + sinr * bp.x;
            vx.y = center.y + cosr * v.y + sinr * bp.y;
            vx.z = center.z + cosr * v.z + sinr * bp.z;
            previous = GLHelper.drawProjectedVertex(q, vp, vx, previous, buf, color, i == 0, i == SUBDIVISIONS, radius);
        }
    }

    @Override
    public void draw(Quat q, Viewport vp, boolean active, BufVertex buf) {
        boolean dragged = beingDragged();
        if ((startPoint == null || endPoint == null) && !dragged)
            return;

        byte[] color = dragged ? dragColor : (active ? activeColor : baseColor);
        Vec3 p0 = dragged ? dragStartPoint : startPoint;
        Vec3 p1 = dragged ? dragEndPoint : endPoint;

        drawCircle(q, vp, p0, p1, buf, color);
    }

    @Override
    protected Vec3 computeDragPoint(Camera camera, int x, int y) {
        return computePointSun(camera, x, y);
    }

    @Nullable
    @Override
    public Object getData() {
        return heightStr;
    }

}
