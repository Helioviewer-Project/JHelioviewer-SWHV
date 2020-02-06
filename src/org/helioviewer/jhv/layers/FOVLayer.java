package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class FOVLayer extends AbstractLayer {

    private enum FOVType {RECTANGULAR, CIRCULAR}

    static class FOV {

        private final String name;
        private final FOVType type;
        private final double inner;
        private final double wide;
        private final double high;
        private final byte[] color;
        private boolean enabled;

        private final FOVLayerOptions.OffControl[] offControl = new FOVLayerOptions.OffControl[2];
        private double centerX = 0;
        private double centerY = 0;

        private static double control2Center(double v) { // v in arcmin
            return Math.tan(v * (Math.PI / 180. / 60.));
        }

        FOV(String _name, FOVType _type, double innerDeg, double wideDeg, double highDeg, byte[] _color) {
            name = _name;
            type = _type;
            inner = 0.5 * Math.tan(innerDeg * (Math.PI / 180.));
            wide = 0.5 * Math.tan(wideDeg * (Math.PI / 180.));
            high = 0.5 * Math.tan(highDeg * (Math.PI / 180.));
            color = _color;

            offControl[0] = new FOVLayerOptions.OffControl();
            offControl[0].addChangeListener(e -> {
                centerX = control2Center((Double) offControl[0].getValue());
                MovieDisplay.display();
            });
            offControl[1] = new FOVLayerOptions.OffControl();
            offControl[1].addChangeListener(e -> {
                centerY = control2Center((Double) offControl[1].getValue());
                MovieDisplay.display();
            });
        }

        FOVLayerOptions.OffControl getOffControl(int col) {
            return offControl[col];
        }

        void putFOV(FOVShape f, double distance, BufVertex lineBuf, BufVertex centerBuf, JhvTextRenderer renderer) {
            if (!enabled)
                return;

            f.setCenter(centerX * distance, centerY * distance);
            f.putCenter(centerBuf, color);

            if (inner > 0)
                f.putCircLine(inner * distance, lineBuf, color);
            if (type == FOVType.RECTANGULAR) {
                f.putRectLine(wide * distance, high * distance, lineBuf, color);
                drawLabel(name, (wide + centerX) * distance, (-high + centerY) * distance, high * distance, renderer);
            } else {
                f.putCircLine(wide * distance, lineBuf, color);
                double halfSide = wide / Math.sqrt(2);
                drawLabel(name, (halfSide + centerX) * distance, (-halfSide + centerY) * distance, halfSide * distance, renderer);
            }
        }

        void zoom(Camera camera) {
            double distance = camera.getViewpoint().distance;
            camera.setTranslation(-centerX * distance, -centerY * distance);
            camera.resetDragRotation();
            camera.setFOV(2 * wide);
        }

        boolean isEnabled() {
            return enabled;
        }

        void toggle() {
            enabled = !enabled;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    private static final List<FOV> FOVs = List.of(
            new FOV("SOLO/EUI/HRI", FOVType.RECTANGULAR, 0, 16.6 / 60., 16.6 / 60., Colors.Blue),
            new FOV("SOLO/EUI/FSI", FOVType.RECTANGULAR, 0, 228 / 60., 228 / 60., Colors.Blue),
            new FOV("SOLO/METIS", FOVType.CIRCULAR, 3, 5.8, 5.8, Colors.Blue),
            new FOV("SOLO/PHI/HRT", FOVType.RECTANGULAR, 0, 0.28, 0.28, Colors.Blue),
            new FOV("SOLO/PHI/FDT", FOVType.RECTANGULAR, 0, 2, 2, Colors.Blue),
            new FOV("SOLO/SPICE", FOVType.RECTANGULAR, 0, 16 / 60., 11 / 60., Colors.Blue),
            new FOV("SOLO/STIX", FOVType.RECTANGULAR, 0, 2, 2, Colors.Blue)
    );

    static FOVLayerOptions.OffControl getOffControl(int row, int col) {
        return FOVs.get(row).getOffControl(col);
    }

    private static final double LINEWIDTH_FOV = GLSLLine.LINEWIDTH_BASIC;
    private static final double textEpsilon = 0.09;
    private static final double textScale = 0.075;

    private final FOVShape fov = new FOVShape();
    private final byte[] fovColor = Colors.Blue;
    private final GLSLLine fovLine = new GLSLLine(true);
    private final BufVertex lineBuf = new BufVertex((4 * (FOVShape.RECT_SUBDIVS + 1) + 2) * GLSLLine.stride);
    private final GLSLShape center = new GLSLShape(true);
    private final BufVertex centerBuf = new BufVertex(GLSLShape.stride);

    private final FOVLayerOptions optionsPanel;

    private static boolean customEnabled;
    private static double customAngle = Camera.INITFOV / Math.PI * 180;

    static void setCustomEnabled(boolean b) {
        customEnabled = b;
        MovieDisplay.display();
    }

    static void setCustomAngle(double a) {
        customAngle = a;
        MovieDisplay.display();
    }

    @Override
    public void serialize(JSONObject jo) {
    }

    public FOVLayer(JSONObject jo) {
        optionsPanel = new FOVLayerOptions(new FOVModel(FOVs), customAngle);
    }

    private static void drawLabel(String name, double x, double y, double size, JhvTextRenderer renderer) {
        float textScaleFactor = (float) (textScale / renderer.getFont().getSize2D() * size);
        renderer.draw3D(name, (float) x, (float) y, (float) (FOVShape.computeZ(x, y) + textEpsilon), textScaleFactor);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        if (!customEnabled) {
            boolean willDraw = false;
            for (FOV f : FOVs) {
                if (f.isEnabled()) {
                    willDraw = true;
                    break;
                }
            }
            if (!willDraw)
                return;
        }

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        Position viewpoint = camera.getViewpoint();

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());
        boolean far = Camera.useWideProjection(viewpoint.distance);
        if (far) {
            Transform.pushProjection();
            camera.projectionOrthoWide(vp.aspect);
        }

        JhvTextRenderer renderer = GLText.getRenderer(48);
        renderer.begin3DRendering();
        FOVs.forEach(f -> f.putFOV(fov, viewpoint.distance, lineBuf, centerBuf, renderer));
        if (customEnabled) {
            fov.setCenter(0, 0);
            fov.putCenter(centerBuf, fovColor);

            double halfSide = 0.5 * viewpoint.distance * Math.tan(customAngle * (Math.PI / 180.));
            fov.putRectLine(halfSide, halfSide, lineBuf, fovColor);
            drawLabel("Custom", halfSide, -halfSide, halfSide, renderer);
        }
        renderer.end3DRendering();

        center.setData(gl, centerBuf);
        center.renderPoints(gl, pixFactor);

        fovLine.setData(gl, lineBuf);
        fovLine.render(gl, vp.aspect, LINEWIDTH_FOV);

        if (far) {
            Transform.popProjection();
        }
        Transform.popView();
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void init(GL2 gl) {
        fovLine.init(gl);
        center.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        fovLine.dispose(gl);
        center.dispose(gl);
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
        return "FOV";
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
    static class FOVModel extends AbstractTableModel {

        private final List<FOV> fovs;

        FOVModel(List<FOV> _fovs) {
            fovs = _fovs;
        }

        @Override
        public int getRowCount() {
            return fovs.size();
        }

        @Override
        public int getColumnCount() {
            return FOVLayerOptions.NUMBEROFCOLUMNS;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (col == FOVLayerOptions.SELECTED_COL || col == FOVLayerOptions.FOV_COL)
                return fovs.get(row);
            else
                return fovs.get(row).getOffControl(col - FOVLayerOptions.OFF1_COL).getValue();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return fovs.get(row).isEnabled() && col != FOVLayerOptions.SELECTED_COL && col != FOVLayerOptions.FOV_COL;
        }

    }

}
