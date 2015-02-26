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
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.opengl.GL3DCameraView;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;

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
    ArrayList<GL3DCameraSelectionModelListener> listeners = new ArrayList<GL3DCameraSelectionModelListener>();

    public static GL3DCameraSelectorModel getInstance() {
        if (instance == null) {
            instance = new GL3DCameraSelectorModel();
        }
        return instance;
    }

    private GL3DCameraSelectorModel() {
    }

    public synchronized void activate(GL3DSceneGraphView sceneGraphView) {
        if (sceneGraphView != null) {
            if (earthCamera == null) {
                earthCamera = new GL3DEarthCamera(sceneGraphView);
                observerCamera = new GL3DObserverCamera(sceneGraphView);
                followObjectCamera = new GL3DFollowObjectCamera(sceneGraphView);
                defaultCamera = observerCamera;
                lastCamera = defaultCamera;
                cameras.add(earthCamera);
                cameras.add(observerCamera);
                cameras.add(followObjectCamera);

                defaultCamera = observerCamera;
            } else {
                earthCamera.createNewGrid(sceneGraphView);
                earthCamera.setSceneGraphView(sceneGraphView);
                observerCamera.createNewGrid(sceneGraphView);
                observerCamera.setSceneGraphView(sceneGraphView);

                followObjectCamera.createNewGrid(sceneGraphView);
                followObjectCamera.createNewFOV(sceneGraphView);
                followObjectCamera.setSceneGraphView(sceneGraphView);

                earthCamera.getGrid().getDrawBits().set(Bit.Hidden, true);
                earthCamera.getFollowGrid().getDrawBits().set(Bit.Hidden, true);
                observerCamera.getGrid().getDrawBits().set(Bit.Hidden, true);
                observerCamera.getFollowGrid().getDrawBits().set(Bit.Hidden, true);
                followObjectCamera.getGrid().getDrawBits().set(Bit.Hidden, true);
                followObjectCamera.getFollowGrid().getDrawBits().set(Bit.Hidden, true);

                setCurrentCamera(observerCamera);
            }
            if (lastCamera != null) {
                setCurrentCamera(lastCamera);
            } else {
                setCurrentCamera(defaultCamera);
            }
        }

        this.fireInit();
    }

    private void fireInit() {
        synchronized (listeners) {
            for (GL3DCameraSelectionModelListener listener : listeners) {
                listener.fireInit();
            }
        }
    }

    public GL3DCamera getCurrentCamera() {
        GL3DCameraView cameraView = getCameraView();
        if (cameraView != null)
            return cameraView.getCurrentCamera();
        return null;
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
        GL3DCameraView cameraView = getCameraView();
        if (cameraView != null) {
            return cameraView.getCurrentCamera();
        }
        return null;
    }

    public void setCurrentCamera(GL3DCamera camera) {
        GL3DCameraView cameraView = getCameraView();
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

    private GL3DCameraView getCameraView() {
        ImageViewerGui imageViewer = ImageViewerGui.getSingletonInstance();
        if (imageViewer == null)
            return null;

        ComponentView mainView = imageViewer.getMainView();
        if (mainView != null) {
            return mainView.getAdapter(GL3DCameraView.class);
        }

        return null;
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
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(GL3DCameraSelectionModelListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

}
