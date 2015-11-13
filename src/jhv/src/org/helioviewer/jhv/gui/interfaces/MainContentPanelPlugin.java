package org.helioviewer.jhv.gui.interfaces;

import java.util.LinkedList;

import javax.swing.JComponent;

public interface MainContentPanelPlugin {

    public String getTabName();

    public LinkedList<JComponent> getVisualInterfaces();

}
