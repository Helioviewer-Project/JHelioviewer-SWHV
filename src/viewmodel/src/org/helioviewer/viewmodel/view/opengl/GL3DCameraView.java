package org.helioviewer.viewmodel.view.opengl;

import java.awt.event.KeyEvent;

import javax.media.opengl.GL2;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.GL3DKeyController;
import org.helioviewer.gl3d.GL3DKeyController.GL3DKeyListener;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.View;

/**
 * The {@link GL3DCameraView} is responsible for applying the currently active
 * {@link GL3DCamera}. Since applying the view space transformation is the first
 * transformation to be applied in a scene, this view must be executed before
 * the {@link GL3DSceneGraphView}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DCameraView extends AbstractGL3DView implements GL3DView {
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

    @Override
    public void render3D(GL3DState state) {
        GL2 gl = state.gl;

        if (this.camera != null) {
            state.setActiveChamera(this.camera);

            if (this.getView() != null) {
                this.renderChild(gl);
            }
        }
    }

    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
    }

    public GL3DCamera getCurrentCamera() {
        return this.camera;
    }

    public void setCurrentCamera(GL3DCamera cam) {
        cam.activate(this.camera);
        this.camera = cam;
        Log.debug("GL3DCameraView: Set Current Camera to " + this.camera);
    }

    @Override
    protected void renderChild(GL2 gl) {
        if (view instanceof GLView) {
            ((GLView) view).renderGL(gl, false);
        }
    }
}
