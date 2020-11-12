package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionCartesian;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.connect.LoadFootpoint;
import org.helioviewer.jhv.layers.connect.LoadHCS;
import org.helioviewer.jhv.layers.connect.PositionMapReceiver;
import org.helioviewer.jhv.layers.connect.VecListReceiver;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class ConnectionLayer extends AbstractLayer implements PositionMapReceiver, VecListReceiver {

    private static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private static final double radius = 1.01;

    private final GLSLLine footpointLine = new GLSLLine(true);
    private final BufVertex footpointBuf = new BufVertex(12 * GLSLLine.stride);

    private final JPanel optionsPanel;

    private List<Vec3> hcsList;
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
        if (footpointMap != null)
            drawFootpointInterpolated(camera, vp, gl);
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        render(camera, vp, gl);
    }

    private static Vec3 interpolate(long t, PositionCartesian prev, PositionCartesian next) {
        long tprev = prev.time.milli;
        long tnext = next.time.milli;
        double alpha = tnext == tprev ? 1. : ((t - tprev) / (double) (tnext - tprev)) % 1.;
        double x = (1. - alpha) * prev.x + alpha * next.x;
        double y = (1. - alpha) * prev.y + alpha * next.y;
        double z = (1. - alpha) * prev.z + alpha * next.z;

        return new Vec3(radius, Math.acos(y), Math.atan2(x, z));
    }

    /*
        private void drawFootpointNearest(Camera camera, Viewport vp, GL2 gl) {
            Position viewpoint = camera.getViewpoint();
            PositionCartesian p = footpointMap.nearestValue(viewpoint.time);
            if (!p.time.equals(lastTimestamp)) {
                lastTimestamp = p.time; // should be reset to null
                JHVFrame.getLayers().fireTimeUpdated(this);
            }

            Vec3 v = new Vec3(radius, Math.acos(p.y), Math.atan2(p.x, p.z));
            Quat q = Layers.getGridLayer().getGridType().toQuat(viewpoint);

            AnnotateCross.drawCross(q, vp, v, footpointBuf, Colors.Green);
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
        Quat q = Layers.getGridLayer().getGridType().toQuat(viewpoint);

        AnnotateCross.drawCross(q, vp, v, footpointBuf, Colors.Green);
        footpointLine.setData(gl, footpointBuf);
        footpointLine.render(gl, vp.aspect, LINEWIDTH);
    }

    @Override
    public void init(GL2 gl) {
        footpointLine.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
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
    public void setMap(TimeMap<PositionCartesian> _footpointMap) {
        footpointMap = _footpointMap;
        MovieDisplay.display();
    }

    @Override
    public void setList(List<Vec3> _hcsList) {
        hcsList = _hcsList;
        MovieDisplay.display();
    }

    private JPanel optionsPanel() {
        JButton footpointBtn = new JButton("Footpoint");
        footpointBtn.addActionListener(e -> loadFootpoint());

        JButton hcsBtn = new JButton("HCS");
        hcsBtn.addActionListener(e -> loadHCS());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;

        c0.gridx = 0;
        panel.add(footpointBtn, c0);
        c0.gridx = 1;
        panel.add(hcsBtn, c0);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

    private void loadFootpoint() {
        FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Choose a file", FileDialog.LOAD);
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0 && fileNames[0].isFile())
            LoadFootpoint.submit(fileNames[0].toURI(), this);
    }

    private void loadHCS() {
        FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Choose a file", FileDialog.LOAD);
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0 && fileNames[0].isFile())
            LoadHCS.submit(fileNames[0].toURI(), this);
    }

}
