package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.StatusPanel;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

@SuppressWarnings("serial")
public class PositionStatusPanel extends StatusPanel.StatusPlugin implements MouseListener {

    private static final String nullCoordStr = "---\u00B0,---\u00B0";

    private final Camera camera;

    public PositionStatusPanel() {
        setText(formatOrtho(null, 0, 0, 0));
        camera = Displayer.getCamera();
    }

    private void update(int x, int y) {
        Viewport vp = Displayer.getActiveViewport();
        Vec2 coord = ImageViewerGui.getRenderableGrid().gridPoint(camera, vp, x, y);

        if (Displayer.mode == Displayer.DisplayMode.LATITUDINAL) {
            setText(String.format("(\u03C6,\u03B8) : (%.2f\u00B0,%.2f\u00B0)", coord.x, coord.y));
        } else if (Displayer.mode == Displayer.DisplayMode.POLAR || Displayer.mode == Displayer.DisplayMode.LOGPOLAR) {
            setText(String.format("(\u03B8,\u03c1) : (%.2f\u00B0,%.2fR\u2299)", coord.x, coord.y));
        } else {
            Vec3 v = CameraHelper.getVectorFromSphereOrPlane(camera, vp, x, y, camera.getCurrentDragRotation());
            double r = Math.sqrt(v.x * v.x + v.y * v.y);

            double d = camera.getViewpoint().distance;
            int px = (int) Math.round((3600 * 180 / Math.PI) * Math.atan2(v.x, d));
            int py = (int) Math.round((3600 * 180 / Math.PI) * Math.atan2(v.y, d));

            setText(formatOrtho(coord, r, px, py));
        }
    }

    private String formatOrtho(Vec2 coord, double r, int px, int py) {
        String coordStr;
        if (coord == null)
            coordStr = nullCoordStr;
        else
            coordStr = String.format("%+7.2f\u00B0,%+7.2f\u00B0", coord.x, coord.y);

        return String.format("(\u03C6,\u03B8) : (%s) | \u03c1 : %.2fR\u2299 | (x,y) : (%+5d\u2033,%+5d\u2033)", coordStr, r, px, py);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        update(e.getX(), e.getY());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        update(e.getX(), e.getY());
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

}
