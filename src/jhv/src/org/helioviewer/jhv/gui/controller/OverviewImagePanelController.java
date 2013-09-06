package org.helioviewer.jhv.gui.controller;

import java.awt.Rectangle;

import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;

public interface OverviewImagePanelController extends ImagePanelInputController {
    public void setImageArea(Rectangle imageArea);

    public void setROIArea(Rectangle roiArea);
}
