package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridScale;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.grid.GridLabel;
import org.helioviewer.jhv.layers.grid.GridMath;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class GridLayer extends AbstractLayer {

    private static final double RADIAL_UNIT = Sun.Radius;
    private static final double RADIAL_STEP = 15;
    private static final double RADIAL_UNIT_FAR = Sun.MeanEarthDistance / 10;
    private static final double RADIAL_STEP_FAR = 45;
    private static final float[] R_LABEL_POS = {(float) (2 * RADIAL_UNIT), (float) (8 * RADIAL_UNIT), (float) (24 * RADIAL_UNIT)};
    private static final float[] R_LABEL_POS_FAR = {(float) (2 * RADIAL_UNIT_FAR), (float) (8 * RADIAL_UNIT_FAR), (float) (24 * RADIAL_UNIT_FAR)};

    // height of text in solar radii
    private static final float textScale = GridLabel.textScale;
    private static final double LINEWIDTH = GridMath.LINEWIDTH;
    private static final double LINEWIDTH_THICK = 2 * LINEWIDTH;
    private static final double LINEWIDTH_EARTH = LINEWIDTH;
    private static final double LINEWIDTH_AXES = 2 * LINEWIDTH;
    // private static final double PLANETEXT_Z = 0.01;

    private static final DecimalFormat formatter2 = MathUtils.numberFormatter("0", 2);

    private double lonStep = 15;
    private double latStep = 20;
    private boolean gridNeedsInit = true;

    private boolean showAxis = true;
    private boolean showLabels = true;
    private boolean showRadial = false;

    private final GLSLShape earthPoint = new GLSLShape(false);
    private final GLSLLine axesLine = new GLSLLine(false);
    private final GLSLLine earthCircleLine = new GLSLLine(false);
    private final GLSLLine radialCircleLine = new GLSLLine(false);
    private final GLSLLine radialThickLine = new GLSLLine(false);
    private final GLSLLine radialCircleLineFar = new GLSLLine(false);
    private final GLSLLine radialThickLineFar = new GLSLLine(false);
    private final GLSLLine flatLine = new GLSLLine(false);
    private final GLSLLine gridLine = new GLSLLine(false);

    private List<GridLabel> latLabels;
    private List<GridLabel> lonLabels;
    private final List<GridLabel> radialLabels;
    private final List<GridLabel> radialLabelsFar;

    private final Component optionsPanel;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("lonStep", lonStep);
        jo.put("latStep", latStep);
        jo.put("showAxis", showAxis);
        jo.put("showLabels", showLabels);
        jo.put("showRadial", showRadial);
        jo.put("type", Display.gridType);
    }

    private void deserialize(JSONObject jo) {
        lonStep = jo.optDouble("lonStep", lonStep);
        latStep = jo.optDouble("latStep", latStep);
        showAxis = jo.optBoolean("showAxis", showAxis);
        showLabels = jo.optBoolean("showLabels", showLabels);
        showRadial = jo.optBoolean("showRadial", showRadial);

        String strGridType = jo.optString("type", Display.gridType.toString());
        try {
            Display.setGridType(GridType.valueOf(strGridType));
        } catch (Exception ignore) {
        }
    }

    public GridLayer(JSONObject jo) {
        if (jo != null)
            deserialize(jo);
        else
            setEnabled(true);
        optionsPanel = new GridLayerOptions();

        latLabels = GridLabel.makeLatLabels(latStep);
        lonLabels = GridLabel.makeLonLabels(Display.gridType, lonStep);
        radialLabels = GridLabel.makeRadialLabels(0, RADIAL_STEP);
        radialLabelsFar = GridLabel.makeRadialLabels(Math.PI / 2, RADIAL_STEP_FAR);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        if (gridNeedsInit) {
            GridMath.initGrid(gl, gridLine, lonStep, latStep);
            gridNeedsInit = false;
        }

        if (showAxis)
            axesLine.render(gl, vp.aspect, LINEWIDTH_AXES);

        Position viewpoint = camera.getViewpoint();
        float ztext = 0; //(float) (camera.getWidth() * PLANETEXT_Z);
        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        drawEarthCircles(gl, vp, pixFactor, Sun.getEarth(viewpoint.time));

        double pixelsPerSolarRadius = textScale * pixFactor;

        Transform.pushView();
        Transform.rotateViewInverse(Display.gridType.toCarrington(viewpoint));
        {
            gridLine.render(gl, vp.aspect, LINEWIDTH);
            if (showLabels) {
                drawGridText(gl, (int) pixelsPerSolarRadius, ztext);
            }
        }
        Transform.popView();

        if (showRadial) {
            Transform.pushView();
            Transform.rotateViewInverse(viewpoint.toQuat());
            {
                if (viewpoint.distance > 100 * Sun.MeanEarthDistance) {
                    radialCircleLineFar.render(gl, vp.aspect, LINEWIDTH);
                    radialThickLineFar.render(gl, vp.aspect, LINEWIDTH_THICK);
                    if (showLabels)
                        drawRadialGridText(gl, radialLabelsFar, pixelsPerSolarRadius * RADIAL_UNIT_FAR, ztext, R_LABEL_POS_FAR);
                } else {
                    radialCircleLine.render(gl, vp.aspect, LINEWIDTH);
                    radialThickLine.render(gl, vp.aspect, LINEWIDTH_THICK);
                    if (showLabels)
                        drawRadialGridText(gl, radialLabels, pixelsPerSolarRadius * RADIAL_UNIT, ztext, R_LABEL_POS);
                }
            }
            Transform.popView();
        }
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        int pixelsPerSolarRadius = (int) (textScale * CameraHelper.getPixelFactor(camera, vp));
        drawGridFlat(gl, vp);
        if (showLabels) {
            drawGridTextFlat(pixelsPerSolarRadius, Display.mode.scale, vp);
        }
    }

    private double previousAspect = -1;

    private void drawGridFlat(GL2 gl, Viewport vp) {
        if (previousAspect != vp.aspect) {
            GridMath.initFlatGrid(gl, flatLine, vp.aspect);
            previousAspect = vp.aspect;
        }
        flatLine.render(gl, vp.aspect, LINEWIDTH);
    }

    private void drawGridTextFlat(int size, GridScale scale, Viewport vp) {
        float w = (float) vp.aspect;
        float h = 1;
        JhvTextRenderer renderer = GLText.getRenderer(size);
        renderer.setColor(Colors.WhiteFloat);
        float textScaleFactor = textScale / renderer.getFont().getSize2D() * w / GridMath.FLAT_STEPS_THETA * 5;

        renderer.begin3DRendering();
        {
            for (int i = 0; i <= GridMath.FLAT_STEPS_THETA; i++) {
                if (i == GridMath.FLAT_STEPS_THETA / 2) {
                    continue;
                }
                double lon = scale.getInterpolatedXValue(1. / GridMath.FLAT_STEPS_THETA * i, Display.gridType);
                String txt = formatter2.format(lon);
                float x = i / (float) GridMath.FLAT_STEPS_THETA - 0.5f;
                renderer.draw3D(txt, w * x, 0, 0, textScaleFactor);
            }
            for (int i = 0; i <= GridMath.FLAT_STEPS_RADIAL; i++) {
                String txt = formatter2.format(scale.getInterpolatedYValue(1. / GridMath.FLAT_STEPS_RADIAL * i));
                float y = i / (float) GridMath.FLAT_STEPS_RADIAL - 0.5f;
                renderer.draw3D(txt, 0, h * y, 0, textScaleFactor);
            }
        }
        renderer.end3DRendering();
    }

    private void drawEarthCircles(GL2 gl, Viewport vp, double factor, Position p) {
        Transform.pushView();
        Transform.rotateViewInverse(p.toQuat());

        earthCircleLine.render(gl, vp.aspect, LINEWIDTH_EARTH);
        earthPoint.renderPoints(gl, factor);

        Transform.popView();
    }

    private static void drawRadialGridText(GL2 gl, List<GridLabel> labels, double size, float z, float[] labelPos) {
        float fuzz = 0.75f;
        gl.glDisable(GL2.GL_CULL_FACE);
        for (float rsize : labelPos) {
            JhvTextRenderer renderer = GLText.getRenderer((int) (fuzz * rsize * size));
            renderer.setColor(Colors.MiddleGrayFloat);
            float textScaleFactor = textScale / renderer.getFont().getSize2D();

            renderer.begin3DRendering();
            labels.forEach(label -> renderer.draw3D(label.txt, rsize * label.x, rsize * label.y, z, fuzz * rsize * textScaleFactor));
            renderer.end3DRendering();
        }
        gl.glEnable(GL2.GL_CULL_FACE);
    }

    private void drawGridText(GL2 gl, int size, float z) {
        JhvTextRenderer renderer = GLText.getRenderer(size);
        renderer.setColor(Colors.WhiteFloat);
        // the scale factor has to be divided by the current font size
        float textScaleFactor = textScale / renderer.getFont().getSize2D();

        renderer.begin3DRendering();

        gl.glDisable(GL2.GL_CULL_FACE);
        latLabels.forEach(label -> renderer.draw3D(label.txt, label.x, label.y, z, textScaleFactor));
        renderer.flush();
        gl.glEnable(GL2.GL_CULL_FACE);

        lonLabels.forEach(lonLabel -> {
            Transform.pushView();

            Transform.mulView(lonLabel.m);
            renderer.draw3D(lonLabel.txt, 0, 0, 0, textScaleFactor);
            renderer.flush();

            Transform.popView();
        });
        renderer.end3DRendering();
    }

    @Override
    public void init(GL2 gl) {
        gridLine.init(gl);
        GridMath.initGrid(gl, gridLine, lonStep, latStep);
        gridNeedsInit = false;

        axesLine.init(gl);
        GridMath.initAxes(gl, axesLine);

        earthCircleLine.init(gl);
        GridMath.initEarthCircles(gl, earthCircleLine);
        earthPoint.init(gl);
        GridMath.initEarthPoint(gl, earthPoint);

        radialCircleLine.init(gl);
        radialThickLine.init(gl);
        GridMath.initRadialCircles(gl, radialCircleLine, radialThickLine, RADIAL_UNIT, RADIAL_STEP);
        radialCircleLineFar.init(gl);
        radialThickLineFar.init(gl);
        GridMath.initRadialCircles(gl, radialCircleLineFar, radialThickLineFar, RADIAL_UNIT_FAR, RADIAL_STEP_FAR);

        flatLine.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        gridLine.dispose(gl);
        axesLine.dispose(gl);
        earthCircleLine.dispose(gl);
        earthPoint.dispose(gl);
        radialCircleLine.dispose(gl);
        radialThickLine.dispose(gl);
        radialCircleLineFar.dispose(gl);
        radialThickLineFar.dispose(gl);
        flatLine.dispose(gl);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Grid";
    }

    @Nullable
    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @SuppressWarnings("serial")
    private class GridLayerOptions extends JPanel {

        private static final double min = 5, max = 90;

        private JSpinner gridResolutionXSpinner;
        private JSpinner gridResolutionYSpinner;
        private JComboBox<GridType> gridTypeBox;

        GridLayerOptions() {
            createGridResolutionX();
            createGridResolutionY();

            setLayout(new GridBagLayout());

            GridBagConstraints c0 = new GridBagConstraints();
            c0.fill = GridBagConstraints.HORIZONTAL;
            c0.weightx = 1.;
            c0.weighty = 1.;

            c0.gridy = 0;

            c0.gridx = 1;
            c0.anchor = GridBagConstraints.LINE_END;
            JCheckBox axis = new JCheckBox("Solar axis", showAxis);
            axis.setHorizontalTextPosition(SwingConstants.LEFT);
            axis.addActionListener(e -> {
                showAxis = axis.isSelected();
                MovieDisplay.display();
            });
            add(axis, c0);

            c0.gridx = 3;
            c0.anchor = GridBagConstraints.LINE_END;
            JCheckBox labels = new JCheckBox("Grid labels", showLabels);
            labels.setHorizontalTextPosition(SwingConstants.LEFT);
            labels.addActionListener(e -> {
                showLabels = labels.isSelected();
                MovieDisplay.display();
            });
            add(labels, c0);

            c0.gridy = 1;

            c0.gridx = 1;
            c0.anchor = GridBagConstraints.LINE_END;
            JCheckBox radial = new JCheckBox("Radial grid", showRadial);
            radial.setHorizontalTextPosition(SwingConstants.LEFT);
            radial.addActionListener(e -> {
                showRadial = radial.isSelected();
                MovieDisplay.display();
            });
            add(radial, c0);

            c0.gridx = 2;
            c0.anchor = GridBagConstraints.LINE_END;
            add(new JLabel("Grid type ", JLabel.RIGHT), c0);
            c0.gridx = 3;
            c0.anchor = GridBagConstraints.LINE_START;
            createGridTypeBox();
            add(gridTypeBox, c0);

            c0.gridy = 2;

            c0.gridx = 0;
            c0.anchor = GridBagConstraints.LINE_END;
            add(new JLabel("Longitude ", JLabel.RIGHT), c0);

            JFormattedTextField fx = ((JSpinner.DefaultEditor) gridResolutionXSpinner.getEditor()).getTextField();
            fx.setFormatterFactory(new TerminatedFormatterFactory("%.1f", "\u00B0", min, max));

            c0.gridx = 1;
            c0.anchor = GridBagConstraints.LINE_START;
            add(gridResolutionXSpinner, c0);

            c0.gridx = 2;
            c0.anchor = GridBagConstraints.LINE_END;
            add(new JLabel("Latitude ", JLabel.RIGHT), c0);

            JFormattedTextField fy = ((JSpinner.DefaultEditor) gridResolutionYSpinner.getEditor()).getTextField();
            fy.setFormatterFactory(new TerminatedFormatterFactory("%.1f", "\u00B0", min, max));

            c0.gridx = 3;
            c0.anchor = GridBagConstraints.LINE_START;
            add(gridResolutionYSpinner, c0);
        }

        private void createGridTypeBox() {
            gridTypeBox = new JComboBox<>(GridType.values());
            gridTypeBox.setSelectedItem(Display.gridType);
            gridTypeBox.addActionListener(e -> {
                GridType gridType = (GridType) Objects.requireNonNull(gridTypeBox.getSelectedItem());
                Display.setGridType(gridType);
                lonLabels = GridLabel.makeLonLabels(gridType, lonStep);
                MovieDisplay.display();
            });
        }

        private void createGridResolutionX() {
            gridResolutionXSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(lonStep), Double.valueOf(min), Double.valueOf(max), Double.valueOf(0.1)));
            gridResolutionXSpinner.addChangeListener(e -> {
                lonStep = (Double) gridResolutionXSpinner.getValue();
                lonLabels = GridLabel.makeLonLabels(Display.gridType, lonStep);
                gridNeedsInit = true;
                MovieDisplay.display();
            });
            WheelSupport.installMouseWheelSupport(gridResolutionXSpinner);
        }

        private void createGridResolutionY() {
            gridResolutionYSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(latStep), Double.valueOf(min), Double.valueOf(max), Double.valueOf(0.1)));
            gridResolutionYSpinner.addChangeListener(e -> {
                latStep = (Double) gridResolutionYSpinner.getValue();
                latLabels = GridLabel.makeLatLabels(latStep);
                gridNeedsInit = true;
                MovieDisplay.display();
            });
            WheelSupport.installMouseWheelSupport(gridResolutionYSpinner);
        }

    }


}
