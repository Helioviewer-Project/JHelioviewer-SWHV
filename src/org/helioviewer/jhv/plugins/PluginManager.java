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

    public void addPlugin(Plugin plugin) {
        plugins.put(plugin, new PluginContainer(plugin));
    }

    public void loadState(JSONObject jo) {
        for (String classname : jo.keySet()) {
            for (Plugin plugin : plugins.keySet()) {
                if (classname.equals(plugin.getClass().getName())) {
                    JSONObject po = jo.optJSONObject(classname);
                    if (po != null) {
                        plugin.loadState(po);
                    }
                }
            }
        }
    }

    public void saveState(JSONObject jo) {
        for (Plugin plugin : plugins.keySet()) {
            JSONObject po = new JSONObject();
            plugin.saveState(po);
            jo.put(plugin.getClass().getName(), po);
        }
    }

}
