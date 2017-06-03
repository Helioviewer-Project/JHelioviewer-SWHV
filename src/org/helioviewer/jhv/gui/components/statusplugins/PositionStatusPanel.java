package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.base.astronomy.Sun;
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
        Vec2 coord = ImageViewerGui.getRenderableContainer().getRenderableGrid().gridPoint(camera, vp, x, y);

        if (Displayer.mode == Displayer.DisplayMode.Latitudinal) {
            setText(String.format("(\u03C6,\u03B8) : (%.2f\u00B0,%.2f\u00B0)", coord.x, coord.y));
        } else if (Displayer.mode == Displayer.DisplayMode.Polar || Displayer.mode == Displayer.DisplayMode.LogPolar) {
            setText(String.format("(\u03B8,\u03c1) : (%.2f\u00B0,%.2fR\u2299)", coord.x, coord.y));
        } else {
            Vec3 v = CameraHelper.getVectorFromSphereOrPlane(camera, vp, x, y, camera.getCurrentDragRotation());
            if (v == null) {
                setText(formatOrtho(null, 0, 0, 0));
            } else {
                double r = Math.sqrt(v.x * v.x + v.y * v.y);

                double d = camera.getViewpoint().distance;
                int px = (int) Math.round((3600 * 180 / Math.PI) * Math.atan2(v.x, d));
                int py = (int) Math.round((3600 * 180 / Math.PI) * Math.atan2(v.y, d));

                setText(formatOrtho(coord, r, px, py));
            }
        }
    }

    private static String formatArcsec(int a) {
        if (Math.abs(a) < 1800)
            return String.format("%+5d\u2033", a);
        else
            return String.format("%+.2f\u00B0", a / 3600.);
    }

    private static String formatR(double r) {
        if (r < 32 * Sun.Radius)
            return String.format("%.2fR\u2299", r);
        else
            return String.format("%.2fau", r * Sun.MeanEarthDistanceInv);
    }

    private static String formatOrtho(Vec2 coord, double r, int px, int py) {
        String coordStr;
        if (coord == null || Double.isNaN(coord.x) || Double.isNaN(coord.y))
            coordStr = nullCoordStr;
        else
            coordStr = String.format("%+7.2f\u00B0,%+7.2f\u00B0", coord.x, coord.y);
        return String.format("(\u03C6,\u03B8) : (%s) | \u03c1 : %s | (x,y) : (%s,%s)", coordStr, formatR(r), formatArcsec(px), formatArcsec(py));
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
