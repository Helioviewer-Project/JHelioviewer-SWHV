package org.helioviewer.gl3d.camera;

public class GL3DEarthCameraOptionPanel extends GL3DCameraOptionPanel {

    private final GL3DEarthCamera camera;

    public GL3DEarthCameraOptionPanel(GL3DEarthCamera camera) {
        super(camera);
        this.camera = camera;
        createGridOptions();
    }

    @Override
    public void deactivate() {
    }

}
