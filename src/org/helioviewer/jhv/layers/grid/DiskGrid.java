package org.helioviewer.jhv.layers.grid;

import java.util.Arrays;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.FastFormat;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

// Radius rings and angular spokes for the disk display modes, drawn in the
// isotropic disk world coordinates (center at the origin, rim at radius 0.5)
public final class DiskGrid {

    private static final int SUBDIVISIONS = 180;
    private static final int MAX_RINGS = 16;
    private static final double MIN_RING_SPACING = 0.04; // fraction of the disk radius
    private static final double[] RING_FACTORS = {1, 2, 5};

    private final GLSLLine line = new GLSLLine(false);

    public void init() {
        line.init();
    }

    public void dispose() {
        line.dispose();
    }

    // spokeStep is the angular spacing of the radial spokes in degrees (the grid layer's
    // longitude step; the latitude/coordinate-system controls do not map to a radial grid).
    public void render(MapView mv, Viewport vp, boolean showLabels, double spokeStep, byte[] color, double lineScale, double labelAlpha, double labelSize, double labelAngle) {
        MapScale scale = mv.scale(vp);
        double[] rings = chooseRings(scale);
        updateLine(scale, rings, spokeStep, color);
        line.renderLine(vp, GridMath.LINEWIDTH * lineScale);
        if (showLabels)
            drawLabels(mv, vp, scale, rings, labelAlpha, labelSize, labelAngle);
    }

    // Ring radii at 1-2-5 decade steps within the radial range, decimated for legibility
    private static double[] chooseRings(MapScale scale) {
        double rMin = scale.getInterpolatedYValue(0);
        double rMax = scale.getInterpolatedYValue(1);
        double[] rings = new double[MAX_RINGS];
        int count = 0;
        double lastT = -1;
        double decade = Math.pow(10, Math.floor(Math.log10(Math.max(rMin, 1e-3 * rMax))));
        while (decade <= rMax && count < MAX_RINGS) {
            for (double factor : RING_FACTORS) {
                double r = factor * decade;
                if (r < rMin || r > rMax || count == MAX_RINGS)
                    continue;
                double t = scale.getYValueInv(r) + .5;
                if (t - lastT < MIN_RING_SPACING)
                    continue;
                rings[count++] = r;
                lastT = t;
            }
            decade *= 10;
        }
        return Arrays.copyOf(rings, count);
    }

    private static float ringRho(MapScale scale, double r) {
        return (float) (.5 * (scale.getYValueInv(r) + .5));
    }

    private void updateLine(MapScale scale, double[] rings, double spokeStep, byte[] color) {
        int spokes = (int) Math.round(360 / spokeStep);
        int noPoints = rings.length * (SUBDIVISIONS + 3) + 4 * spokes;
        BufVertex vexBuf = new BufVertex(noPoints * GLSLLine.stride);

        for (double r : rings) {
            float rho = ringRho(scale, r);
            for (int j = 0; j <= SUBDIVISIONS; j++) {
                double a = 2 * Math.PI * j / SUBDIVISIONS;
                float x = (float) (rho * Math.cos(a));
                float y = (float) (rho * Math.sin(a));
                if (j == 0)
                    vexBuf.putVertex(x, y, 0, 1, Colors.Null);
                vexBuf.putVertex(x, y, 0, 1, color);
                if (j == SUBDIVISIONS)
                    vexBuf.putVertex(x, y, 0, 1, Colors.Null);
            }
        }

        for (int s = 0; s < spokes; s++) {
            double a = Math.toRadians(s * spokeStep);
            float x = (float) (.5 * -Math.sin(a));
            float y = (float) (.5 * Math.cos(a));
            vexBuf.putVertex(0, 0, 0, 1, Colors.Null);
            vexBuf.repeatVertex(color);
            vexBuf.putVertex(x, y, 0, 1, color);
            vexBuf.repeatVertex(Colors.Null);
        }
        line.setVertex(vexBuf);
    }

    private static void drawLabels(MapView mv, Viewport vp, MapScale scale, double[] rings, double labelAlpha, double labelSize, double labelAngle) {
        SdfTextRenderer renderer = GLText.renderer();
        // Size the labels as a fraction of the camera FOV (world units), not a fixed pixel count:
        // this keeps a constant on-screen size at any zoom AND makes the size invariant to the
        // render resolution, so exporting a movie at a higher resolution keeps the same relative
        // label size (the old pixel-based form scaled by the screen's stale pixelScale / vp.height).
        // labelSize reads as ~pixels per 1000 px of frame width.
        double width = mv.cameraWidth(vp);
        double worldTextHeight = labelSize * width / 1000.;
        float textScaleFactor = (float) (worldTextHeight / renderer.getFontSize());
        float labelOffset = (float) (0.1 * worldTextHeight);

        // Place the label column along a spoke at labelAngle (clockwise from straight up); the text
        // stays upright. labelAngle = 0 reproduces the original vertical column up the +y axis.
        double th = Math.toRadians(labelAngle);
        double sin = Math.sin(th);
        double cos = Math.cos(th);

        // White labels at the label opacity (premultiplied)
        float a = (float) labelAlpha;
        renderer.setColor(new float[]{a, a, a, a});
        renderer.begin3DRendering();
        for (double r : rings) {
            double rho = ringRho(scale, r);
            renderer.draw(FastFormat.rounded2(r), (float) (sin * rho + labelOffset), (float) (cos * rho + labelOffset), 0, textScaleFactor);
        }
        renderer.end3DRendering();
    }

}
