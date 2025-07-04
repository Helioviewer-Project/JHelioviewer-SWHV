package org.helioviewer.jhv.gui.components.statusplugin;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.JHVTransferHandler;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

@SuppressWarnings("serial")
public final class PositionStatusPanel extends StatusPanel.StatusPlugin implements MouseListener, MouseMotionListener {

    private static final String nanOrtho = String.format("%7s\u00B0,%7s\u00B0", "--", "--");
    private static final String nanLati = String.format("%7s\u00B0,%7s\u00B0", "--", "--");
    private static final String nanPolar = String.format("%7s\u00B0,%7s\u2609", "--", "--");

    private final Camera camera;

    public PositionStatusPanel() {
        setText(formatOrtho(Vec2.NAN, "", 0, 0, 0, 0, ImageData.nanValue));
        camera = Display.getCamera();
    }

    private void update(int x, int y) {
        Viewport vp = Display.getActiveViewport();
        Vec2 coord = Display.mode.scale.mouseToGrid(x, y, vp, camera, Display.gridType);

        if (Display.mode == Display.ProjectionMode.Latitudinal) {
            setText(formatLati(coord));
        } else if (Display.mode == Display.ProjectionMode.Polar || Display.mode == Display.ProjectionMode.LogPolar) {
            setText(formatPolar(coord));
        } else {
            String valueStr = ImageData.nanValue;
            Vec3 v = CameraHelper.getVectorFromSphereOrPlane(camera, vp, x, y, camera.getDragRotation());
            if (v == null) {
                setText(formatOrtho(Vec2.NAN, "", 0, 0, 0, 0, valueStr));
            } else {
                String annStr = "";
                double r = Math.sqrt(v.x * v.x + v.y * v.y);
                Position viewpoint = camera.getViewpoint();

                Object annData = JHVFrame.getInteraction().getAnnotationData();
                if (annData instanceof String str) {
                    annStr = str;
                } else if (r > 1 && annData instanceof Vec3 annv) {
                    Vec3 v_m = new Vec3(v.x / r, v.y / r, 0);
                    Vec3 vva = viewpoint.toQuat().rotateVector(annv);
                    Vec3 v_a = v.x < 0 ?
                            Vec3.cross(Vec3.cross(vva, Vec3.YAxis), Vec3.cross(Vec3.ZAxis, v_m)) :
                            Vec3.cross(Vec3.cross(Vec3.ZAxis, v_m), Vec3.cross(vva, Vec3.YAxis));
                    //System.out.println(">>> " + vva + " " + v_a);

                    double alpha = Math.atan2(r, viewpoint.distance);
                    double beta = Math.acos(Vec3.dot(v_a, Vec3.ZAxis));
                    double gamma = Math.PI - alpha - beta;
                    double h = /*Math.abs*/(viewpoint.distance * Math.sin(alpha) / Math.sin(gamma) - 1);

                    annStr = String.format("Hann: %7.2fMm", h * (Sun.RadiusMeter / 1e6));
                }

                double px = (180 / Math.PI) * Math.atan2(v.x, viewpoint.distance);
                double py = (180 / Math.PI) * Math.atan2(v.y, viewpoint.distance);
                double pa = MathUtils.mapTo0To360((180 / Math.PI) * Math.atan2(v.y, v.x) - (camera.getUpdateViewpoint() == UpdateViewpoint.equatorial ? 0 : 90)); // w.r.t. axis

                ImageLayer layer = Layers.getActiveImageLayer();
                ImageData id;
                if (layer != null && (id = layer.getImageData()) != null) {
                    valueStr = id.getPixelString((float) v.x, (float) v.y);
                }

                setText(formatOrtho(coord, annStr, r, pa, px, py, valueStr));
            }
        }
    }

    private static String formatXY(double p) {
        if (Math.abs(p) < 1) // accomodate SOLO
            return String.format("%+7d\u2033", (int) Math.round(3600 * p));
        else
            return String.format("%+7.2f\u00B0", p);
    }

    private static String formatR(double r) {
        if (r < 32 * Sun.Radius)
            return String.format("%7.2fR\u2609", r);
        else
            return String.format("%7.2fau", r * Sun.MeanEarthDistanceInv);
    }

    private static String formatLati(@Nonnull Vec2 coord) {
        String coordStr = coord == Vec2.NAN ? nanLati : String.format("%+7.2f\u00B0,%+7.2f\u00B0", coord.x, coord.y);
        return String.format("(\u03C6,\u03B8):(%s)", coordStr);
    }

    private static String formatPolar(@Nonnull Vec2 coord) {
        String coordStr = coord == Vec2.NAN ? nanPolar : String.format("%+7.2f\u00B0,%s", coord.x, formatR(coord.y));
        return String.format("(\u03B8,\u03c1):(%s)", coordStr);
    }

    private static String formatOrtho(@Nonnull Vec2 coord, String annStr, double r, double pa, double px, double py, String valueStr) {
        String coordStr = coord == Vec2.NAN ? nanOrtho : String.format("%+7.2f\u00B0,%+7.2f\u00B0", coord.x, coord.y);
        return String.format("%s | (\u03c1,\u03c8):(%s,%+7.2f\u00B0) | (\u03C6,\u03B8):(%s) | (x,y):(%s,%s) | %s", annStr, formatR(r), pa, coordStr, formatXY(px), formatXY(py), valueStr);
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
            JHVTransferHandler.getInstance().toClipboard(camera.getViewpoint().time.toString() + getText());
    }

}
