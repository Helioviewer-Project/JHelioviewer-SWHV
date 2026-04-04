package org.helioviewer.jhv.opengl;

import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;

public class GLInfo {

    public static final int GLSAMPLES = 4;

    public static final double[] pixelScale = {1, 1};
    public static String glVersion = "";

    public static int maxTextureSize;

    public static void updatePixelScale(GraphicsConfiguration gc) {
        if (gc != null) {
            AffineTransform tx = gc.getDefaultTransform();
            pixelScale[0] = tx.getScaleX();
            pixelScale[1] = tx.getScaleY();
        } else {
            pixelScale[0] = 1;
            pixelScale[1] = 1;
        }
    }

}
