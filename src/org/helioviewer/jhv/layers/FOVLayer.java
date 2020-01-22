package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.annotation.Nullable;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class FOVLayer extends AbstractLayer {

    private static final double LINEWIDTH_FOV = GLSLLine.LINEWIDTH_BASIC;

    private final FOVShape fov = new FOVShape();
    private final byte[] fovColor = Colors.Blue;
    private final GLSLLine fovLine = new GLSLLine(true);
    private final BufVertex fovBuf = new BufVertex((4 * (FOVShape.SUBDIVISIONS + 1) + 2) * GLSLLine.stride);
    private final GLSLShape center = new GLSLShape(true);
    private final BufVertex centerBuf = new BufVertex(GLSLShape.stride);

    private final JPanel optionsPanel;

    private double fovAngle = Camera.INITFOV / Math.PI * 180;

    @Override
    public void serialize(JSONObject jo) {
    }

    public FOVLayer(JSONObject jo) {
        optionsPanel = optionsPanel();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        Position viewpoint = camera.getViewpoint();
        double halfSide = 0.5 * viewpoint.distance * Math.tan(fovAngle * (Math.PI / 180.));

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());
        boolean far = Camera.useWideProjection(viewpoint.distance);
        if (far) {
            Transform.pushProjection();
            camera.projectionOrthoWide(vp.aspect);
        }

        fov.putCenter(centerBuf, fovColor);
        center.setData(gl, centerBuf);
        center.renderPoints(gl, pixFactor);

        fov.putLine(halfSide, halfSide, fovBuf, fovColor);
        fovLine.setData(gl, fovBuf);
        fovLine.render(gl, vp.aspect, LINEWIDTH_FOV);

        if (far) {
            Transform.popProjection();
        }
        Transform.popView();
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void init(GL2 gl) {
        fovLine.init(gl);
        center.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        fovLine.dispose(gl);
        center.dispose(gl);
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
        return "FOV";
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

    private JPanel optionsPanel() {
        double fovMin = 0, fovMax = 180;
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(fovAngle), Double.valueOf(fovMin), Double.valueOf(fovMax), Double.valueOf(0.01)));
        spinner.setMaximumSize(new Dimension(6, 22));
        spinner.addChangeListener(e -> {
            fovAngle = (Double) spinner.getValue();
            MovieDisplay.display();
        });
        JFormattedTextField f = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u00B0", fovMin, fovMax));
        WheelSupport.installMouseWheelSupport(spinner);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        panel.add(new JLabel("FOV angle", JLabel.RIGHT), c0);
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 1;
        panel.add(spinner, c0);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
