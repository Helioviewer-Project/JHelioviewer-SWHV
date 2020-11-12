package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.net.URI;
import java.util.List;
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
import org.helioviewer.jhv.layers.connect.LoadFootpoint;
import org.helioviewer.jhv.layers.connect.LoadHCS;
import org.helioviewer.jhv.layers.connect.PositionMapReceiver;
import org.helioviewer.jhv.layers.connect.VecListReceiver;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class ConnectionLayer extends AbstractLayer implements PositionMapReceiver, VecListReceiver {

    private static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private static final double radius = 1.01;

    private final byte[] footpointColor = Colors.Green;
    private final GLSLLine footpointLine = new GLSLLine(true);
    private final BufVertex footpointBuf = new BufVertex(12 * GLSLLine.stride);

    private final byte[] hcsColor = Colors.Red;
    private final GLSLLine hcsLine = new GLSLLine(true); // TBD
    private final BufVertex hcsBuf = new BufVertex(512 * GLSLLine.stride);

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
        if (hcsList != null)
            drawHCS(camera, vp, gl);
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        render(camera, vp, gl);
    }

    private void drawHCS(Camera camera, Viewport vp, GL2 gl) {
        int size = hcsList.size();
        Vec3 v, vv = new Vec3();

        if (Display.mode == Display.DisplayMode.Orthographic) {
            for (int i = 0; i < size; i++) {
                v = hcsList.get(i);
                vv.x = radius * v.x;
                vv.y = radius * v.y;
                vv.z = radius * v.z;

                if (i == 0) {
                    hcsBuf.putVertex(vv, Colors.Null);
                }
                hcsBuf.putVertex(vv, hcsColor);
                if (i == size - 1) {
                    v = hcsList.get(0);
                    vv.x = radius * v.x;
                    vv.y = radius * v.y;
                    vv.z = radius * v.z;
                    hcsBuf.putVertex(vv, hcsColor);
                    hcsBuf.putVertex(vv, Colors.Null);
                }
            }
        } else {
            Quat q = Layers.getGridLayer().getGridType().toQuat(camera.getViewpoint());
            Vec2 previous = null;

            for (int i = 0; i < size; i++) {
                v = hcsList.get(i);
                vv.x = v.x;
                vv.y = -v.y;
                vv.z = v.z;

                if (i == 0) {
                    GLHelper.drawVertex(q, vp, vv, previous, hcsBuf, Colors.Null);
                }
                previous = GLHelper.drawVertex(q, vp, vv, previous, hcsBuf, hcsColor);
                if (i == size - 1) {
                    v = hcsList.get(0);
                    vv.x = v.x;
                    vv.y = -v.y;
                    vv.z = v.z;
                    previous = GLHelper.drawVertex(q, vp, vv, previous, hcsBuf, hcsColor);
                    GLHelper.drawVertex(q, vp, vv, previous, hcsBuf, Colors.Null);
                }
            }
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
        Quat q = Layers.getGridLayer().getGridType().toQuat(viewpoint);

        AnnotateCross.drawCross(q, vp, v, footpointBuf, footpointColor);
        footpointLine.setData(gl, footpointBuf);
        footpointLine.render(gl, vp.aspect, LINEWIDTH);
    }

    @Override
    public void init(GL2 gl) {
        footpointLine.init(gl);
        hcsLine.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        footpointLine.dispose(gl);
        hcsLine.dispose(gl);
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
        footpointBtn.addActionListener(e -> load(LoadFootpoint::submit));

        JButton hcsBtn = new JButton("HCS");
        hcsBtn.addActionListener(e -> load(LoadHCS::submit));

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

    private void load(BiFunction<URI, ConnectionLayer, Void> func) {
        FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Choose a file", FileDialog.LOAD);
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0 && fileNames[0].isFile())
            func.apply(fileNames[0].toURI(), this);
    }

}
