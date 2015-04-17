package org.helioviewer.viewmodel.view.opengl;

import java.awt.event.KeyEvent;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.GL3DKeyController;
import org.helioviewer.gl3d.GL3DKeyController.GL3DKeyListener;
import org.helioviewer.gl3d.camera.GL3DCamera;

/**
 * The {@link GL3DCameraView} is responsible for applying the currently active
 * {@link GL3DCamera}. Since applying the view space transformation is the first
 * transformation to be applied in a scene
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DCameraView {
    private GL3DCamera camera;

    public GL3DCameraView() {
        // Register short keys for changing the interaction
        GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
            @Override
            public void keyHit(KeyEvent e) {
                camera.setCurrentInteraction(camera.getPanInteraction());
            }
        }, KeyEvent.VK_P);

        GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
            @Override
            public void keyHit(KeyEvent e) {
                camera.setCurrentInteraction(camera.getRotateInteraction());
            }
        }, KeyEvent.VK_R);

        GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
            @Override
            public void keyHit(KeyEvent e) {
                camera.setCurrentInteraction(camera.getZoomInteraction());
            }
        }, KeyEvent.VK_Z);

        // Center Image when pressing alt+c
        GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
            @Override
            public void keyHit(KeyEvent e) {
                if (e.isAltDown()) {
                    camera.setPanning(0, 0);
                    camera.updateCameraTransformation();
                }
            }
        }, KeyEvent.VK_C);
    }

    public GL3DCamera getCurrentCamera() {
        return this.camera;
    }

    public void setCurrentCamera(GL3DCamera cam) {
        cam.activate(this.camera);
        this.camera = cam;
        Log.debug("GL3DCameraView: Set Current Camera to " + this.camera);
    }

}
