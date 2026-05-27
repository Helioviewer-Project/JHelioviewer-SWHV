package org.helioviewer.jhv.display;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class OrthographicMap {

    private OrthographicMap() {}

    static Vec3 mouseToSurface(Camera camera, Position viewpoint, double width, Viewport vp, int x, int y) {
        return ViewportMath.unprojectToOutputSphere(camera, vp, width, x, y, viewpoint.toQuat());
    }

    static void emitMapLine(List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf) {
        if (vertices.isEmpty())
            return;

        Vec3 first = vertices.getFirst();
        vexBuf.putVertex((float) (first.x * radius), (float) (first.y * radius), (float) (first.z * radius), 1, Colors.Null);
        vexBuf.repeatVertex(color);
        for (int i = 1; i < vertices.size(); i++) {
            Vec3 vertex = vertices.get(i);
            float x = (float) (vertex.x * radius);
            float y = (float) (vertex.y * radius);
            float z = (float) (vertex.z * radius);
            vexBuf.putVertex(x, y, z, 1, color);
        }
        vexBuf.repeatVertex(Colors.Null);
    }

    static void emitMapPoints(List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf) {
        float pointSize = (float) size;
        for (Vec3 vertex : vertices)
            vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), pointSize, color);
    }

    static Vec2 mouseToGrid(Camera camera, Position viewpoint, double width, Viewport vp, GridType gridType, int x, int y) {
        Quat rotation = gridType == GridType.Viewpoint
                ? Quat.ZERO
                : Quat.rotateWithConjugate(viewpoint.toQuat(), gridType.toCarrington(viewpoint));

        Vec3 p = ViewportMath.unprojectToOutputSphere(camera, vp, width, x, y, rotation);
        if (p == null)
            return Vec2.NAN;

        double theta = Math.toDegrees(SphericalCoords.latitude(p));
        double phi = Math.toDegrees(SphericalCoords.longitude(p));
        phi = gridType == GridType.Carrington && phi < 0 ? phi + 360 : phi;
        return new Vec2(phi, theta);
    }
}
