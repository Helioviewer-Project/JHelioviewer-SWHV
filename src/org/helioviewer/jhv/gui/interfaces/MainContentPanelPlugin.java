package org.helioviewer.jhv.gui.interfaces;

import java.util.List;

import javax.swing.JComponent;

public interface MainContentPanelPlugin {

    String getTabName();

    List<JComponent> getVisualInterfaces();

}
