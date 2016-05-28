package org.helioviewer.jhv.gui.controller;

import java.awt.Component;

import org.helioviewer.jhv.camera.Camera;

public interface InputControllerPlugin {

    void setCamera(Camera camera);

    void setComponent(Component component);

}
