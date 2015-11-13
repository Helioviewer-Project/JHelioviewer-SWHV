package org.helioviewer.jhv.gui.interfaces;

import java.awt.Component;

import org.helioviewer.jhv.camera.Camera;

public interface InputControllerPlugin {

    public void setCamera(Camera camera);

    public void setComponent(Component component);

}
