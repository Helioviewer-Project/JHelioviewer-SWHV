package org.helioviewer.gl3d.plugin;

/**
 * A plugin listener is informed about loading and unloading of a specific
 * plugin
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public interface GL3DPluginListener {

    public void pluginLoaded(GL3DModelPlugin plugin);

    public void pluginUnloaded(GL3DModelPlugin plugin);
}