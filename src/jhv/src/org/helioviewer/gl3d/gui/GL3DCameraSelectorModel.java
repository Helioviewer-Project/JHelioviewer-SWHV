package org.helioviewer.gl3d.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DEarthCamera;
import org.helioviewer.gl3d.camera.GL3DFollowObjectCamera;
import org.helioviewer.gl3d.camera.GL3DObserverCamera;
import org.helioviewer.gl3d.camera.GL3DSolarRotationTrackingTrackballCamera;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.viewmodel.view.opengl.GL3DCameraView;

/**
 * Can be used as the global singleton for all available and the currently
 * active {@link GL3DCamera}. Also it implements the {@link ComboBoxModel} and
 * {@link ListModel} and can thus be used for GUI elements directly.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DCameraSelectorModel extends AbstractListModel implements ComboBoxModel {

    private static final long serialVersionUID = 1L;

    private static GL3DCameraSelectorModel instance;

    private final List<GL3DCamera> cameras = new ArrayList<GL3DCamera>();

    private GL3DCamera defaultCamera;

    private GL3DCamera lastCamera;

    private GL3DEarthCamera earthCamera;

    private GL3DSolarRotationTrackingTrackballCamera solarRotationCamera;

    private GL3DObserverCamera observerCamera;

    private GL3DFollowObjectCamera followObjectCamera;
    private final ArrayList<GL3DCameraSelectionModelListener> listeners = new ArrayList<GL3DCameraSelectionModelListener>();

    public static GL3DCameraSelectorModel getInstance() {
        if (instance == null) {
            instance = new GL3DCameraSelectorModel();
        }
        return instance;
    }

    public void activate() {
        if (earthCamera == null) {
            earthCamera = new GL3DEarthCamera();
            observerCamera = new GL3DObserverCamera();
            followObjectCamera = new GL3DFollowObjectCamera();
            defaultCamera = observerCamera;
            lastCamera = defaultCamera;
            cameras.add(earthCamera);
            cameras.add(observerCamera);
            cameras.add(followObjectCamera);

            defaultCamera = observerCamera;
        } else {
            setCurrentCamera(observerCamera);
        }
        if (lastCamera != null) {
            setCurrentCamera(lastCamera);
        } else {
            setCurrentCamera(defaultCamera);
        }

        this.fireInit();
    }

    private void fireInit() {
        for (GL3DCameraSelectionModelListener listener : listeners) {
            listener.fireInit();
        }
    }

    public GL3DCamera getCurrentCamera() {
        return ImageViewerGui.getSingletonInstance().getCameraView().getCurrentCamera();
    }

    @Override
    public Object getElementAt(int index) {
        return cameras.get(index);
    }

    @Override
    public int getSize() {
        return cameras.size();
    }

    @Override
    public GL3DCamera getSelectedItem() {
        GL3DCameraView cameraView = ImageViewerGui.getSingletonInstance().getCameraView();
        if (cameraView != null) {
            return cameraView.getCurrentCamera();
        }
        return null;
    }

    public void setCurrentCamera(GL3DCamera camera) {
        GL3DCameraView cameraView = ImageViewerGui.getSingletonInstance().getCameraView();
        if (cameraView != null) {
            lastCamera = camera;
            cameraView.setCurrentCamera(camera);
        }
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if (anItem instanceof GL3DCamera) {
            setCurrentCamera((GL3DCamera) anItem);
        } else {
            throw new IllegalArgumentException("Cannot set Selected Camera to an object of Type other than " + GL3DCamera.class + ". Given Object is " + anItem);
        }
    }

    public GL3DObserverCamera getObserverCamera() {
        return observerCamera;
    }

    public GL3DSolarRotationTrackingTrackballCamera getSolarRotationCamera() {
        return solarRotationCamera;
    }

    public GL3DObserverCamera getStonyHurstCamera() {
        return observerCamera;
    }

    public GL3DFollowObjectCamera getFollowObjectCamera() {
        return followObjectCamera;
    }

    public GL3DEarthCamera getEarthCamera() {
        return earthCamera;
    }

    public void addListener(GL3DCameraSelectionModelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GL3DCameraSelectionModelListener listener) {
        listeners.remove(listener);
    }

}
