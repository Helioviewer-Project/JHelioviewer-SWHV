package org.helioviewer.gl3d.camera;

public class GL3DCameraOptionsAttributeManager {

    private static GL3DCameraOptionsAttributeManager manager = new GL3DCameraOptionsAttributeManager();

    private GL3DCameraOptionsAttributeManager() {
    }

    public static GL3DCameraOptionsAttributeManager getSingletonInstance() {
        return manager;
    }

    public GL3DCameraOptionPanel getCameraOptionAttributePanel(GL3DCamera camera) {
        if (camera instanceof GL3DTrackballCamera) {
            return new GL3DTrackBallCameraOptionPanel((GL3DTrackballCamera) camera);
        } else if (camera instanceof GL3DFixedTimeCamera) {
            return new GL3DFixedTimeCameraOptionPanel((GL3DFixedTimeCamera) camera);

        } else if (camera instanceof GL3DFollowObjectCamera) {
            return new GL3DFollowObjectCameraOptionPanel((GL3DFollowObjectCamera) camera);
        } else if (camera instanceof GL3DSolarRotationTrackingTrackballCamera) {
            return new GL3DSolarRotationTrackingTrackballCameraOptionPanel((GL3DSolarRotationTrackingTrackballCamera) camera);
        }
        return new GL3DCameraOptionPanel();
    }
}
