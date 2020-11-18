package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.net.URI;
import java.util.function.BiFunction;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionCartesian;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.connect.LoadConnectivity;
import org.helioviewer.jhv.layers.connect.LoadFootpoint;
import org.helioviewer.jhv.layers.connect.LoadHCS;
import org.helioviewer.jhv.layers.connect.ReceiverConnectivity;
import org.helioviewer.jhv.layers.connect.ReceiverConnectivity.Connectivity;
import org.helioviewer.jhv.layers.connect.ReceiverHCS;
import org.helioviewer.jhv.layers.connect.ReceiverPositionMap;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLLine;
//import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class ConnectionLayer extends AbstractLayer implements ReceiverConnectivity, ReceiverHCS, ReceiverPositionMap {

    private static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private static final double radius = 1.01;

    private final byte[] sswColor = Colors.Red;
    private final byte[] fswColor = Colors.Green;
    private final byte[] mColor = Colors.Orange;
    // private final GLSLShape connectivityCenter = new GLSLShape(true);
    // private final BufVertex connectivityBuf = new BufVertex(96 * GLSLShape.stride);
    private final GLSLLine connectivityCenter = new GLSLLine(true);
    private final BufVertex connectivityBuf = new BufVertex(96 * GLSLLine.stride);

    private final byte[] hcsColor = Colors.Red;
    private final GLSLLine hcsLine = new GLSLLine(true); // TBD
    private final BufVertex hcsBuf = new BufVertex(512 * GLSLLine.stride);

    private final byte[] footpointColor = Colors.Green;
    private final GLSLLine footpointLine = new GLSLLine(true);
    private final BufVertex footpointBuf = new BufVertex(12 * GLSLLine.stride);

    private final JPanel optionsPanel;

    private Connectivity connectivity;
    private HCS hcs;
    private TimeMap<PositionCartesian> footpointMap;

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
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        render(camera, vp, gl);
    }

    private void drawConnectivity(Camera camera, Viewport vp, GL2 gl) {
        Position viewpoint = camera.getViewpoint();
        Quat q = Layers.getGridLayer().getGridType().toGrid(viewpoint);

        connectivity.SSW.forEach(v -> AnnotateCross.drawCross(q, vp, v, connectivityBuf, sswColor));
        connectivity.FSW.forEach(v -> AnnotateCross.drawCross(q, vp, v, connectivityBuf, fswColor));
        connectivity.M.forEach(v -> AnnotateCross.drawCross(q, vp, v, connectivityBuf, mColor));
        connectivityCenter.setData(gl, connectivityBuf);
        connectivityCenter.render(gl, vp.aspect, LINEWIDTH);
    }

    private void drawHCS(Camera camera, Viewport vp, GL2 gl) {
        if (Display.mode == Display.DisplayMode.Orthographic) {
            Vec3 first = hcs.ortho.get(0);
            hcsBuf.putVertex(first, Colors.Null);
            hcs.ortho.forEach(v -> hcsBuf.putVertex(v, hcsColor));
            hcsBuf.putVertex(first, hcsColor);
            hcsBuf.putVertex(first, Colors.Null);
        } else {
            Quat q = Layers.getGridLayer().getGridType().toGrid(camera.getViewpoint());
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

        hcsLine.setData(gl, hcsBuf);
        hcsLine.render(gl, vp.aspect, 2 * LINEWIDTH);
    }

    private static Vec3 interpolate(long t, PositionCartesian prev, PositionCartesian next) {
        long tprev = prev.time.milli;
        long tnext = next.time.milli;
        double alpha = tnext == tprev ? 1. : ((t - tprev) / (double) (tnext - tprev)) % 1.;
        double x = (1. - alpha) * prev.x + alpha * next.x;
        double y = (1. - alpha) * prev.y + alpha * next.y;
        double z = (1. - alpha) * prev.z + alpha * next.z;

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
            Quat q = Layers.getGridLayer().getGridType().toGrid(viewpoint);

            AnnotateCross.drawCross(q, vp, v, footpointBuf, footpointColor);
            footpointLine.setData(gl, footpointBuf);
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
        Quat q = Layers.getGridLayer().getGridType().toGrid(viewpoint);

        AnnotateCross.drawCross(q, vp, v, footpointBuf, footpointColor);
        footpointLine.setData(gl, footpointBuf);
        footpointLine.render(gl, vp.aspect, LINEWIDTH);
    }

    @Override
    public void init(GL2 gl) {
        connectivityCenter.init(gl);
        hcsLine.init(gl);
        footpointLine.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        connectivityCenter.dispose(gl);
        hcsLine.dispose(gl);
        footpointLine.dispose(gl);
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
        // System.out.println(">>> SSW: " + connectivity.SSW.size() + " FSW: " + connectivity.FSW.size() + " M: " + connectivity.M.size());
        MovieDisplay.display();
    }

    @Override
    public void setPositionMap(TimeMap<PositionCartesian> _footpointMap) {
        footpointMap = _footpointMap;
        MovieDisplay.display();
    }

    @Override
    public void setHCS(HCS _hcs) {
        hcs = _hcs;
        // System.out.println(">>> HCS: " + hcs.ortho.size());
        MovieDisplay.display();
    }

    private JPanel optionsPanel() {
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
        panel.add(connectivityBtn, c0);
        c0.gridx = 1;
        panel.add(hcsBtn, c0);
        c0.gridx = 2;
        panel.add(footpointBtn, c0);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

    private void load(BiFunction<URI, ConnectionLayer, Void> function) {
        FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Choose a file", FileDialog.LOAD);
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0 && fileNames[0].isFile())
            function.apply(fileNames[0].toURI(), this);
    }

}
