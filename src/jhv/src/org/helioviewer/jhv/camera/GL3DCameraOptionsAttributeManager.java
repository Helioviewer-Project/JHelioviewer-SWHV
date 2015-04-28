package org.helioviewer.jhv.camera;

public class GL3DCameraOptionsAttributeManager {

    private static GL3DCameraOptionsAttributeManager manager = new GL3DCameraOptionsAttributeManager();
    private GL3DEarthCameraOptionPanel earthCameraOptionPanel;
    private GL3DFollowObjectCameraOptionPanel followObjectCameraOptionPanel;
    private GL3DObserverCameraOptionPanel observerCameraOptionPanel;

    private GL3DCameraOptionsAttributeManager() {
    }

    public static GL3DCameraOptionsAttributeManager getSingletonInstance() {
        return manager;
    }

    public GL3DCameraOptionPanel getCameraOptionAttributePanel(GL3DCamera camera) {
        if (camera instanceof GL3DEarthCamera) {
            if (earthCameraOptionPanel == null)
                earthCameraOptionPanel = new GL3DEarthCameraOptionPanel((GL3DEarthCamera) camera);
            return earthCameraOptionPanel;
        } else if (camera instanceof GL3DFollowObjectCamera) {
            if (followObjectCameraOptionPanel == null)
                followObjectCameraOptionPanel = new GL3DFollowObjectCameraOptionPanel((GL3DFollowObjectCamera) camera);
            return followObjectCameraOptionPanel;
        } else if (camera instanceof GL3DObserverCamera) {
            if (observerCameraOptionPanel == null)
                observerCameraOptionPanel = new GL3DObserverCameraOptionPanel((GL3DObserverCamera) camera);
            return observerCameraOptionPanel;
        }
        return null;
    }

}
