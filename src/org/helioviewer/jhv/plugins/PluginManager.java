package org.helioviewer.jhv.plugins;

import java.util.Collection;
import java.util.HashMap;

import org.json.JSONObject;

public class PluginManager {

    private static final PluginManager singletonInstance = new PluginManager();

    private final HashMap<Plugin, PluginContainer> plugins = new HashMap<>();

    private PluginManager() {
    }

    public static PluginManager getSingletonInstance() {
        return singletonInstance;
    }

    public Collection<PluginContainer> getPlugins() {
        return plugins.values();
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
        PluginContainer pluginContainer = new PluginContainer(plugin, name, PluginSettings.getSingletonInstance().isPluginActivated(name));
        plugins.put(plugin, pluginContainer);
        if (pluginContainer.isActive()) {
            plugin.installPlugin();
        }
    }

    public void loadState(JSONObject jo) {
        for (String classname : jo.keySet()) {
            for (Plugin plugin : plugins.keySet()) {
                if (classname.equals(plugin.getClass().getName())) {
                    JSONObject cl = jo.optJSONObject(classname);
                    if (cl != null) {
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
