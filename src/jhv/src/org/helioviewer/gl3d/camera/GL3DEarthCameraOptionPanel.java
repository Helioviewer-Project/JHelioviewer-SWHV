package org.helioviewer.gl3d.camera;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class GL3DEarthCameraOptionPanel extends GL3DCameraOptionPanel {

    private static final long serialVersionUID = 1L;

    private final GL3DEarthCamera camera;

    public GL3DEarthCameraOptionPanel(GL3DEarthCamera camera) {
        super();
        this.camera = camera;
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(0, 200));
        add(panel);
    }

    @Override
    public void deactivate() {
    }

}
