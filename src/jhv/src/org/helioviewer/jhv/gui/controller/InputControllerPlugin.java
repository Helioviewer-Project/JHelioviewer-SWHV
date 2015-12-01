package org.helioviewer.jhv.gui.controller;

import java.awt.Component;

import org.helioviewer.jhv.camera.Camera;

public interface InputControllerPlugin {

    public void setCamera(Camera camera);

    public void setComponent(Component component);

}
