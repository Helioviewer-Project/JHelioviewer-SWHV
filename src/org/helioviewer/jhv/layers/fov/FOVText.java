package org.helioviewer.jhv.layers.fov;

import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

public class FOVText {

    private static final double textEpsilon = 0.09;
    private static final double textScale = 0.075;

    public static void drawLabel(JhvTextRenderer renderer, String name, double x, double y, double size) {
        float scaleFactor = (float) (textScale / renderer.getFont().getSize2D() * size);
        renderer.draw3D(name, (float) x, (float) y, (float) (FOVShape.computeZ(x, y) + textEpsilon), scaleFactor);
    }

}
