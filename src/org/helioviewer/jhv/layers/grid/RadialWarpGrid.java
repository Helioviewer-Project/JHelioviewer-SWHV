package org.helioviewer.jhv.layers.grid;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.GridLayer;
import org.helioviewer.jhv.math.FastFormat;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

// Radius rings and angular spokes in RadialWarp's circular output coordinates.
public final class RadialWarpGrid {

    private static final int TEXT_SIZE = 12;
    private static final int SUBDIVISIONS = 180;
    private static final int MAX_RINGS = 16;
    private static final double MIN_RING_SPACING = 0.04;
    private static final double[] RING_FACTORS = {1, 2, 5};

    private final GLSLLine line = new GLSLLine(false);
    private final double[] rings = new double[MAX_RINGS];

    public void init() {
        line.init();
    }

    public void dispose() {
        line.dispose();
    }

    public void render(MapView mv, Viewport vp, boolean showLabels, double spokeStep, byte[] color, double lineScale, float[] labelColor, double labelSize, double labelAngle) {
        MapScale scale = mv.scale(vp);
        int ringCount = chooseRings(scale);
        updateLine(scale, ringCount, spokeStep, color);
        line.renderLine(vp, GridMath.LINEWIDTH * lineScale);
        if (showLabels)
            drawLabels(mv, vp, scale, rings, ringCount, labelColor, labelSize, labelAngle);
    }

    private int chooseRings(MapScale scale) {
        double rMin = scale.getInterpolatedYValue(0);
        double rMax = scale.getInterpolatedYValue(1);
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
        return count;
    }

    private static float ringRho(MapScale scale, double r) {
        return (float) (.5 * (scale.getYValueInv(r) + .5));
    }

    private void updateLine(MapScale scale, int ringCount, double spokeStep, byte[] color) {
        int spokes = (int) Math.round(360 / spokeStep);
        int noPoints = ringCount * (SUBDIVISIONS + 3) + 4 * spokes;
        BufVertex vexBuf = new BufVertex(noPoints * GLSLLine.stride);

        for (int i = 0; i < ringCount; i++) {
            double r = rings[i];
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

    private static void drawLabels(MapView mv, Viewport vp, MapScale scale, double[] rings, int ringCount, float[] color, double labelSize, double labelAngle) {
        SdfTextRenderer renderer = GLText.renderer();
        double width = mv.cameraWidth(vp);
        double worldTextHeight = TEXT_SIZE * labelSize / GridLayer.GRID_LABEL_SIZE_REF * Display.pixelScale[1] * width / vp.height;
        float textScaleFactor = (float) (worldTextHeight / renderer.getFontSize());
        float labelOffset = (float) (0.1 * worldTextHeight);
        double angle = Math.toRadians(labelAngle);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        renderer.setColor(color);
        renderer.begin3DRendering();
        for (int i = 0; i < ringCount; i++) {
            double r = rings[i];
            double rho = ringRho(scale, r);
            renderer.draw(FastFormat.rounded2(r), (float) (sin * rho + labelOffset), (float) (cos * rho + labelOffset), 0, textScaleFactor);
        }
        renderer.end3DRendering();
    }

}
