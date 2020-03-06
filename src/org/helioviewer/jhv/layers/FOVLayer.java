package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.annotation.Nullable;
import javax.swing.JCheckBox;
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
import org.helioviewer.jhv.layers.fov.FOVText;
import org.helioviewer.jhv.layers.fov.FOVTreePane;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class FOVLayer extends AbstractLayer {

    private static final double LINEWIDTH_FOV = GLSLLine.LINEWIDTH_BASIC;

    private final FOVShape fov = new FOVShape();
    private final byte[] fovColor = Colors.Blue;
    private final GLSLLine fovLine = new GLSLLine(true);
    private final BufVertex lineBuf = new BufVertex((4 * (FOVShape.RECT_SUBDIVS + 1) + 2) * GLSLLine.stride);
    private final GLSLShape center = new GLSLShape(true);
    private final BufVertex centerBuf = new BufVertex(GLSLShape.stride);

    private final FOVTreePane treePane = new FOVTreePane();
    private final JPanel optionsPanel;

    private static boolean customEnabled;
    private static double customAngle = Camera.INITFOV / Math.PI * 180;

    @Override
    public void serialize(JSONObject jo) {
    }

    public FOVLayer(JSONObject jo) {
        optionsPanel = buildOptionsPanel();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        if (!customEnabled && !treePane.hasEnabled())
            return;

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        Position viewpoint = camera.getViewpoint();

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());
        boolean far = Camera.useWideProjection(viewpoint.distance);
        if (far) {
            Transform.pushProjection();
            camera.projectionOrthoWide(vp.aspect);
        }

        JhvTextRenderer renderer = GLText.getRenderer(48);
        renderer.setColor(Colors.WhiteFloat);
        renderer.begin3DRendering();
        renderer.setSurfacePut();

        treePane.putFOV(fov, viewpoint.distance, lineBuf, centerBuf, renderer);
        if (customEnabled) {
            fov.setCenter(0, 0);
            fov.putCenter(centerBuf, fovColor);

            double halfSide = 0.5 * viewpoint.distance * Math.tan(customAngle * (Math.PI / 180.));
            fov.putRectLine(halfSide, halfSide, lineBuf, fovColor);
            FOVText.drawLabel(renderer, "Custom", -halfSide, -halfSide, halfSide);
        }

        renderer.setDirectPut();
        renderer.end3DRendering();

        center.setData(gl, centerBuf);
        center.renderPoints(gl, pixFactor);

        fovLine.setData(gl, lineBuf);
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

    private JPanel buildOptionsPanel() {
        double fovMin = 0, fovMax = 180;
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(customAngle, fovMin, fovMax, 0.01));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u00B0", fovMin, fovMax));
        WheelSupport.installMouseWheelSupport(spinner);
        spinner.addChangeListener(e -> {
            customAngle = (Double) spinner.getValue();
            MovieDisplay.display();
        });

        JCheckBox customCheckBox = new JCheckBox("Custom FOV", false);
        customCheckBox.addChangeListener(e -> {
            customEnabled = customCheckBox.isSelected();
            MovieDisplay.display();
        });

        JPanel customPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        customPanel.add(customCheckBox, c0);
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 1;
        customPanel.add(spinner, c0);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c1 = new GridBagConstraints();
        c1.fill = GridBagConstraints.BOTH;
        c1.weightx = 1;
        c1.gridx = 0;

        c1.weighty = 0;
        c1.gridy = 0;
        panel.add(customPanel, c1);

        c1.weighty = 1;
        c1.gridy = 1;
        panel.add(treePane, c1);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
