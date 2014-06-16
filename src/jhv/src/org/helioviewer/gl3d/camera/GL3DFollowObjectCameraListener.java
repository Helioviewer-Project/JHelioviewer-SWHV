package org.helioviewer.gl3d.camera;

import java.util.Date;

public interface GL3DFollowObjectCameraListener {
    public void fireLoaded(boolean isLoaded);

    public void fireCameraTime(Date cameraDate);

    public void fireNewDate(Date date);
}
