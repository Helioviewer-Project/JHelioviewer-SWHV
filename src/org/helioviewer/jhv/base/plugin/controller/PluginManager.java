package org.helioviewer.jhv.base.plugin.controller;

import java.util.Collection;
import java.util.HashMap;

import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.json.JSONObject;

/**
 * This class is responsible to manage all plug-ins for JHV. It loads available
 * plug-ins and provides methods to access the loaded plug-ins.
 *
 * @author Stephan Pagel
 */
public class PluginManager {

    private static final PluginManager singletonInstance = new PluginManager();

    private final PluginSettings pluginSettings = PluginSettings.getSingletonInstance();
    private final HashMap<Plugin, PluginContainer> plugins = new HashMap<>();

    private PluginManager() {
    }

    public static PluginManager getSingletonInstance() {
        return singletonInstance;
    }

    /**
     * Loads the saved settings from the corresponding file.
     *
     * @param settingsFilePath
     *            Path of the directory where the plug-in settings file is
     *            saved.
     */
    public void loadSettings(String settingsFilePath) {
        pluginSettings.loadPluginSettings(settingsFilePath);
    }

    /**
     * Saves the settings of all loaded plug-ins to a file. The file will be
     * saved in the directory which was specified in
     * {@link #loadSettings(String)}.
     */
    public void saveSettings() {
        pluginSettings.savePluginSettings();
    }

    /**
     * Returns a list with all loaded plug-ins.
     *
     * @return a list with all loaded plug-ins.
     */
    public PluginContainer[] getAllPlugins() {
        Collection<PluginContainer> col = plugins.values();
        return col.toArray(new PluginContainer[col.size()]);
    }

    /**
     * Adds a plug-in to the list of all loaded plug-ins. By default a plug-in
     * is not activated. If there is a plug-in entry in the plug-in settings
     * file the status of the plug-in will be set to this value.
     *
     * @param plugin
     *            Plug-in to add to the list.
     * @param name
     */
    public void addPlugin(Plugin plugin, String name) {
        PluginContainer pluginContainer = new PluginContainer(plugin, name, pluginSettings.isPluginActivated(name));
        plugins.put(plugin, pluginContainer);
        if (pluginContainer.isActive()) {
            plugin.installPlugin();
        }
    }

    public void loadState(JSONObject jo) {
        for (String classname : jo.keySet()) {
            for (Plugin plugin : plugins.keySet()) {
                System.out.println(classname + " " + plugin.getClass().getName());
                if (classname.equals(plugin.getClass().getName())) {
                    JSONObject cl = jo.optJSONObject(classname);
                    if (cl != null) {
                        System.out.println(cl);
                        plugin.loadState(cl);
                    }
                }
            }
        }

    }

    public void saveState(JSONObject jo) {
        for (Plugin plugin : plugins.keySet()) {
            JSONObject swekObject = new JSONObject();
            plugin.saveState(swekObject);
            jo.put(plugin.getClass().getName(), swekObject);
        }
    }

}
