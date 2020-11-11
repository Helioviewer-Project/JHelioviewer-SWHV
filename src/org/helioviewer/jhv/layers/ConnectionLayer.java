package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionMapReceiver;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.connect.LoadFootpoint;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class ConnectionLayer extends AbstractLayer implements PositionMapReceiver {

    private static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private static final double radius = 1.01;

    private final GLSLLine footpoint = new GLSLLine(true);
    private final BufVertex footpointBuf = new BufVertex(12 * GLSLLine.stride);

    private final JPanel optionsPanel;

    private TimeMap<Position> positionMap;
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
        if (positionMap == null)
            return;
        draw(camera, vp, gl);
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        render(camera, vp, gl);
    }

    private void draw(Camera camera, Viewport vp, GL2 gl) {
        Position viewpoint = camera.getViewpoint();
        Position p = positionMap.nearestValue(viewpoint.time);
        if (!p.time.equals(lastTimestamp)) {
            lastTimestamp = p.time; // should be reset to null
            JHVFrame.getLayers().fireTimeUpdated(this);
        }

        Vec3 v = new Vec3(radius, Math.PI / 2 - p.lat, p.lon);
        Quat q = Layers.getGridLayer().getGridType().toQuat(viewpoint);

        AnnotateCross.drawCross(q, vp, v, footpointBuf, Colors.Green);
        footpoint.setData(gl, footpointBuf);
        footpoint.render(gl, vp.aspect, LINEWIDTH);
    }

    @Override
    public void init(GL2 gl) {
        footpoint.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        footpoint.dispose(gl);
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
        return "Connection";
    }

    @Nullable
    @Override
    public String getTimeString() {
        return positionMap == null ? null : lastTimestamp.toString();
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void setMap(TimeMap<Position> _positionMap) {
        positionMap = _positionMap;
        MovieDisplay.display();
    }

    private JPanel optionsPanel() {
        JButton button = new JButton("Footpoint");
        button.addActionListener(e -> loadFootpoint());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        panel.add(button, c0);

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

}
