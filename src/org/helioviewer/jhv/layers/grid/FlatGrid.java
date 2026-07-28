package org.helioviewer.jhv.layers.grid;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.GridLayer;
import org.helioviewer.jhv.math.FastFormat;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.RasterLine;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

public class FlatGrid {

    //private static final float TEXT_SCALE = GridLabel.textScale; // scalable text
    private static final int TEXT_SIZE = 12;
    private static final double BASE_THICKNESS_PIXELS = 1.5;
    private static final double[] ANGULAR_STEPS = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 15, 30, 45, 90, 180};
    private static final double[] LINEAR_STEP_FACTORS = {1, 2, 5, 10};
    private static final double TARGET_GRID_PIXELS = 12 * TEXT_SIZE;
    private static final byte[] AXIS_COLOR = Colors.Green;
    private static final double AXIS_EPSILON = 1e-9;

    private final GLSLShape shape = new GLSLShape(true);
    private final BufVertex vexBuf = new BufVertex(0);
    private final Axis xAxis = new Axis();
    private final Axis yAxis = new Axis();

    private static final class Axis {
        private double first;
        private double step;
        private int count;

        private double value(int i) {
            return first + i * step;
        }

        private double position(MapScale scale, boolean horizontal, int i) {
            double value = value(i);
            double position = (horizontal ? scale.toUnitX(value) : scale.toUnitY(value)) - 0.5;
            return Math.abs(position) < AXIS_EPSILON ? 0 : position;
        }
    }

    public void init() {
        shape.init();
    }

    public void dispose() {
        shape.dispose();
    }

    public void render(MapView mv, Viewport vp, boolean showLabels, byte[] gridColor, double lineScale, float[] labelColor, double labelSize) {
        double width = mv.cameraWidth(vp);
        MapScale scale = mv.scale(vp);
        double xCenter = 0.5 - mv.cameraTranslationX() / vp.aspect;
        double yCenter = 0.5 - mv.cameraTranslationY();
        double halfWidth = 0.5 * width;
        double x0 = scale.toMapX(Math.clamp(xCenter - halfWidth, 0, 1));
        double x1 = scale.toMapX(Math.clamp(xCenter + halfWidth, 0, 1));
        double y0 = scale.toMapY(Math.clamp(yCenter - halfWidth, 0, 1));
        double y1 = scale.toMapY(Math.clamp(yCenter + halfWidth, 0, 1));
        updateAxis(xAxis, true, x0, x1, vp.width);
        updateAxis(yAxis, mv.isHpc() || mv.isLatitudinal(), y0, y1, vp.height);
        updateShape(scale, mv, vp, width, gridColor, lineScale);
        shape.renderShape(GL.TRIANGLES);
        if (showLabels)
            drawLabels(scale, mv, vp, width, labelColor, labelSize);
    }

    private void drawLabels(MapScale scale, MapView mv, Viewport vp, double width, float[] labelColor, double labelSize) {
        SdfTextRenderer renderer = GLText.renderer();
        //float textScaleFactor = 0.3f * TEXT_SCALE / renderer.getFontSize(); // scalable text
        double worldTextHeight = TEXT_SIZE * labelSize / GridLayer.GRID_LABEL_SIZE_REF * Display.pixelScale[1] * Math.min(width, 1) / vp.height;
        float textScaleFactor = (float) (worldTextHeight / renderer.getFontSize());
        float labelOffset = (float) (0.1 * worldTextHeight);

        renderer.setColor(labelColor);
        renderer.begin3DRendering();
        for (int i = 0; i < xAxis.count; i++) {
            double position = xAxis.position(scale, true, i);
            if (position == 0)
                continue;
            double x = RasterLine.snapVertical(vp, width, mv.cameraTranslationX(), position);
            double value = xAxis.value(i);
            if (mv.isLatitudinal())
                value = mv.gridType().displayLongitude(value);
            renderer.draw(FastFormat.rounded2(value), (float) (vp.aspect * x), labelOffset, 0, textScaleFactor);
        }
        for (int i = 0; i < yAxis.count; i++) {
            double position = yAxis.position(scale, false, i);
            double y = RasterLine.snapHorizontal(vp, width, mv.cameraTranslationY(), position);
            renderer.draw(FastFormat.rounded2(yAxis.value(i)), 0, (float) y + labelOffset, 0, textScaleFactor);
        }
        renderer.end3DRendering();
    }

    private void updateShape(MapScale scale, MapView mv, Viewport vp, double width, byte[] gridColor, double lineScale) {
        double thickness = BASE_THICKNESS_PIXELS * lineScale;
        for (int i = 0; i < xAxis.count; i++) {
            double position = xAxis.position(scale, true, i);
            byte[] color = position == 0 ? AXIS_COLOR : gridColor;
            RasterLine.putVertical(vp, width, mv.cameraTranslationX(), vp.aspect * position, -0.5, 0.5, thickness, color, vexBuf);
        }
        for (int i = 0; i < yAxis.count; i++) {
            double position = yAxis.position(scale, false, i);
            byte[] color = position == 0 ? AXIS_COLOR : gridColor;
            RasterLine.putHorizontal(vp, width, mv.cameraTranslationY(), -0.5 * vp.aspect, 0.5 * vp.aspect, position, thickness, color, vexBuf);
        }
        shape.setVertex(vexBuf);
    }

    private static void updateAxis(Axis axis, boolean angularStep, double start, double stop, int pixels) {
        double range = Math.abs(stop - start);
        double first = start;
        double step = 0;
        int count = 1;
        if (Double.isFinite(range) && range > Math.ulp(1.0)) {
            double targetDivisions = Math.max(1, pixels / TARGET_GRID_PIXELS);
            step = angularStep ? chooseAngularStep(range, targetDivisions) : chooseLinearStep(range, targetDivisions);
            first = Math.ceil(Math.min(start, stop) / step) * step;
            double last = Math.floor(Math.max(start, stop) / step) * step;
            count = (int) Math.max(0, Math.floor((last - first) / step) + 1);
        }

        axis.first = first;
        axis.step = step;
        axis.count = count;
    }

    private static double chooseAngularStep(double range, double targetDivisions) {
        double target = range / targetDivisions;
        for (double step : ANGULAR_STEPS) {
            if (step >= target)
                return step;
        }
        return ANGULAR_STEPS[ANGULAR_STEPS.length - 1];
    }

    private static double chooseLinearStep(double range, double targetDivisions) {
        double target = range / targetDivisions;
        double base = Math.pow(10, Math.floor(Math.log10(target)));
        for (double factor : LINEAR_STEP_FACTORS) {
            double step = factor * base;
            if (step >= target)
                return step;
        }
        return 10 * base;
    }

}
