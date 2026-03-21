package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class OrthoProjection {

    private OrthoProjection() {
    }

    static Vec2 emitMapVertex(Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last, double radius) {
        if (first) {
            vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), 1, Colors.Null);
        }
        vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), 1, color);
        if (last) {
            vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), 1, Colors.Null);
        }
        return previous;
    }

    static void emitMapPoint(Vec3 vertex, BufVertex vexBuf, byte[] color, double size, double radius) {
        vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), (float) size, color);
    }

    static Vec3 unprojectSurfacePoint(Camera camera, Viewport vp, int x, int y) {
        return CameraHelper.unprojectToOutputSphere(camera, vp, x, y, camera.getViewpoint().toQuat());
    }

    static Vec2 mouseToGrid(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        Quat rotation = Quat.ZERO;
        if (gridType != GridType.Viewpoint) {
            Position viewpoint = camera.getViewpoint();
            rotation = Quat.rotateWithConjugate(viewpoint.toQuat(), gridType.toCarrington(viewpoint));
        }

        Vec3 p = CameraHelper.unprojectToOutputSphere(camera, vp, x, y, rotation);
        if (p == null)
            return Vec2.NAN;

        double theta = Math.toDegrees(SphericalCoords.latitude(p));
        double phi = Math.toDegrees(SphericalCoords.longitude(p));
        if (gridType == GridType.Carrington && phi < 0)
            phi += 360;
        return new Vec2(phi, theta);
    }
}
