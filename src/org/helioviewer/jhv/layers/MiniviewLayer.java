package org.helioviewer.jhv.layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayLayout;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.json.JSONObject;

import org.lwjgl.opengl.GL33;

import com.jogamp.opengl.GL3;

public final class MiniviewLayer extends AbstractLayer {

    private static final int MIN_SCALE = 5;
    private static final int MAX_SCALE = 15;
    private int scale = 10;

    private static final byte[] colorCircle = Colors.bytes(Color.RED, 0.2);
    private static final byte[] colorRectangle = Colors.bytes(Color.GREEN, 0.2);
    private final GLSLShape circle = new GLSLShape(false);
    private final GLSLShape rectangle = new GLSLShape(false);

    private final JPanel optionsPanel;
    private Viewport miniViewport = DisplayLayout.viewport(0, 0, 0, 100, 100, 100);

    @Override
    public void serialize(JSONObject jo) {
        jo.put("scale", scale);
    }

    public MiniviewLayer(JSONObject jo) {
        if (jo != null)
            scale = Math.clamp(jo.optInt("scale", scale), MIN_SCALE, MAX_SCALE);
        else
            setEnabled(true);
        optionsPanel = optionsPanel();
        reshapeViewport();
    }

    public void reshapeViewport() {
        int vpw = Display.fullViewport.width;
        int offset = (int) (vpw * 0.01);
        int size = (int) (vpw * 0.01 * scale);
        miniViewport = DisplayLayout.viewport(0, offset, offset, size, size, Display.fullViewport.height);
    }

    public void renderBackground() {
        rectangle.renderShape(GL33.GL_TRIANGLE_STRIP);
        circle.renderShape(GL33.GL_TRIANGLE_STRIP);
    }

    @Override
    public void remove(GL3 gl) {
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

    @Override
    public void init(GL3 gl) {
        circle.init();
        GLHelper.initCircleFront(circle, 0, 0, 1, 18, colorCircle);
        rectangle.init();
        GLHelper.initRectangleFront(rectangle, -30, -30, 60, 60, colorRectangle);
    }

    @Override
    public void dispose(GL3 gl) {
        circle.dispose();
        rectangle.dispose();
    }

    public Viewport getViewport() {
        return miniViewport;
    }

    private JPanel optionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JHVSlider slider = new JHVSlider(MIN_SCALE, MAX_SCALE, scale);
        slider.addChangeListener(e -> {
            scale = slider.getValue();
            reshapeViewport();
            MovieDisplay.display();
        });

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 0;
        panel.add(new JLabel("Size", JLabel.RIGHT), c);
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 1;
        panel.add(slider, c);

        return panel;
    }

}
