package org.helioviewer.gl3d.camera;

public class GL3DObserverCameraOptionPanel extends GL3DCameraOptionPanel {

    private final GL3DObserverCamera camera;

    public GL3DObserverCameraOptionPanel(GL3DObserverCamera camera) {
        super(camera);
        this.camera = camera;
        createGridOptions();
    }

    @Override
    public void deactivate() {

    }

}
