package org.helioviewer.jhv.layers.grid;

import java.util.Arrays;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
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

    private static final int TEXT_SIZE = 22;
    private static final int SUBDIVISIONS = 180;
    private static final int MAX_RINGS = 16;
    private static final double MIN_RING_SPACING = 0.04; // fraction of the disk radius
    private static final double[] RING_FACTORS = {1, 2, 5};
    private static final byte[] gridColor = Colors.Red;

    private final GLSLLine line = new GLSLLine(false);

    public void init() {
        line.init();
    }

    public void dispose() {
        line.dispose();
    }

    // spokeStep is the angular spacing of the radial spokes in degrees (the grid layer's
    // longitude step; the latitude/coordinate-system controls do not map to a radial grid).
    public void render(MapView mv, Viewport vp, boolean showLabels, double spokeStep) {
        MapScale scale = mv.scale(vp);
        double[] rings = chooseRings(scale);
        updateLine(scale, rings, spokeStep);
        line.renderLine(vp, GridMath.LINEWIDTH);
        if (showLabels)
            drawLabels(mv, vp, scale, rings);
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

    private void updateLine(MapScale scale, double[] rings, double spokeStep) {
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
                vexBuf.putVertex(x, y, 0, 1, gridColor);
                if (j == SUBDIVISIONS)
                    vexBuf.putVertex(x, y, 0, 1, Colors.Null);
            }
        }

        for (int s = 0; s < spokes; s++) {
            double a = Math.toRadians(s * spokeStep);
            float x = (float) (.5 * -Math.sin(a));
            float y = (float) (.5 * Math.cos(a));
            vexBuf.putVertex(0, 0, 0, 1, Colors.Null);
            vexBuf.repeatVertex(gridColor);
            vexBuf.putVertex(x, y, 0, 1, gridColor);
            vexBuf.repeatVertex(Colors.Null);
        }
        line.setVertex(vexBuf);
    }

    private static void drawLabels(MapView mv, Viewport vp, MapScale scale, double[] rings) {
        SdfTextRenderer renderer = GLText.renderer();
        // Scale to the camera width (not min(width, 1)) so the labels keep a constant on-screen
        // size at any zoom; the disk view normally spans several R_sun, where the cap shrank them.
        double width = mv.cameraWidth(vp);
        double worldTextHeight = TEXT_SIZE * Display.pixelScale[1] * width / vp.height;
        float textScaleFactor = (float) (worldTextHeight / renderer.getFontSize());
        float labelOffset = (float) (0.1 * worldTextHeight);

        renderer.setColor(Colors.WhiteFloat);
        renderer.begin3DRendering();
        for (double r : rings) {
            renderer.draw(FastFormat.rounded2(r), labelOffset, ringRho(scale, r) + labelOffset, 0, textScaleFactor);
        }
        renderer.end3DRendering();
    }

}
