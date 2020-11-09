package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.net.URI;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionListReceiver;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.connect.LoadFootpoints;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class ConnectLayer extends AbstractLayer implements PositionListReceiver {

    private final GLSLLine hcs = new GLSLLine(true);
    private final BufVertex hcsBuf = new BufVertex(512 * GLSLLine.stride);

    private final JPanel optionsPanel;

    private List<Position> posList;

    @Override
    public void serialize(JSONObject jo) {
    }

    public ConnectLayer(JSONObject jo) {
        optionsPanel = optionsPanel();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void init(GL2 gl) {
        hcs.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        hcs.dispose(gl);
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
        return "Connect";
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

    @Override
    public void setList(List<Position> _posList) {
        posList = _posList;
        posList.forEach(p -> System.out.println(">>> " + p));
    }

    private JPanel optionsPanel() {
        JButton button = new JButton("Footpoints");
        button.addActionListener(e -> loadFootpoints());

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

    private void loadFootpoints() {
        FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Choose a file", FileDialog.LOAD);
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0 && fileNames[0].isFile())
            LoadFootpoints.submit(fileNames[0].toURI(), this);
    }

}
