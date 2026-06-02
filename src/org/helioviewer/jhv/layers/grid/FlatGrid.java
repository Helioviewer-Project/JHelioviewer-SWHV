package org.helioviewer.jhv.layers.grid;

import java.text.DecimalFormat;
import java.util.Objects;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.RasterLine;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

public class FlatGrid {

    //private static final float TEXT_SCALE = GridLabel.textScale; // scalable text
    private static final int TEXT_SIZE = 12;
    private static final DecimalFormat FORMATTER = MathUtils.numberFormatter("0", 2);
    private static final double THICKNESS_PIXELS = 1.5;
    private static final double[] ANGULAR_STEPS = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 15, 30, 45, 90, 180};
    private static final double[] LINEAR_STEP_FACTORS = {1, 2, 5, 10};
    private static final double TARGET_GRID_PIXELS = 12 * TEXT_SIZE;
    private static final byte[] GRID_COLOR = Colors.Red;
    private static final byte[] AXIS_COLOR = Colors.Green;

    private final GLSLShape shape = new GLSLShape(false);

    private FlatGridKey previousKey;

    private Axis xAxis = Axis.EMPTY;
    private Axis yAxis = Axis.EMPTY;

    // Step and inclusive bounds of one flat axis.
    private record AxisSignature(double step, double first, double last) {}

    // Built axis labels, zero-axis flags, and positions.
    private record Axis(AxisSignature signature, String[] labels, boolean[] axisFlags, double[] positions) {
        private static final Axis EMPTY = new Axis(null, new String[0], new boolean[0], new double[0]);
    }

    // Projection and camera state that invalidates the cached flat grid.
    private record FlatGridKey(MapMode mode, GridType gridType, double aspect, double cameraWidth,
                               double translationX, double translationY) {}

    public void init() {
        shape.init();
    }

    public void dispose() {
        shape.dispose();
    }

    public void render(MapView mv, Viewport vp, boolean showLabels) {
        double width = mv.cameraWidth(vp);
        rebuildIfNeeded(mv, vp, width);
        shape.renderShape(GL.TRIANGLES);
        if (showLabels)
            drawLabels(mv, vp, width);
    }

    private static FlatGridKey key(MapView mv, Viewport vp, double width) {
        return new FlatGridKey(mv.mode(), mv.gridType(), vp.aspect, width, mv.cameraTranslationX(), mv.cameraTranslationY());
    }

    private void rebuildIfNeeded(MapView mv, Viewport vp, double width) {
        FlatGridKey flatGridKey = key(mv, vp, width);

        MapScale scale = mv.scale(vp);
        double xCenter = 0.5 - mv.cameraTranslationX() / vp.aspect;
        double yCenter = 0.5 - mv.cameraTranslationY();
        double halfWidth = 0.5 * width;
        AxisSignature xSignature = buildAxisSignature(true,
                scale.getInterpolatedXValue(Math.clamp(xCenter - halfWidth, 0, 1)),
                scale.getInterpolatedXValue(Math.clamp(xCenter + halfWidth, 0, 1)), vp.width);
        AxisSignature ySignature = buildAxisSignature(mv.isHpc() || mv.isLatitudinal(),
                scale.getInterpolatedYValue(Math.clamp(yCenter - halfWidth, 0, 1)),
                scale.getInterpolatedYValue(Math.clamp(yCenter + halfWidth, 0, 1)), vp.height);
        if (!needsRebuild(flatGridKey, xSignature, ySignature))
            return;

        boolean wrap0to360 = mv.gridType() == GridType.Carrington;
        xAxis = buildAxis(scale, true, wrap0to360, xSignature);
        yAxis = buildAxis(scale, false, false, ySignature);
        rebuildShape(mv, vp, width);

        previousKey = flatGridKey;
    }

    private boolean needsRebuild(FlatGridKey flatGridKey, AxisSignature xSignature, AxisSignature ySignature) {
        return !Objects.equals(previousKey, flatGridKey) ||
                !Objects.equals(xAxis.signature(), xSignature) ||
                !Objects.equals(yAxis.signature(), ySignature);
    }

    private void drawLabels(MapView mv, Viewport vp, double width) {
        SdfTextRenderer renderer = GLText.renderer();
        //float textScaleFactor = 0.3f * TEXT_SCALE / renderer.getFontSize(); // scalable text
        double worldTextHeight = TEXT_SIZE * Display.pixelScale[1] * Math.min(width, 1) / vp.height;
        float textScaleFactor = (float) (worldTextHeight / renderer.getFontSize());
        float labelOffset = (float) (0.1 * worldTextHeight);

        renderer.setColor(Colors.WhiteFloat);
        renderer.begin3DRendering();
        for (int i = 0; i < xAxis.labels().length; i++) {
            if (xAxis.axisFlags()[i])
                continue;
            double x = RasterLine.snapVertical(vp, width, mv.cameraTranslationX(), xAxis.positions()[i]);
            renderer.draw(xAxis.labels()[i], (float) (vp.aspect * x), labelOffset, 0, textScaleFactor);
        }
        for (int i = 0; i < yAxis.labels().length; i++) {
            double y = RasterLine.snapHorizontal(vp, width, mv.cameraTranslationY(), yAxis.positions()[i]);
            renderer.draw(yAxis.labels()[i], 0, (float) y + labelOffset, 0, textScaleFactor);
        }
        renderer.end3DRendering();
    }

    private void rebuildShape(MapView mv, Viewport vp, double width) {
        int noPoints = RasterLine.vertexCount(xAxis.positions().length + yAxis.positions().length);
        BufVertex vexBuf = new BufVertex(noPoints * GLSLShape.stride);

        for (int i = 0; i < xAxis.positions().length; i++) {
            byte[] color = xAxis.axisFlags()[i] ? AXIS_COLOR : GRID_COLOR;
            RasterLine.putVertical(vp, width, mv.cameraTranslationX(), vp.aspect * xAxis.positions()[i], -0.5, 0.5, THICKNESS_PIXELS, color, vexBuf);
        }
        for (int i = 0; i < yAxis.positions().length; i++) {
            byte[] color = yAxis.axisFlags()[i] ? AXIS_COLOR : GRID_COLOR;
            RasterLine.putHorizontal(vp, width, mv.cameraTranslationY(), -0.5 * vp.aspect, 0.5 * vp.aspect, yAxis.positions()[i], THICKNESS_PIXELS, color, vexBuf);
        }
        shape.setVertex(vexBuf);
    }

    private static AxisSignature buildAxisSignature(boolean angular, double start, double stop, int pixels) {
        double range = Math.abs(stop - start);
        if (!Double.isFinite(range) || range <= Math.ulp(1.0))
            return new AxisSignature(0, start, start);

        double targetDivisions = Math.max(1, pixels / TARGET_GRID_PIXELS);
        double step = angular ? chooseAngularStep(range, targetDivisions) : chooseLinearStep(range, targetDivisions);
        double lo = Math.min(start, stop);
        double hi = Math.max(start, stop);
        double first = Math.ceil(lo / step) * step;
        double last = Math.floor(hi / step) * step;
        return new AxisSignature(step, first, last);
    }

    private static Axis buildAxis(MapScale scale, boolean horizontal, boolean wrap0to360, AxisSignature signature) {
        String[] labels;
        double[] positions;
        boolean[] axisFlags;
        if (signature.step() == 0) {
            double value = signature.first();
            labels = new String[]{FORMATTER.format(wrap0to360 ? wrapCarrington(value) : value)};
            positions = new double[]{horizontal ? scale.getXValueInv(value) : scale.getYValueInv(value)};
            axisFlags = new boolean[]{Math.abs(positions[0]) < 1e-9};
        } else {
            double step = signature.step();
            double first = signature.first();
            int count = (int) Math.max(0, Math.floor((signature.last() - first) / step) + 1);
            labels = new String[count];
            positions = new double[count];
            axisFlags = new boolean[count];
            for (int i = 0; i < count; i++) {
                double value = first + i * step;
                labels[i] = FORMATTER.format(wrap0to360 ? wrapCarrington(value) : value);
                positions[i] = horizontal ? scale.getXValueInv(value) : scale.getYValueInv(value);
                axisFlags[i] = Math.abs(positions[i]) < 1e-9;
            }
        }
        return new Axis(signature, labels, axisFlags, positions);
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
        if (!Double.isFinite(range) || range <= Math.ulp(1.0))
            return 1;
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
