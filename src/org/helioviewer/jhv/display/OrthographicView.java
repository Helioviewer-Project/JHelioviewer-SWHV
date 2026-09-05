package org.helioviewer.jhv.display;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class OrthographicView extends MapView {

    OrthographicView(Camera _camera, Position _viewpoint, GridType _gridType, MapScale[] _scales) {
        super(_camera, _viewpoint, MapMode.Orthographic, _gridType, _scales);
    }

    @Override
    public Vec2 projectToScreen(Viewport vp, Vec3 v) {
        throw new UnsupportedOperationException("Orthographic mode does not support projectToScreen()");
    }

    @Override
    public Vec2 mouseToMap(Viewport vp, int x, int y) {
        Quat rotation = gridType == GridType.Viewpoint
                ? Quat.ZERO
                : Quat.rotateWithConjugate(viewpoint.toQuat(), gridType.toCarrington(viewpoint));

        Vec3 p = ViewportMath.unprojectToOutputSphere(camera, vp, cameraWidth(vp), x, y, rotation);
        if (p == null)
            return Vec2.NAN;

        double theta = Math.toDegrees(SphericalCoords.latitude(p));
        double phi = Math.toDegrees(SphericalCoords.longitude(p));
        return new Vec2(phi, theta);
    }

    @Override
    public Vec3 mouseToSurface(Viewport vp, int x, int y) {
        return ViewportMath.unprojectToOutputSphere(camera, vp, cameraWidth(vp), x, y, viewpoint.toQuat());
    }

    @Override
    public Vec2 mouseToScreen(Viewport vp, int x, int y) {
        throw new UnsupportedOperationException("Orthographic mode does not support mouseToScreen()");
    }

    @Override
    public void emitMapLine(Viewport vp, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf) {
        if (vertices.isEmpty())
            return;

        Vec3 first = vertices.getFirst();
        vexBuf.startLine((float) (first.x * radius), (float) (first.y * radius), (float) (first.z * radius), 1, color);
        for (int i = 1; i < vertices.size(); i++) {
            Vec3 vertex = vertices.get(i);
            float x = (float) (vertex.x * radius);
            float y = (float) (vertex.y * radius);
            float z = (float) (vertex.z * radius);
            vexBuf.putVertex(x, y, z, 1, color);
        }
        vexBuf.endLine();
    }

    @Override
    public void emitMapPoints(Viewport vp, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf) {
        float pointSize = (float) size;
        for (Vec3 vertex : vertices)
            vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), pointSize, color);
    }
}
