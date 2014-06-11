package org.helioviewer.gl3d.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DFixedTimeCamera;
import org.helioviewer.gl3d.camera.GL3DSolarOrbiterCamera;
import org.helioviewer.gl3d.camera.GL3DSolarRotationTrackingTrackballCamera;
import org.helioviewer.gl3d.camera.GL3DTrackballCamera;
import org.helioviewer.gl3d.camera.GL3DTrackballStonyhurstCamera;
import org.helioviewer.gl3d.view.GL3DCameraView;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.viewmodel.view.ComponentView;

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

    private GL3DTrackballCamera trackballCamera;

    private GL3DSolarRotationTrackingTrackballCamera solarRotationCamera;

    private GL3DFixedTimeCamera fixedTimeCamera;

    private GL3DTrackballStonyhurstCamera stonyHurstCamera;

    private GL3DSolarOrbiterCamera solarOrbiterCamera;

    public static GL3DCameraSelectorModel getInstance() {
        if (instance == null) {
            instance = new GL3DCameraSelectorModel();
        }
        return instance;
    }

    private GL3DCameraSelectorModel() {
        // StateController.getInstance().addStateChangeListener(new
        // StateChangeListener() {
        //
        // public void stateChanged(State newState, State oldState,
        // StateController stateController) {
        // if(newState.getType()==ViewStateEnum.View3D) {
        // //Needs to be checked, because if new State is 2D no CameraView is
        // available.
        //
        // } else {
        // Log.info("GL3DCameraSelectorModel: No camera change, no GL3DSceneGraphView available");
        // }
        // }
        // });
    }

    public void activate(GL3DSceneGraphView sceneGraphView) {
        // GL3DSceneGraphView sceneGraphView =
        // getMainView().getAdapter(GL3DSceneGraphView.class);

        if (sceneGraphView != null) {
            trackballCamera = new GL3DTrackballCamera(sceneGraphView);
            fixedTimeCamera = new GL3DFixedTimeCamera(sceneGraphView);
            stonyHurstCamera = new GL3DTrackballStonyhurstCamera(sceneGraphView);
            solarRotationCamera = new GL3DSolarRotationTrackingTrackballCamera(sceneGraphView);
            setSolarOrbiterCamera(new GL3DSolarOrbiterCamera(sceneGraphView));

            defaultCamera = trackballCamera;
            lastCamera = defaultCamera;
            cameras.add(trackballCamera);
            cameras.add(solarRotationCamera);
            defaultCamera = trackballCamera;

            if (getCameraView() != null) {
                setCurrentCamera(lastCamera);
            } else {
                Log.warn("Cannot set Current Camera, no GL3DCameraView yet!");
            }
        }
        getCameraView().setCurrentCamera(defaultCamera);
    }

    public GL3DCamera getCurrentCamera() {
        return getCameraView().getCurrentCamera();
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
        return getCameraView().getCurrentCamera();
    }

    public void setCurrentCamera(GL3DCamera camera) {
        lastCamera = camera;
        System.out.println(camera);
        getCameraView().setCurrentCamera(camera);
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if (anItem instanceof GL3DCamera) {
            setCurrentCamera((GL3DCamera) anItem);
        } else {
            throw new IllegalArgumentException("Cannot set Selected Camera to an object of Type other than " + GL3DCamera.class + ". Given Object is " + anItem);
        }
    }

    private ComponentView getMainView() {
        ImageViewerGui imageViewer = ImageViewerGui.getSingletonInstance();
        if (imageViewer == null)
            return null;
        return imageViewer.getMainView();
    }

    private GL3DCameraView getCameraView() {
        ComponentView mainView = getMainView();
        if (mainView != null) {
            return mainView.getAdapter(GL3DCameraView.class);
        }
        return null;
    }

    public GL3DTrackballCamera getTrackballCamera() {
        return trackballCamera;
    }

    public GL3DSolarRotationTrackingTrackballCamera getSolarRotationCamera() {
        return solarRotationCamera;
    }

    public GL3DFixedTimeCamera getFixedTimeCamera() {
        return fixedTimeCamera;
    }

    public GL3DTrackballStonyhurstCamera getStonyHurstCamera() {
        return stonyHurstCamera;
    }

    public GL3DSolarOrbiterCamera getSolarOrbiterCamera() {
        return solarOrbiterCamera;
    }

    public void setSolarOrbiterCamera(GL3DSolarOrbiterCamera solarOrbiterCamera) {
        this.solarOrbiterCamera = solarOrbiterCamera;
    }

}