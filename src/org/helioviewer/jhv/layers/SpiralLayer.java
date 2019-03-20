package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.annotation.Nullable;
//import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
//import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.position.Position;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class SpiralLayer extends AbstractLayer {

    private static final double ROT = (2 * Math.PI) / (Carrington.CR_SIDEREAL * 86400);
    private static final double SPIRAL_RADIUS = 3 * Sun.MeanEarthDistance;
    private static final int SPIRAL_DIVISIONS = 64;
    private static final int SPIRAL_ARMS = 9;
    private static final double LINEWIDTH_SPIRAL = 2 * GLSLLine.LINEWIDTH_BASIC;

    private final GLSLLine spiral = new GLSLLine(true);
    private final BufVertex spiralBuf = new BufVertex(SPIRAL_ARMS * (2 * SPIRAL_DIVISIONS + 1 + 2) * GLSLLine.stride);
    private final byte[] spiralColor = Colors.Green;

    private static final double MIN_SPEED = 200;
    private static final double MAX_SPEED = 5000;
    private double speed = 500;

    private final JPanel optionsPanel;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("speed", speed);
    }

    public SpiralLayer(JSONObject jo) {
        if (jo != null)
            speed = MathUtils.clip(jo.optDouble("speed", speed), MIN_SPEED, MAX_SPEED);
        optionsPanel = optionsPanel();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        Position viewpoint = camera.getViewpoint();

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());
        boolean far = Camera.useWideProjection(viewpoint.distance);
        if (far) {
            Transform.pushProjection();
            camera.projectionOrthoWide(vp.aspect);
        }

        spiralRender(gl, vp, viewpoint);

        if (far) {
            Transform.popProjection();
        }
        Transform.popView();
    }

    private void spiralPutVertex(double rad, double lon, double lat, byte[] color) {
        float x = (float) (rad * Math.cos(lat) * Math.cos(lon));
        float y = (float) (rad * Math.cos(lat) * Math.sin(lon));
        float z = (float) (rad * Math.sin(lat));
        spiralBuf.putVertex(x, y, z, 1, color);
    }

    private void spiralRender(GL2 gl, Viewport vp, Position viewpoint) {
        double sr = speed * Sun.RadiusKMeterInv / ROT;
        // control point
        Position p0 = Sun.getEarth(viewpoint.time);
        double rad0 = p0.distance;
        double lon0 = 0;
        double lat0 = 0;

        for (int j = 0; j < SPIRAL_ARMS; j++) {
            double lona = lon0 + j * (2 * Math.PI / SPIRAL_ARMS); // arm longitude
            // before control point
            for (int i = 0; i < SPIRAL_DIVISIONS; i++) {
                double rad = (Sun.Radius + (rad0 - Sun.Radius) * i / (double) SPIRAL_DIVISIONS);
                double lon = lona - (rad - rad0) / sr;
                if (i == 0) {
                    spiralPutVertex(rad, lon, lat0, Colors.Null);
                    spiralBuf.repeatVertex(spiralColor);
                } else {
                    spiralPutVertex(rad, lon, lat0, spiralColor);
                }
            }
            // after control point
            for (int i = 0; i <= SPIRAL_DIVISIONS; i++) {
                double rad = (rad0 + (SPIRAL_RADIUS - rad0) * i / (double) SPIRAL_DIVISIONS);
                double lon = lona - (rad - rad0) / sr;
                if (i == SPIRAL_DIVISIONS) {
                    spiralPutVertex(rad, lon, lat0, spiralColor);
                    spiralBuf.repeatVertex(Colors.Null);
                } else {
                    spiralPutVertex(rad, lon, lat0, spiralColor);
                }
            }
        }

        spiral.setData(gl, spiralBuf);
        spiral.render(gl, vp.aspect, LINEWIDTH_SPIRAL);
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void init(GL2 gl) {
        spiral.init(gl);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Spiral";
    }

    @Nullable
    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void dispose(GL2 gl) {
        spiral.dispose(gl);
    }

    private JPanel optionsPanel() {
/*
        JFormattedTextField propagationField = new JFormattedTextField(new TerminatedFormatterFactory("%.3f", "km/s", MIN_SPEED, MAX_SPEED));
        propagationField.setValue(speed);
        propagationField.setColumns(10);
        propagationField.addPropertyChangeListener("value", e -> speed = (Double) propagationField.getValue());
*/
        JLabel label = new JLabel((int) speed + " km/s");
        JSlider slider = new JSlider(JSlider.HORIZONTAL, (int) MIN_SPEED, (int) MAX_SPEED, (int) speed);
        slider.addChangeListener(e -> {
            speed = slider.getValue();
            label.setText((int) speed + " km/s");
            MovieDisplay.display();
        });
        WheelSupport.installMouseWheelSupport(slider);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        panel.add(new JLabel("Speed", JLabel.RIGHT), c0);
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 1;
        panel.add(/*propagationField*/slider, c0);
        c0.gridx = 2;
        panel.add(label, c0);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
