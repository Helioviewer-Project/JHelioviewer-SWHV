package org.helioviewer.jhv.gui.components.statusplugin;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.annotations.Annotations;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.input.InputPointerListener;
import org.helioviewer.jhv.input.InputPointerMotionListener;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.swing.TransferAccess;

@SuppressWarnings("serial")
public final class PositionStatusPanel extends StatusPanel.StatusPlugin implements InputPointerListener, InputPointerMotionListener {

    private static final String nanOrtho = String.format("%7s\u00B0,%7s\u00B0", "--", "--");
    private static final String nanHpc = String.format("%7s,%7s", "--", "--");
    private static final String nanLati = String.format("%7s\u00B0,%7s\u00B0", "--", "--");
    private static final String nanPolar = String.format("%7s\u00B0,%7s\u2609", "--", "--");

    public PositionStatusPanel() {
        setText(formatOrtho(Vec2.NAN, 0, 0, 0, 0));
    }

    private void update(int x, int y) {
        Viewport vp = Display.getActiveViewport();
        MapView mv = GLRenderer.getMapView();
        MapMode mode = mv.mode();
        Vec2 coord = mv.mouseToGrid(vp, x, y);

        if (mode == MapMode.HPC) {
            setText(formatHpc(coord));
        } else if (mode == MapMode.Latitudinal) {
            setText(formatLati(coord));
        } else if (mode == MapMode.Polar || mode == MapMode.LogPolar) {
            setText(formatPolar(coord));
        } else {
            Vec3 v = mv.mouseToSky(vp, x, y);
            if (v == null) {
                setText(formatOrtho(Vec2.NAN, 0, 0, 0, 0));
            } else {
                String annStr = "";
                double r = Math.sqrt(v.x * v.x + v.y * v.y);
                Position viewpoint = GLRenderer.getDisplayedViewpoint();

                Object annData = Annotations.getAnnotationData();
                if (annData instanceof String str) {
                    annStr = str;
                } /* else if (r > 1 && annData instanceof Vec3 annv) {
                    Vec3 v_m = new Vec3(v.x / r, v.y / r, 0);
                    Vec3 vva = viewpoint.toQuat().rotateVector(annv);
                    Vec3 v_a = v.x < 0 ?
                            Vec3.cross(Vec3.cross(vva, Vec3.YAxis), Vec3.cross(Vec3.ZAxis, v_m)) :
                            Vec3.cross(Vec3.cross(Vec3.ZAxis, v_m), Vec3.cross(vva, Vec3.YAxis));
                    v_a.normalize();
                    //System.out.println(">>> " + vva + " " + v_a);

                    double alpha = Math.atan2(r, viewpoint.distance);
                    double beta = Math.acos(Vec3.dot(v_a, Vec3.ZAxis));
                    double gamma = Math.PI - alpha - beta;
                    double h = (viewpoint.distance * Math.sin(alpha) / Math.sin(gamma) - 1);

                    annStr = String.format("Hann: %7.2fMm", h * (Sun.RadiusMeter / 1e6));
                } */

                double zeta = viewpoint.distance - v.z;
                double px = (180 / Math.PI) * Math.atan2(v.x, zeta);
                double py = (180 / Math.PI) * Math.atan2(v.y, Math.sqrt(v.x * v.x + zeta * zeta));
                double pa = MathUtils.mapTo0To360((180 / Math.PI) * Math.atan2(v.y, v.x) - (DisplayController.getViewpointUpdate() == UpdateViewpoint.equatorial ? 0 : 90)); // w.r.t. axis
                String ortho = formatOrtho(coord, r, pa, px, py);
                setText(annStr.isEmpty() ? ortho : annStr + " | " + ortho);
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

    private static String formatHpc(@Nonnull Vec2 coord) {
        String coordStr = coord == Vec2.NAN ? nanHpc : String.format("%s,%s", formatXY(coord.x), formatXY(coord.y));
        return String.format("(Tx,Ty):(%s)", coordStr);
    }

    private static String formatPolar(@Nonnull Vec2 coord) {
        String coordStr = coord == Vec2.NAN ? nanPolar : String.format("%+7.2f\u00B0,%s", coord.x, formatR(coord.y));
        return String.format("(\u03B8,\u03c1):(%s)", coordStr);
    }

    private static String formatOrtho(@Nonnull Vec2 coord, double r, double pa, double px, double py) {
        String coordStr = coord == Vec2.NAN ? nanOrtho : String.format("%+7.2f\u00B0,%+7.2f\u00B0", coord.x, coord.y);
        return String.format("(\u03c1,\u03c8):(%s,%+7.2f\u00B0) | (\u03C6,\u03B8):(%s) | (x,y):(%s,%s)", formatR(r), pa, coordStr, formatXY(px), formatXY(py));
    }

    @Override
    public void mouseDragged(PointerEvent e) {
        update(e.x(), e.y());
    }

    @Override
    public void mouseMoved(PointerEvent e) {
        update(e.x(), e.y());
    }

    @Override
    public void mouseClicked(PointerEvent e) {
        maybeCopyToClipboard(e);
    }

    @Override
    public void mousePressed(PointerEvent e) {
        maybeCopyToClipboard(e);
    }

    @Override
    public void mouseReleased(PointerEvent e) {
        maybeCopyToClipboard(e);
    }

    private void maybeCopyToClipboard(PointerEvent e) {
        if (e.popupTrigger() || e.button() == 3)
            TransferAccess.writeClipboard(GLRenderer.getDisplayedViewpoint().time.toString() + getText());
    }

}
