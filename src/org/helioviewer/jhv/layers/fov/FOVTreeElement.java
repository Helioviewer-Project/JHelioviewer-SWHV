package org.helioviewer.jhv.layers.fov;

import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

@SuppressWarnings("serial")
class FOVTreeElement extends DefaultMutableTreeNode {

    enum FOVType {RECTANGULAR, CIRCULAR}

    private static final double textEpsilon = 0.09;
    private static final double textScale = 0.075;

    private final String name;
    private final FOVType type;
    private final double inner;
    private final double wide;
    private final double high;
    private final byte[] color;
    private boolean enabled;

    private double centerX = 0;
    private double centerY = 0;

    private static void drawLabel(String name, double x, double y, double size, JhvTextRenderer renderer) {
        float textScaleFactor = (float) (textScale / renderer.getFont().getSize2D() * size);
        renderer.draw3D(name, (float) x, (float) y, (float) (FOVShape.computeZ(x, y) + textEpsilon), textScaleFactor);
    }

    FOVTreeElement(String _name, FOVType _type, double innerDeg, double wideDeg, double highDeg, byte[] _color) {
        name = _name;
        type = _type;
        inner = 0.5 * Math.tan(innerDeg * (Math.PI / 180.));
        wide = 0.5 * Math.tan(wideDeg * (Math.PI / 180.));
        high = 0.5 * Math.tan(highDeg * (Math.PI / 180.));
        color = _color;
    }

    void setCenterX(double _centerX) {
        centerX = _centerX;
    }

    void setCenterY(double _centerY) {
        centerY = _centerY;
    }

    void putFOV(FOVShape f, double distance, BufVertex lineBuf, BufVertex centerBuf, JhvTextRenderer renderer) {
        if (!enabled)
            return;

        f.setCenter(centerX * distance, centerY * distance);
        f.putCenter(centerBuf, color);

        if (inner > 0)
            f.putCircLine(inner * distance, lineBuf, color);
        if (type == FOVType.RECTANGULAR) {
            f.putRectLine(wide * distance, high * distance, lineBuf, color);
            drawLabel(name, (wide + centerX) * distance, (-high + centerY) * distance, high * distance, renderer);
        } else {
            f.putCircLine(wide * distance, lineBuf, color);
            double halfSide = wide / Math.sqrt(2);
            drawLabel(name, (halfSide + centerX) * distance, (-halfSide + centerY) * distance, halfSide * distance, renderer);
        }
    }

    void zoom() {
        Camera camera = Display.getCamera();
        double distance = camera.getViewpoint().distance;
        camera.setTranslation(-centerX * distance, -centerY * distance);
        camera.resetDragRotation();
        camera.setFOV(2 * wide);
    }

    boolean isEnabled() {
        return enabled;
    }

    void toggle() {
        enabled = !enabled;
    }

    @Override
    public String toString() {
        return name;
    }

}
