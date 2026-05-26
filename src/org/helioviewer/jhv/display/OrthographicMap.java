package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

final class OrthographicMap {

    private OrthographicMap() {}

    static Vec3 mouseToSurface(Camera camera, RenderView renderView, Viewport vp, int x, int y) {
        return ViewportMath.unprojectToOutputSphere(camera, vp, renderView.cameraWidth(vp.zoom), x, y, renderView.viewpoint().toQuat());
    }

    static void emitMapVertex(Vec3 vertex, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf) {
        if (first)
            vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), 1, Colors.Null);
        vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), 1, color);
        if (last)
            vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), 1, Colors.Null);
    }

    static void emitMapPoint(Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
        vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), (float) size, color);
    }

    static Vec2 mouseToGrid(Camera camera, RenderView renderView, Viewport vp, GridType gridType, int x, int y) {
        Position viewpoint = renderView.viewpoint();
        Quat rotation = gridType == GridType.Viewpoint
                ? Quat.ZERO
                : Quat.rotateWithConjugate(viewpoint.toQuat(), gridType.toCarrington(viewpoint));

        Vec3 p = ViewportMath.unprojectToOutputSphere(camera, vp, renderView.cameraWidth(vp.zoom), x, y, rotation);
        if (p == null)
            return Vec2.NAN;

        double theta = Math.toDegrees(SphericalCoords.latitude(p));
        double phi = Math.toDegrees(SphericalCoords.longitude(p));
        phi = gridType == GridType.Carrington && phi < 0 ? phi + 360 : phi;
        return new Vec2(phi, theta);
    }
}
