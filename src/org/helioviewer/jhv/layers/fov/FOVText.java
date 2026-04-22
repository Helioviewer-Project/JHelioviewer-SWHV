package org.helioviewer.jhv.layers.fov;

import org.helioviewer.jhv.opengl.text.TextRenderer;

class FOVText {

    private static final double textScale = 0.075;

    static void drawLabel(TextRenderer renderer, String text, double x, double y, double size) {
        float scaleFactor = (float) (textScale / renderer.getFontSize() * size);
        renderer.draw(text, (float) x, (float) y, 0, scaleFactor); // using SurfacePut
    }

}
