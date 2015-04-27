package org.helioviewer.jhv.gui.interfaces;

import java.awt.Component;

import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.View;

public interface ImagePanelPlugin {

    public void setView(ComponentView newView);

    public ComponentView getView();

    public void setImagePanel(Component newImagePanel);

    public Component getImagePanel();

    public void detach();

}
