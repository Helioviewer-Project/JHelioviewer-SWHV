package org.helioviewer.jhv.plugins;

import java.util.Collection;
import java.util.HashMap;

import org.json.JSONObject;

public class PluginManager {

    private static final HashMap<Plugin, PluginContainer> plugins = new HashMap<>();

    public static Collection<PluginContainer> getPlugins() {
        return plugins.values();
    }

    public static void addPlugin(Plugin plugin) {
        plugins.put(plugin, new PluginContainer(plugin));
    }

    public static void loadState(JSONObject jo) {
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

    public static void saveState(JSONObject jo) {
        for (Plugin plugin : plugins.keySet()) {
            JSONObject po = new JSONObject();
            plugin.saveState(po);
            jo.put(plugin.getClass().getName(), po);
        }
    }

}
