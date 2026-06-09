package org.helioviewer.jhv.layers.grid;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.Viewport;
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
    private static final double THICKNESS_PIXELS = 1.5;
    private static final double[] ANGULAR_STEPS = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 15, 30, 45, 90, 180};
    private static final double[] LINEAR_STEP_FACTORS = {1, 2, 5, 10};
    private static final double TARGET_GRID_PIXELS = 12 * TEXT_SIZE;
    private static final byte[] GRID_COLOR = Colors.Red;
    private static final byte[] AXIS_COLOR = Colors.Green;
    private static final double AXIS_EPSILON = 1e-9;

    private final GLSLShape shape = new GLSLShape(false);

    private record Axis(double[] labelValues, double[] positions) {}

    public void init() {
        shape.init();
    }

    public void dispose() {
        shape.dispose();
    }

    public void render(MapView mv, Viewport vp, boolean showLabels) {
        double width = mv.cameraWidth(vp);
        MapScale scale = mv.scale(vp);
        double xCenter = 0.5 - mv.cameraTranslationX() / vp.aspect;
        double yCenter = 0.5 - mv.cameraTranslationY();
        double halfWidth = 0.5 * width;
        double x0 = scale.getInterpolatedXValue(Math.clamp(xCenter - halfWidth, 0, 1));
        double x1 = scale.getInterpolatedXValue(Math.clamp(xCenter + halfWidth, 0, 1));
        double y0 = scale.getInterpolatedYValue(Math.clamp(yCenter - halfWidth, 0, 1));
        double y1 = scale.getInterpolatedYValue(Math.clamp(yCenter + halfWidth, 0, 1));
        Axis xAxis = buildAxis(scale, true, true, mv.gridType() == GridType.Carrington,
                x0, x1, vp.width);
        Axis yAxis = buildAxis(scale, false, mv.isHpc() || mv.isLatitudinal(), false,
                y0, y1, vp.height);
        updateShape(xAxis, yAxis, mv, vp, width);
        shape.renderShape(GL.TRIANGLES);
        if (showLabels)
            drawLabels(xAxis, yAxis, mv, vp, width);
    }

    private static void drawLabels(Axis xAxis, Axis yAxis, MapView mv, Viewport vp, double width) {
        SdfTextRenderer renderer = GLText.renderer();
        //float textScaleFactor = 0.3f * TEXT_SCALE / renderer.getFontSize(); // scalable text
        double worldTextHeight = TEXT_SIZE * Display.pixelScale[1] * Math.min(width, 1) / vp.height;
        float textScaleFactor = (float) (worldTextHeight / renderer.getFontSize());
        float labelOffset = (float) (0.1 * worldTextHeight);

        renderer.setColor(Colors.WhiteFloat);
        renderer.begin3DRendering();
        for (int i = 0; i < xAxis.labelValues().length; i++) {
            if (xAxis.positions()[i] == 0)
                continue;
            double x = RasterLine.snapVertical(vp, width, mv.cameraTranslationX(), xAxis.positions()[i]);
            renderer.draw(FastFormat.rounded2(xAxis.labelValues()[i]), (float) (vp.aspect * x), labelOffset, 0, textScaleFactor);
        }
        for (int i = 0; i < yAxis.labelValues().length; i++) {
            double y = RasterLine.snapHorizontal(vp, width, mv.cameraTranslationY(), yAxis.positions()[i]);
            renderer.draw(FastFormat.rounded2(yAxis.labelValues()[i]), 0, (float) y + labelOffset, 0, textScaleFactor);
        }
        renderer.end3DRendering();
    }

    private void updateShape(Axis xAxis, Axis yAxis, MapView mv, Viewport vp, double width) {
        int noPoints = RasterLine.vertexCount(xAxis.positions().length + yAxis.positions().length);
        BufVertex vexBuf = new BufVertex(noPoints * GLSLShape.stride);

        for (int i = 0; i < xAxis.positions().length; i++) {
            byte[] color = xAxis.positions()[i] == 0 ? AXIS_COLOR : GRID_COLOR;
            RasterLine.putVertical(vp, width, mv.cameraTranslationX(), vp.aspect * xAxis.positions()[i], -0.5, 0.5, THICKNESS_PIXELS, color, vexBuf);
        }
        for (int i = 0; i < yAxis.positions().length; i++) {
            byte[] color = yAxis.positions()[i] == 0 ? AXIS_COLOR : GRID_COLOR;
            RasterLine.putHorizontal(vp, width, mv.cameraTranslationY(), -0.5 * vp.aspect, 0.5 * vp.aspect, yAxis.positions()[i], THICKNESS_PIXELS, color, vexBuf);
        }
        shape.setVertex(vexBuf);
    }

    private static Axis buildAxis(MapScale scale, boolean xAxis, boolean angularStep, boolean wrap0to360, double start, double stop, int pixels) {
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

        double[] labelValues = new double[count];
        double[] positions = new double[count];
        for (int i = 0; i < count; i++) {
            double value = first + i * step;
            labelValues[i] = wrap0to360 ? wrapCarrington(value) : value;
            double position = xAxis ? scale.getXValueInv(value) : scale.getYValueInv(value);
            positions[i] = Math.abs(position) < AXIS_EPSILON ? 0 : position;
        }
        return new Axis(labelValues, positions);
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

    private static double wrapCarrington(double value) {
        double wrapped = value % 360;
        return wrapped < 0 ? wrapped + 360 : wrapped;
    }
}
