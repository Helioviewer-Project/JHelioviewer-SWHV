package org.helioviewer.gl3d.plugin;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;

/**
 * The Plugin Controller is responible for loading and unloading 3D plugins. Add
 * a GL3DPluginListener to be informed about changes in loaded plugins.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DPluginController {
    private static GL3DPluginController instance = new GL3DPluginController();

    private List<GL3DPluginListener> listeners = new ArrayList<GL3DPluginListener>();

    private GL3DPluginConfiguration configuration;

    private List<GL3DModelPlugin> plugins = new ArrayList<GL3DModelPlugin>();

    public static GL3DPluginController getInstance() {
        return GL3DPluginController.instance;
    }

    public void loadPlugins() {
        this.plugins.clear();

        Log.debug("GL3DPluginController.loadPlugins()...");
        List<GL3DModelPlugin> pluginsToLoad = configuration.findPlugins();

        for (GL3DModelPlugin plugin : pluginsToLoad) {
            loadPlugin(plugin);
        }
    }

    public void loadPlugin(GL3DModelPlugin plugin) {
        plugin.load();
        this.plugins.add(plugin);
        firePluginLoaded(plugin);
    }

    public void setPluginConfiguration(GL3DPluginConfiguration configuration) {
        this.configuration = configuration;
    }

    public int getPluginCount() {
        return this.plugins.size();
    }

    public GL3DModelPlugin getPlugin(int index) {
        return this.plugins.get(index);
    }

    public void removePlugin(GL3DModelPlugin plugin) {
        plugin.unload();
        this.plugins.remove(plugin);
        firePluginUnloaded(plugin);
    }

    public void deactivatePlugin(GL3DModelPlugin plugin) {
        plugin.getPluginRootNode().getDrawBits().on(Bit.Hidden);
    }

    public void activatePlugin(GL3DModelPlugin plugin) {
        plugin.getPluginRootNode().getDrawBits().off(Bit.Hidden);
    }

    public void addPluginListener(GL3DPluginListener listener) {
        this.listeners.add(listener);
    }

    public void removePluginListener(GL3DPluginListener listener) {
        this.listeners.remove(listener);
    }

    private void firePluginLoaded(GL3DModelPlugin plugin) {
        for (GL3DPluginListener listener : this.listeners) {
            listener.pluginLoaded(plugin);
        }
    }

    private void firePluginUnloaded(GL3DModelPlugin plugin) {
        for (GL3DPluginListener listener : this.listeners) {
            listener.pluginUnloaded(plugin);
        }
    }

}
