package org.helioviewer.jhv.gui.interfaces;

import java.util.LinkedList;

import javax.swing.JComponent;

public interface MainContentPanelPlugin {

    String getTabName();

    LinkedList<JComponent> getVisualInterfaces();

}
