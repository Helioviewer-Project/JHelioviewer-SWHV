package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.connect.LoadConnectivity;
import org.helioviewer.jhv.layers.connect.LoadConnectivity.Connectivity;
import org.helioviewer.jhv.layers.connect.LoadFootpoint;
import org.helioviewer.jhv.layers.connect.LoadHCS;
import org.helioviewer.jhv.layers.connect.LoadSunJSON;
import org.helioviewer.jhv.layers.connect.OrthoScaleList;
import org.helioviewer.jhv.layers.connect.SunJSON;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class ConnectionLayer extends AbstractLayer implements LoadConnectivity.Receiver, LoadFootpoint.Receiver, LoadHCS.Receiver, LoadSunJSON.Receiver {

    private static final double LINEWIDTH = 2 * GLSLLine.LINEWIDTH_BASIC;
    private static final float SIZE_POINT = 0.01f;

    private final byte[] sswColor = Colors.bytes(164, 48, 42);
    private final byte[] fswColor = Colors.bytes(74, 136, 92);
    private final byte[] mColor = Colors.bytes(240, 145, 53);
    private final GLSLShape connectivityCenter = new GLSLShape(true);
    private final BufVertex connectivityBuf = new BufVertex(96 * GLSLShape.stride);

    private final byte[] hcsColor = Colors.bytes(223, 62, 48);
    private final GLSLLine hcsLine = new GLSLLine(true); // TBD
    private final BufVertex hcsBuf = new BufVertex(512 * GLSLLine.stride);

    private final byte[] footpointColor = Colors.White;
    private final GLSLLine footpointLine = new GLSLLine(true);
    private final BufVertex footpointBuf = new BufVertex(12 * GLSLLine.stride);

    private final GLSLLine geometryLine = new GLSLLine(true);
    private final GLSLShape geometryPoint = new GLSLShape(true);

    private final JPanel optionsPanel;

    private Connectivity connectivity;
    private OrthoScaleList hcs;
    private TimeMap<Position.Cartesian> footpointMap;
    private final TimeMap<SunJSON.GeometryCollection> geometryMap = new TimeMap<>();

    private JHVTime lastTimestamp;

    @Override
    public void serialize(JSONObject jo) {
    }

    public ConnectionLayer(JSONObject jo) {
        optionsPanel = optionsPanel();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        if (connectivity != null)
            drawConnectivity(camera, vp, gl);
        if (hcs != null)
            drawHCS(camera, vp, gl);
        if (footpointMap != null)
            drawFootpointInterpolated(camera, vp, gl);
        geometryMap.nearestValue(camera.getViewpoint().time).render(gl, geometryLine, geometryPoint, vp.aspect, CameraHelper.getPixelFactor(camera, vp));
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        render(camera, vp, gl);
    }

    private void drawConnectivity(Camera camera, Viewport vp, GL2 gl) {
        Quat q = Display.gridType.toGrid(camera.getViewpoint());
        putConnectivity(q, vp, connectivity.SSW, connectivityBuf, sswColor);
        putConnectivity(q, vp, connectivity.FSW, connectivityBuf, fswColor);
        putConnectivity(q, vp, connectivity.M, connectivityBuf, mColor);

        connectivityCenter.setVertex(gl, connectivityBuf);
        connectivityCenter.renderPoints(gl, CameraHelper.getPixelFactor(camera, vp));
    }

    private static void putPointScale(Quat q, Viewport vp, Vec3 vertex, BufVertex vexBuf, byte[] color) {
        Vec2 tf = Display.mode.transform(q, vertex);
        float x = (float) (tf.x * vp.aspect);
        float y = (float) tf.y;
        vexBuf.putVertex(x, y, 0, SIZE_POINT, color);
    }

    private static void putConnectivity(Quat q, Viewport vp, OrthoScaleList points, BufVertex vexBuf, byte[] color) {
        if (Display.mode == Display.ProjectionMode.Orthographic)
            points.ortho.forEach(v -> vexBuf.putVertex((float) v.x, (float) v.y, (float) v.z, 2 * SIZE_POINT, color));
        else
            points.scale.forEach(v -> putPointScale(q, vp, v, vexBuf, color));
    }

    private void drawHCS(Camera camera, Viewport vp, GL2 gl) {
        if (hcs.size == 0)
            return;
        if (Display.mode == Display.ProjectionMode.Orthographic) {
            Vec3 first = hcs.ortho.get(0);
            hcsBuf.putVertex(first, Colors.Null);
            hcs.ortho.forEach(v -> hcsBuf.putVertex(v, hcsColor));
            hcsBuf.putVertex(first, hcsColor);
            hcsBuf.putVertex(first, Colors.Null);
        } else {
            Quat q = Display.gridType.toGrid(camera.getViewpoint());
            Vec2 previous = null;

            Vec3 first = hcs.scale.get(0);
            GLHelper.drawVertex(q, vp, first, previous, hcsBuf, Colors.Null);

            int size = hcs.scale.size();
            for (int i = 0; i < size; i++) {
                Vec3 v = hcs.scale.get(i);
                previous = GLHelper.drawVertex(q, vp, v, previous, hcsBuf, hcsColor);
            }
            previous = GLHelper.drawVertex(q, vp, first, previous, hcsBuf, hcsColor);
            GLHelper.drawVertex(q, vp, first, previous, hcsBuf, Colors.Null);
        }

        hcsLine.setVertex(gl, hcsBuf);
        hcsLine.renderLine(gl, vp.aspect, LINEWIDTH);
    }

    private static Vec3 interpolate(long t, Position.Cartesian prev, Position.Cartesian next) {
        long tprev = prev.milli();
        long tnext = next.milli();
        double alpha = tnext == tprev ? 1. : ((t - tprev) / (double) (tnext - tprev)) % 1.;
        double x = (1. - alpha) * prev.x() + alpha * next.x();
        double y = (1. - alpha) * prev.y() + alpha * next.y();
        double z = (1. - alpha) * prev.z() + alpha * next.z();

        return new Vec3(1, Math.acos(y), Math.atan2(x, z));
    }

    /*
        private void drawFootpointNearest(Camera camera, Viewport vp, GL2 gl) {
            Position viewpoint = camera.getViewpoint();
            PositionCartesian p = footpointMap.nearestValue(viewpoint.time);
            if (!p.time.equals(lastTimestamp)) {
                lastTimestamp = p.time; // should be reset to null
                JHVFrame.getLayers().fireTimeUpdated(this);
            }

            Vec3 v = new Vec3(1, Math.acos(p.y), Math.atan2(p.x, p.z));
            Quat q = Display.getGridType().toGrid(viewpoint);

            AnnotateCross.drawCross(q, vp, v, footpointBuf, footpointColor);
            footpointLine.setVertex(gl, footpointBuf);
            footpointLine.render(gl, vp.aspect, LINEWIDTH);
        }
    */
    private void drawFootpointInterpolated(Camera camera, Viewport vp, GL2 gl) {
        Position viewpoint = camera.getViewpoint();
        if (!viewpoint.time.equals(lastTimestamp)) {
            lastTimestamp = viewpoint.time;
            JHVFrame.getLayers().fireTimeUpdated(this);
        }

        Vec3 v = interpolate(viewpoint.time.milli, footpointMap.lowerValue(viewpoint.time), footpointMap.higherValue(viewpoint.time));
        Quat q = Display.gridType.toGrid(viewpoint);

        AnnotateCross.drawCross(q, vp, v, footpointBuf, footpointColor);
        footpointLine.setVertex(gl, footpointBuf);
        footpointLine.renderLine(gl, vp.aspect, LINEWIDTH);
    }

    @Override
    public void init(GL2 gl) {
        connectivityCenter.init(gl);
        hcsLine.init(gl);
        footpointLine.init(gl);

        geometryLine.init(gl);
        geometryPoint.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        connectivityCenter.dispose(gl);
        hcsLine.dispose(gl);
        footpointLine.dispose(gl);

        geometryLine.dispose(gl);
        geometryPoint.dispose(gl);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);
        if (!enabled) {
            lastTimestamp = null;
        }
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Connection";
    }

    @Nullable
    @Override
    public String getTimeString() {
        return lastTimestamp == null ? null : lastTimestamp.toString();
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void setConnectivity(Connectivity _connectivity) {
        connectivity = _connectivity;
        // System.out.println(">>> SSW: " + connectivity.SSW.ortho.size() + " FSW: " + connectivity.FSW.ortho.size() + " M: " + connectivity.M.ortho.size());
        MovieDisplay.display();
    }

    @Override
    public void setPositionMap(TimeMap<Position.Cartesian> _footpointMap) {
        footpointMap = _footpointMap;
        MovieDisplay.display();
    }

    @Override
    public void setHCS(OrthoScaleList _hcs) {
        hcs = _hcs;
        // System.out.println(">>> HCS: " + hcs.ortho.size());
        MovieDisplay.display();
    }

    @Override
    public void setGeometry(List<SunJSON.GeometryCollection> geometry) {
        geometry.forEach(g -> geometryMap.put(g.time(), g));
        geometryMap.buildIndex();
        MovieDisplay.display();
    }

    private JPanel optionsPanel() {
        JButton clearBtn = new JButton("Clear all");
        clearBtn.addActionListener(e -> {
            connectivity = null;
            hcs = null;
            footpointMap = null;
            geometryMap.clear();
            MovieDisplay.display();
        });

        JButton connectivityBtn = new JButton("Connectivity");
        connectivityBtn.addActionListener(e -> load(LoadConnectivity::submit));

        JButton hcsBtn = new JButton("HCS");
        hcsBtn.addActionListener(e -> load(LoadHCS::submit));

        JButton footpointBtn = new JButton("Footpoint");
        footpointBtn.addActionListener(e -> load(LoadFootpoint::submit));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;

        c0.gridx = 0;
        panel.add(clearBtn, c0);
        c0.gridx = 1;
        panel.add(connectivityBtn, c0);
        c0.gridx = 2;
        panel.add(hcsBtn, c0);
        c0.gridx = 3;
        panel.add(footpointBtn, c0);

        return panel;
    }

    private void load(BiConsumer<URI, ConnectionLayer> consumer) {
        FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Choose a file", FileDialog.LOAD);
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0 && fileNames[0].isFile())
            consumer.accept(fileNames[0].toURI(), this);
    }

}
