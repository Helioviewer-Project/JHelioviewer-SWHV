package org.helioviewer.gl3d.plugin;

import javax.swing.JComponent;

import org.helioviewer.gl3d.scenegraph.GL3DGroup;

/**
 * Every 3D plugin must implement this interface.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public interface GL3DModelPlugin {
    public void load();

    public void unload();

    public GL3DGroup getPluginRootNode();

    public String getPluginName();

    public String getPluginDescription();

    public boolean isActive();

    public void setActive(boolean active);

    /**
     * UI Component that lets the user configure this Plugin (including File
     * lists and File load dialogs etc.)
     * 
     * @return UI Configuration Component
     */
    public JComponent getConfigurationComponent();

    public void addModelListener(GL3DModelListener listener);

    public void removeModelListener(GL3DModelListener listener);
}
