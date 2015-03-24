package org.helioviewer.gl3d.camera;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

public abstract class GL3DCameraOptionPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    abstract public void deactivate();

    public GL3DCameraOptionPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    }

}
