package org.helioviewer.jhv.layers.fov;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.Enumeration;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.JHVSpinner;
import org.helioviewer.jhv.gui.interfaces.JHVCell;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.time.JHVTime;

import com.jogamp.opengl.GL2;

@SuppressWarnings("serial")
class FOVPlatform extends DefaultMutableTreeNode implements JHVCell {

    static final double LINEWIDTH_FOV = GLSLLine.LINEWIDTH_BASIC;
    private static final int SUBDIVISIONS = 180;
    private static final double HEMI_RADIUS = Sun.Radius + LINEWIDTH_FOV; // avoid intersecting solar surface

    private final GLSLLine hemiLine = new GLSLLine(false);

    private final String observer;
    private final byte[] color;
    private final boolean isSOLO;

    private final JPanel panel;
    private final JHVSpinner spinnerX;
    private final JHVSpinner spinnerY;

    FOVPlatform(String name, String _observer, byte[] _color) {
        observer = _observer;
        color = _color;
        isSOLO = "SOLO".equals(observer);

        spinnerX = createSpinner();
        spinnerX.addChangeListener(e -> setCenterX((Double) spinnerX.getValue()));
        spinnerY = createSpinner();
        spinnerY.addChangeListener(e -> setCenterY((Double) spinnerY.getValue()));

        panel = new JPanel(new GridLayout(1, 5, 0, 0));
        panel.setOpaque(false);
        panel.add(new JLabel(name));
        panel.add(new JLabel("\u03B4x ", JLabel.RIGHT));
        panel.add(spinnerX);
        panel.add(new JLabel("\u03B4y ", JLabel.RIGHT));
        panel.add(spinnerY);
    }

    @Override
    public Component getComponent() {
        return panel;
    }

    private void initHemiLine(GL2 gl) {
        int no_points = 2 * (SUBDIVISIONS + 3);
        BufVertex vexBuf = new BufVertex(no_points * GLSLLine.stride);
        Vec3 rotv = new Vec3(), v = new Vec3();

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double a = 2 * Math.PI * i / SUBDIVISIONS;
            v.x = HEMI_RADIUS * Math.cos(a);
            v.y = HEMI_RADIUS * Math.sin(a);
            v.z = 0.;

            if (i == 0) {
                vexBuf.putVertex(v, Colors.Null);
            }
            vexBuf.putVertex(v, i % 2 == 0 ? color : Colors.White);
        }
        vexBuf.putVertex(v, Colors.Null);

        for (int i = 0; i <= SUBDIVISIONS / 2; i++) {
            double a = 2 * Math.PI * i / SUBDIVISIONS;
            v.x = HEMI_RADIUS * Math.cos(a);
            v.y = HEMI_RADIUS * Math.sin(a);
            v.z = 0.;
            rotv = Quat.X90.rotateVector(v);
            if (i == 0) {
                vexBuf.putVertex(rotv, Colors.Null);
            }
            vexBuf.putVertex(rotv, i % 2 == 0 ? color : Colors.White);
        }
        vexBuf.putVertex(rotv, Colors.Null);

        for (int i = 0; i <= SUBDIVISIONS / 2; i++) {
            double a = 2 * Math.PI * i / SUBDIVISIONS + Math.PI / 2;
            v.x = HEMI_RADIUS * Math.cos(a);
            v.y = HEMI_RADIUS * Math.sin(a);
            v.z = 0.;
            rotv = Quat.Y90.rotateVector(v);
            if (i == 0) {
                vexBuf.putVertex(rotv, Colors.Null);
            }
            vexBuf.putVertex(rotv, i % 2 == 0 ? color : Colors.White);
        }
        vexBuf.putVertex(rotv, Colors.Null);

        hemiLine.setVertex(gl, vexBuf);
    }

    void init(GL2 gl) {
        hemiLine.init(gl);
        initHemiLine(gl);

        children().asIterator().forEachRemaining(c -> ((FOVInstrument) c).init(gl));
    }

    void dispose(GL2 gl) {
        hemiLine.dispose(gl);
        children().asIterator().forEachRemaining(c -> ((FOVInstrument) c).dispose(gl));
    }

    void render(Camera camera, Viewport vp, GL2 gl) {
        if (!hasEnabled())
            return;

        JHVTime time = camera.getViewpoint().time;
        Position obsPosition = Spice.getCarrington("SUN", observer, time);
        if (obsPosition == null)
            return;

        Transform.pushView();
        Transform.rotateViewInverse(obsPosition.toQuat());

        hemiLine.renderLine(gl, vp.aspect, LINEWIDTH_FOV);
        double[] rot;
        if (isSOLO && null != (rot = Spice.getRotation("SOLO_EQUAT_NORM", "SOLO_ORBIT_NORM", time))) { // SOLO pointing normal to orbit
            Transform.rotateViewInverse(Quat.createRotation(rot[2], Vec3.ZAxis));
        }

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        children().asIterator().forEachRemaining(c -> ((FOVInstrument) c).render(vp, gl, obsPosition.distance, pixFactor, color));

        Transform.popView();
    }

    private boolean hasEnabled() {
        Enumeration<TreeNode> e = children();
        while (e.hasMoreElements()) {
            if (((FOVInstrument) e.nextElement()).isEnabled())
                return true;
        }
        return false;
    }

    private static double control2Center(double v) { // v in arcmin
        return Math.tan(v * (Math.PI / 180. / 60.));
    }

    private void setCenterX(double controlX) {
        children().asIterator().forEachRemaining(c -> ((FOVInstrument) c).setCenterX(control2Center(controlX)));
        MovieDisplay.display();
    }

    private void setCenterY(double controlY) {
        children().asIterator().forEachRemaining(c -> ((FOVInstrument) c).setCenterY(control2Center(controlY)));
        MovieDisplay.display();
    }

    private static final double min = -60;
    private static final double max = 60;

    private static JHVSpinner createSpinner() {
        JHVSpinner spinner = new JHVSpinner(0, min, max, 0.1);
        JFormattedTextField f = ((JHVSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u2032", min, max));
        return spinner;
    }

}
