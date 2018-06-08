package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLMatrix;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class MiniviewLayer extends AbstractLayer {

    private static final int MIN_SCALE = 5;
    private static final int MAX_SCALE = 15;
    private int scale = 10;

    private static final float[] colorCircle = { 1 * 0.2f, 0, 0, 0.2f };
    private static final float[] colorRectangle = { 0, 1 * 0.2f, 0, 0.2f };
    private static final GLSLShape circle = new GLSLShape();
    private static final GLSLShape rectangle = new GLSLShape();

    private final JPanel optionsPanel;
    private Viewport miniViewport = new Viewport(0, 0, 0, 100, 100);

    @Override
    public void serialize(JSONObject jo) {
        jo.put("scale", scale);
    }

    public MiniviewLayer(JSONObject jo) {
        if (jo != null)
            scale = MathUtils.clip(jo.optInt("scale", scale), MIN_SCALE, MAX_SCALE);
        else
            setEnabled(true);
        optionsPanel = optionsPanel();
        reshapeViewport();
    }

    public void reshapeViewport() {
        int vpw = Display.fullViewport.width;
        int offset = (int) (vpw * 0.01);
        int size = (int) (vpw * 0.01 * scale);
        miniViewport = new Viewport(0, offset, offset, size, size);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
    }

    public static void renderBackground(Camera camera, Viewport vp, GL2 gl) {
        gl.glDepthRange(0, 0);
        GLMatrix.push();
        GLMatrix.mulView(camera.getViewpoint().toQuat().toMatrixTranspose().getFloatArray());
        {
            rectangle.renderShape(gl, GL2.GL_TRIANGLE_FAN);
            circle.renderShape(gl, GL2.GL_TRIANGLE_FAN);
        }
        GLMatrix.pop();
        gl.glDepthRange(0, 1);
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
        return "Miniview";
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
    public void init(GL2 gl) {
        circle.init(gl);
        GLHelper.initCircleFront(gl, circle, 0, 0, 1, 36, colorCircle);
        rectangle.init(gl);
        GLHelper.initRectangleFront(gl, rectangle, -30, -30, 60, 60, colorRectangle);
    }

    @Override
    public void dispose(GL2 gl) {
        circle.dispose(gl);
        rectangle.dispose(gl);
    }

    public Viewport getViewport() {
        return miniViewport;
    }

    private JPanel optionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JSlider slider = new JSlider(JSlider.HORIZONTAL, MIN_SCALE, MAX_SCALE, scale);
        slider.addChangeListener(e -> {
            scale = slider.getValue();
            reshapeViewport();
            Display.display();
        });
        WheelSupport.installMouseWheelSupport(slider);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.EAST;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        panel.add(new JLabel("Size", JLabel.RIGHT), c0);
        c0.anchor = GridBagConstraints.WEST;
        c0.gridx = 1;
        panel.add(slider, c0);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
