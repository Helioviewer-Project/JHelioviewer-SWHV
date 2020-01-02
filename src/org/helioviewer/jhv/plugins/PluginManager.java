package org.helioviewer.jhv.plugins;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

public class PluginManager {

    private static final HashSet<PluginContainer> plugins = new HashSet<>();

    public static Set<PluginContainer> getPlugins() {
        return Collections.unmodifiableSet(plugins);
    }

    public static void addPlugin(Plugin plugin) {
        plugins.add(new PluginContainer(plugin));
    }

    public static void loadState(JSONObject jo) {
        for (String name : jo.keySet()) {
            for (PluginContainer plugin : plugins) {
                if (name.equals(plugin.toString())) {
                    JSONObject po = jo.optJSONObject(name);
                    if (po != null) {
                        plugin.loadState(po);
                    }
                }
            }
        }
    }

    public static void saveState(JSONObject jo) {
        for (PluginContainer plugin : plugins) {
            JSONObject po = new JSONObject();
            plugin.saveState(po);
            jo.put(plugin.toString(), po);
        }
    }

}
