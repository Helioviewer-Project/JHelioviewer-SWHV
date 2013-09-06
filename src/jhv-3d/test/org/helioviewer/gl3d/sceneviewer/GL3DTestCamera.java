package org.helioviewer.gl3d.sceneviewer;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DInteraction;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.wcs.Cartesian3DCoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateSystem;

public class GL3DTestCamera extends GL3DCamera {
    GL3DInteraction currentInteraction;
    CoordinateSystem viewSpaceCoordinateSystem;

    public GL3DTestCamera() {
        super(0.01, 100);
        this.viewSpaceCoordinateSystem = new Cartesian3DCoordinateSystem();
        this.currentInteraction = new GL3DTestInteraction(this);
    }

    public GL3DInteraction getCurrentInteraction() {
        return this.currentInteraction;
    }

    public double getDistanceToSunSurface() {
        return 0;
    }

    public String getName() {
        return "Test Camera";
    }

    public CoordinateSystem getViewSpaceCoordinateSystem() {
        return this.viewSpaceCoordinateSystem;
    }

    public GL3DMat4d getVM() {
        return getCameraTransformation().copy();
    }

    public void reset() {
        this.currentInteraction.reset(this);
    }

    public GL3DInteraction getPanInteraction() {
        throw new UnsupportedOperationException();
    }

    public GL3DInteraction getRotateInteraction() {
        throw new UnsupportedOperationException();
    }

    public GL3DInteraction getZoomInteraction() {
        throw new UnsupportedOperationException();
    }

    public void setCurrentInteraction(GL3DInteraction currentInteraction) {
        throw new UnsupportedOperationException();
    }

    class GL3DTestInteraction extends GL3DInteraction {

        public GL3DTestInteraction(GL3DCamera camera) {
            super(camera);
        }

        public void reset(GL3DCamera camera) {
            camera.getTranslation().set(0, 0, -15);
            camera.getRotation().clear();
            camera.updateCameraTransformation();
            System.out.println("Reset Test Camera");
        }

        public void mouseWheelMoved(MouseWheelEvent e, GL3DCamera camera) {
            double zoom = e.getScrollAmount() * e.getWheelRotation() * 0.1;
            // System.out.println("Zooming "+zoom);
            camera.getTranslation().add(new GL3DVec3d(0, 0, zoom));
            camera.updateCameraTransformation();
        }

        private Point mouseDownPoint = null;

        public void mouseDragged(MouseEvent e, GL3DCamera camera) {
            if (mouseDownPoint == null) {
                return;
            }
            // System.out.println("Dragged");

            Point currentPoint = e.getPoint();
            double dx = currentPoint.x - mouseDownPoint.x;
            double dy = currentPoint.y - mouseDownPoint.y;

            if (dx != 0)
                camera.getRotation().rotate(GL3DQuatd.createRotation(dx / 180, new GL3DVec3d(0, 1, 0)));
            if (dy != 0)
                camera.getRotation().rotate(GL3DQuatd.createRotation(dy / 180, new GL3DVec3d(1, 0, 0)));
            camera.updateCameraTransformation();
            mouseDownPoint = currentPoint;
        }

        public void mouseClicked(MouseEvent e, GL3DCamera camera) {
            if (e.getClickCount() == 2)
                camera.reset();
        }

        public void mousePressed(MouseEvent e, GL3DCamera camera) {
            mouseDownPoint = e.getPoint();
        }

        public void mouseReleased(MouseEvent e, GL3DCamera camera) {
            mouseDownPoint = null;
        }
    }
}
