package org.helioviewer.gl3d.camera;

import java.awt.BorderLayout;

public class GL3DObserverCameraOptionPanel extends GL3DCameraOptionPanel {

    private static final long serialVersionUID = 1L;

    private final GL3DObserverCamera camera;

    public GL3DObserverCameraOptionPanel(GL3DObserverCamera camera) {
        super();
        this.camera = camera;
        this.setLayout(new BorderLayout());
    }

    @Override
    public void deactivate() {
        camera.deactivate();
    }

}
