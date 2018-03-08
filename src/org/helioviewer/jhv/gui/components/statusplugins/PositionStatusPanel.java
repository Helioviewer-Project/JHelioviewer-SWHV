package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ClipBoardCopier;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

@SuppressWarnings("serial")
public class PositionStatusPanel extends StatusPanel.StatusPlugin implements MouseListener {

    private static final String nullCoordStr = "---\u00B0,---\u00B0";

    private final Camera camera;

    public PositionStatusPanel() {
        setText(formatOrtho(null, 0, 0, 0, 0));
        camera = Displayer.getCamera();
    }

    private void update(int x, int y) {
        Viewport vp = Displayer.getActiveViewport();
        Vec2 coord = Layers.getGridLayer().gridPoint(camera, vp, x, y);

        if (Displayer.mode == Displayer.DisplayMode.Latitudinal) {
            setText(String.format("(\u03C6,\u03B8) : (%.2f\u00B0,%.2f\u00B0)", coord.x, coord.y));
        } else if (Displayer.mode == Displayer.DisplayMode.Polar || Displayer.mode == Displayer.DisplayMode.LogPolar) {
            setText(String.format("(\u03B8,\u03c1) : (%.2f\u00B0,%.2fR\u2299)", coord.x, coord.y));
        } else {
            Vec3 v = CameraHelper.getVectorFromSphereOrPlane(camera, vp, x, y, camera.getCurrentDragRotation());
            if (v == null) {
                setText(formatOrtho(null, 0, 0, 0, 0));
            } else {
                double r = Math.sqrt(v.x * v.x + v.y * v.y);

                double d = camera.getViewpoint().distance;
                double px = (180 / Math.PI) * Math.atan2(v.x, d);
                double py = (180 / Math.PI) * Math.atan2(v.y, d);
                double pa = MathUtils.mapTo0To360((180 / Math.PI) * Math.atan2(v.y, v.x) - 90); // w.r.t. axis

                setText(formatOrtho(coord, r, px, py, pa));
            }
        }
    }

    private static String formatXY(double p) {
        if (Math.abs(p) < 0.5)
            return String.format("%+5d\u2033", (int) Math.round(3600 * p));
        else
            return String.format("%+.2f\u00B0", p);
    }

    private static String formatR(double r) {
        if (r < 32 * Sun.Radius)
            return String.format("%.2fR\u2299", r);
        else
            return String.format("%.2fau", r * Sun.MeanEarthDistanceInv);
    }

    private static String formatOrtho(Vec2 coord, double r, double px, double py, double pa) {
        String coordStr;
        if (coord == null || Double.isNaN(coord.x) || Double.isNaN(coord.y))
            coordStr = nullCoordStr;
        else
            coordStr = String.format("%+7.2f\u00B0,%+7.2f\u00B0", coord.x, coord.y);
        return String.format("(\u03C6,\u03B8) : (%s) | \u03c1 : %s | (x,y,\u03c8) : (%s,%s,%6.2f)", coordStr, formatR(r), formatXY(px), formatXY(py), pa);
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
        if (e.getButton() == MouseEvent.BUTTON3)
            ClipBoardCopier.getSingletonInstance().setString(getText());
    }

}
