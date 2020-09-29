package org.helioviewer.jhv.layers.fov;

import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

class FOVText {

    private static final double textScale = 0.075;

    static void drawLabel(JhvTextRenderer renderer, String text, double x, double y, double size) {
        float scaleFactor = (float) (textScale / renderer.getFont().getSize2D() * size);
        renderer.draw3D(text, (float) x, (float) y, 0, scaleFactor); // using SurfacePut
    }

}
