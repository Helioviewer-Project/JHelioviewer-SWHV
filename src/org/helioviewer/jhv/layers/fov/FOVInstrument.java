package org.helioviewer.jhv.layers.fov;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.interfaces.JHVCell;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

import com.jogamp.opengl.GL2;

@SuppressWarnings("serial")
class FOVInstrument extends DefaultMutableTreeNode implements JHVCell {

    enum FOVType {RECTANGULAR, CIRCULAR}

    private static final double LINEWIDTH_FOV = GLSLLine.LINEWIDTH_BASIC;

    private final FOVShape fov = new FOVShape();
    private final GLSLLine fovLine = new GLSLLine(true);
    private final BufVertex lineBuf = new BufVertex((4 * (FOVShape.RECT_SUBDIVS + 1) + 2) * GLSLLine.stride);
    private final GLSLShape center = new GLSLShape(true);
    private final BufVertex centerBuf = new BufVertex(GLSLShape.stride);

    private final String name;
    private final FOVType type;
    private final double inner;
    private final double wide;
    private final double high;
    private final byte[] color;

    private final JPanel panel;
    private final JCheckBox checkBox;

    private double centerX = 0;
    private double centerY = 0;

    FOVInstrument(String _name, FOVType _type, double innerDeg, double wideDeg, double highDeg, byte[] _color) {
        name = _name;
        type = _type;
        inner = 0.5 * Math.tan(innerDeg * (Math.PI / 180.));
        wide = 0.5 * Math.tan(wideDeg * (Math.PI / 180.));
        high = 0.5 * Math.tan(highDeg * (Math.PI / 180.));
        color = _color;

        panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        checkBox = new JCheckBox(name);
        checkBox.addActionListener(e -> MovieDisplay.display());
        checkBox.setFocusPainted(false);
        checkBox.setOpaque(false);

        panel.add(checkBox, BorderLayout.LINE_START);
        panel.add(new JLabel("      "), BorderLayout.LINE_END); // avoid ellipsis on Windows
        ComponentUtils.smallVariant(panel);
    }

    @Override
    public Component getComponent() {
        return panel;
    }

    void init(GL2 gl) {
        fovLine.init(gl);
        center.init(gl);
    }

    void dispose(GL2 gl) {
        fovLine.dispose(gl);
        center.dispose(gl);
    }

    void setCenterX(double _centerX) {
        centerX = _centerX;
    }

    void setCenterY(double _centerY) {
        centerY = _centerY;
    }

    void render(Camera camera, Viewport vp, GL2 gl, Position obsPosition) {
        if (!checkBox.isSelected() || obsPosition == null)
            return;

        double distance = obsPosition.distance;
        double pixFactor = CameraHelper.getPixelFactor(camera, vp);

        Transform.pushView();
        Transform.rotateViewInverse(obsPosition.toQuat());

        boolean far = Camera.useWideProjection(distance);
        if (far) {
            Transform.pushProjection();
            camera.projectionOrthoWide(vp.aspect);
        }

        fov.setCenter(centerX * distance, centerY * distance);
        fov.putCenter(centerBuf, color);

        JhvTextRenderer renderer = GLText.getRenderer(48);
        renderer.setColor(Colors.WhiteFloat);
        renderer.begin3DRendering();
        renderer.setSurfacePut();

        if (inner > 0)
            fov.putCircLine(inner * distance, lineBuf, color);
        if (type == FOVType.RECTANGULAR) {
            fov.putRectLine(wide * distance, high * distance, lineBuf, color);
            FOVText.drawLabel(renderer, name, (centerX - wide) * distance, (centerY - high) * distance, high * distance);
        } else {
            fov.putCircLine(wide * distance, lineBuf, color);
            double halfSide = wide / Math.sqrt(2);
            FOVText.drawLabel(renderer, name, (centerX - halfSide) * distance, (centerY - halfSide) * distance, halfSide * distance);
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

    boolean isEnabled() {
        return checkBox.isSelected();
    }

}
