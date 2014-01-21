package org.helioviewer.gl3d.view;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.GL3DKeyController;
import org.helioviewer.gl3d.GL3DKeyController.GL3DKeyListener;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraListener;
import org.helioviewer.gl3d.camera.GL3DTrackballCamera;
import org.helioviewer.gl3d.changeevent.CameraChangeChangedReason;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.View;

/**
 * The {@link GL3DCameraView} is responsible for applying the currently active
 * {@link GL3DCamera}. Since applying the view space transformation is the first
 * transformation to be applied in a scene, this view must be executed before
 * the {@link GL3DSceneGraphView}.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraView extends AbstractGL3DView implements GL3DView, GL3DCameraListener {
    private GL3DCamera camera;

    private List<GL3DCameraListener> listeners = new ArrayList<GL3DCameraListener>();

    public GL3DCameraView() {
        // Register short keys for changing the interaction
        GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {

            public void keyHit(KeyEvent e) {
                camera.setCurrentInteraction(camera.getPanInteraction());
            }
        }, KeyEvent.VK_P);
        GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {

            public void keyHit(KeyEvent e) {
                camera.setCurrentInteraction(camera.getRotateInteraction());
            }
        }, KeyEvent.VK_R);
        GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {

            public void keyHit(KeyEvent e) {
                camera.setCurrentInteraction(camera.getZoomInteraction());
            }
        }, KeyEvent.VK_Z);

        // Center Image when pressing alt+c
        GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {

            public void keyHit(KeyEvent e) {
                if (e.isAltDown()) {
                    camera.setPanning(0, 0);
                    camera.updateCameraTransformation();
                }
            }
        }, KeyEvent.VK_C);
    }

    public void render3D(GL3DState state) {
        GL gl = state.gl;

        if (this.camera != null) {
            state.setActiveChamera(this.camera);

            if (this.getView() != null) {
                this.renderChild(gl);
            }
        }
    }

    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
    }

    public GL3DCamera getCurrentCamera() {
        return this.camera;
    }

    public void setCurrentCamera(GL3DCamera cam) {
        if (this.camera != null) {
            this.camera.removeCameraListener(this);
            this.camera.deactivate();
        }
        cam.activate(this.camera);
        this.camera = cam;
        this.camera.addCameraListener(this);
        Log.debug("GL3DCameraView: Set Current Camera to " + this.camera);
        notifyViewListeners(new ChangeEvent(new CameraChangeChangedReason(this, this.camera)));
    }

    public void cameraMoved(GL3DCamera camera) {
        for (GL3DCameraListener l : this.listeners) {
            l.cameraMoved(camera);
        }
    }

    public void cameraMoving(GL3DCamera camera) {
        for (GL3DCameraListener l : this.listeners) {
            l.cameraMoving(camera);
        }
    }

    public void addCameraListener(GL3DCameraListener listener) {
        this.listeners.add(listener);
    }

    public void removeCameraListener(GL3DCameraListener listener) {
        this.listeners.remove(listener);
    }
}
