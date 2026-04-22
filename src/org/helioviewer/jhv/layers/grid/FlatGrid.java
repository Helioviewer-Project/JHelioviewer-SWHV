package org.helioviewer.jhv.layers.grid;

import java.text.DecimalFormat;
import java.util.Objects;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridScale;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.RasterLine;
import org.helioviewer.jhv.opengl.text.TextRenderer;

public class FlatGrid {

    private static final float TEXT_SCALE = GridLabel.textScale;
    private static final DecimalFormat FORMATTER = MathUtils.numberFormatter("0", 2);
    private static final double THICKNESS_PIXELS = 1.5;
    private static final double[] ANGULAR_STEPS = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 15, 30, 45, 90, 180};
    private static final double[] LINEAR_STEP_FACTORS = {1, 2, 5, 10};
    private static final double TARGET_DIVISIONS = 8;
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
    private record FlatGridKey(ProjectionMode mode, GridType gridType, double aspect, double cameraWidth,
                               double translationX, double translationY) {}

    public void init() {
        shape.init();
    }

    public void dispose() {
        shape.dispose();
    }

    public void render(Camera camera, Viewport vp, boolean showLabels) {
        rebuildIfNeeded(camera, vp);
        shape.renderShape(GL.TRIANGLES);
        if (showLabels) {
            int labelSize = (int) (TEXT_SCALE * CameraHelper.getPixelFactor(camera, vp));
            drawLabels(camera, labelSize, vp);
        }
    }

    private static FlatGridKey key(Camera camera, Viewport vp) {
        return new FlatGridKey(Display.mode, Display.gridType, vp.aspect, camera.getCameraWidth(), camera.getTranslationX(), camera.getTranslationY());
    }

    private void rebuildIfNeeded(Camera camera, Viewport vp) {
        FlatGridKey flatGridKey = key(camera, vp);
        GridScale scale = Display.mode.scale;
        AxisSignature xSignature = buildAxisSignature(true, scale.getInterpolatedXValue(0), scale.getInterpolatedXValue(1));
        AxisSignature ySignature = buildAxisSignature(Display.mode.isHpc() || Display.mode.isLatitudinal(), scale.getInterpolatedYValue(0), scale.getInterpolatedYValue(1));
        if (!needsRebuild(flatGridKey, xSignature, ySignature))
            return;

        boolean wrap0to360 = Display.gridType == GridType.Carrington;
        xAxis = buildAxis(scale, true, wrap0to360, xSignature);
        yAxis = buildAxis(scale, false, false, ySignature);
        rebuildShape(camera, vp);

        previousKey = flatGridKey;
    }

    private boolean needsRebuild(FlatGridKey flatGridKey, AxisSignature xSignature, AxisSignature ySignature) {
        return !Objects.equals(previousKey, flatGridKey) ||
                !Objects.equals(xAxis.signature(), xSignature) ||
                !Objects.equals(yAxis.signature(), ySignature);
    }

    private void drawLabels(Camera camera, int size, Viewport vp) {
        TextRenderer renderer = GLText.getRenderer(size);
        float textScaleFactor = 0.3f * TEXT_SCALE / renderer.getFontSize();
        renderer.setColor(Colors.WhiteFloat);
        renderer.begin3DRendering();
        for (int i = 0; i < xAxis.labels().length; i++) {
            if (xAxis.axisFlags()[i])
                continue;
            double x = RasterLine.snapVertical(camera, vp, xAxis.positions()[i]);
            renderer.draw(xAxis.labels()[i], (float) (vp.aspect * x), 0, 0, textScaleFactor);
        }
        for (int i = 0; i < yAxis.labels().length; i++) {
            renderer.draw(yAxis.labels()[i], 0, (float) RasterLine.snapHorizontal(camera, vp, yAxis.positions()[i]), 0, textScaleFactor);
        }
        renderer.end3DRendering();
    }

    private void rebuildShape(Camera camera, Viewport vp) {
        int noPoints = RasterLine.vertexCount(xAxis.positions().length + yAxis.positions().length);
        BufVertex vexBuf = new BufVertex(noPoints * GLSLShape.stride);

        for (int i = 0; i < xAxis.positions().length; i++) {
            byte[] color = xAxis.axisFlags()[i] ? AXIS_COLOR : GRID_COLOR;
            RasterLine.putVertical(camera, vp, vp.aspect * xAxis.positions()[i], -0.5, 0.5, THICKNESS_PIXELS, color, vexBuf);
        }
        for (int i = 0; i < yAxis.positions().length; i++) {
            byte[] color = yAxis.axisFlags()[i] ? AXIS_COLOR : GRID_COLOR;
            RasterLine.putHorizontal(camera, vp, -0.5 * vp.aspect, 0.5 * vp.aspect, yAxis.positions()[i], THICKNESS_PIXELS, color, vexBuf);
        }
        shape.setVertex(vexBuf);
    }

    private static AxisSignature buildAxisSignature(boolean angular, double start, double stop) {
        double range = Math.abs(stop - start);
        if (!Double.isFinite(range) || range <= Math.ulp(1.0))
            return new AxisSignature(0, start, start);

        double step = angular ? chooseAngularStep(range) : chooseLinearStep(range);
        double lo = Math.min(start, stop);
        double hi = Math.max(start, stop);
        double first = Math.ceil(lo / step) * step;
        double last = Math.floor(hi / step) * step;
        return new AxisSignature(step, first, last);
    }

    private static Axis buildAxis(GridScale scale, boolean horizontal, boolean wrap0to360, AxisSignature signature) {
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

    private static double chooseAngularStep(double range) {
        double target = range / TARGET_DIVISIONS;
        for (double step : ANGULAR_STEPS) {
            if (step >= target)
                return step;
        }
        return ANGULAR_STEPS[ANGULAR_STEPS.length - 1];
    }

    private static double chooseLinearStep(double range) {
        if (!Double.isFinite(range) || range <= Math.ulp(1.0))
            return 1;
        double base = Math.pow(10, Math.floor(Math.log10(range / TARGET_DIVISIONS)));
        for (double factor : LINEAR_STEP_FACTORS) {
            double step = factor * base;
            if (step >= range / TARGET_DIVISIONS)
                return step;
        }
        return 10 * base;
    }

    private static double wrapCarrington(double value) {
        double wrapped = value % 360;
        return wrapped < 0 ? wrapped + 360 : wrapped;
    }
}
