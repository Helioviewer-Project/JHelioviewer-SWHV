package org.helioviewer.jhv.layers.fov;

import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

import org.json.JSONObject;

@SuppressWarnings("serial")
class FOVInstrument extends DefaultMutableTreeNode {

    enum FOVType {RECTANGULAR, CIRCULAR}

    private static final double TEXT_SCALE = 0.075;
    private final FOVShape fov = new FOVShape();

    private final String name;
    private final FOVType type;
    private final double inner;
    private final double wide;
    private final double high;

    private boolean enabled;

    private double centerX = 0;
    private double centerY = 0;

    FOVInstrument(String _name, FOVType _type, double innerDeg, double wideDeg, double highDeg, JSONObject jo) {
        name = _name;
        type = _type;
        inner = 0.5 * Math.tan(innerDeg * (Math.PI / 180.));
        wide = 0.5 * Math.tan(wideDeg * (Math.PI / 180.));
        high = 0.5 * Math.tan(highDeg * (Math.PI / 180.));
        enabled = jo.optBoolean(name, false);
    }

    void setCenterX(double _centerX) {
        centerX = _centerX;
    }

    void setCenterY(double _centerY) {
        centerY = _centerY;
    }

    void putGeometry(double distance, byte[] color, SdfTextRenderer renderer, BufVertex lineBuf, BufVertex centerBuf) {
        if (!enabled)
            return;

        fov.setCenter(centerX * distance, centerY * distance);
        fov.putCenter(false, color, centerBuf);

        if (inner > 0)
            fov.putCircLine(inner * distance, false, color, lineBuf);
        if (type == FOVType.RECTANGULAR) {
            fov.putRectLine(wide * distance, high * distance, false, color, lineBuf);
            float x = (float) ((centerX - wide) * distance);
            float y = (float) ((centerY - high) * distance);
            double labelSize = high * distance;
            float scaleFactor = (float) (TEXT_SCALE / renderer.getFontSize() * labelSize);
            renderer.draw(name, x, y, 0, scaleFactor); // using SurfacePut
        } else {
            fov.putCircLine(wide * distance, false, color, lineBuf);
            double halfSide = wide / Math.sqrt(2);
            float x = (float) ((centerX - halfSide) * distance);
            float y = (float) ((centerY - halfSide) * distance);
            double labelSize = halfSide * distance;
            float scaleFactor = (float) (TEXT_SCALE / renderer.getFontSize() * labelSize);
            renderer.draw(name, x, y, 0, scaleFactor); // using SurfacePut
        }
    }

    boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean _enabled) {
        enabled = _enabled;
    }

    @Override
    public String toString() {
        return name;
    }

}
