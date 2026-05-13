package org.helioviewer.jhv.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

public class PluginManager {

    private static final List<Plugin> plugins = new ArrayList<>();

    public static List<Plugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public static boolean isActive(Class<? extends Plugin> type) {
        for (Plugin plugin : plugins) {
            if (plugin.getClass() == type)
                return plugin.isActive();
        }
        return false;
    }

    public static void addPlugin(Plugin plugin) {
        for (Plugin existing : plugins) {
            if (existing.getClass() == plugin.getClass())
                return;
        }
        plugins.add(plugin);
        if (plugin.isActive())
            plugin.install();
    }

    public static void loadState(JSONObject jo) {
        for (Plugin plugin : plugins) {
            JSONObject po = jo.optJSONObject(plugin.toString());
            if (po != null)
                plugin.loadState(po);
        }
    }

    public static void saveState(JSONObject jo) {
        for (Plugin plugin : plugins) {
            JSONObject po = new JSONObject();
            plugin.saveState(po);
            jo.put(plugin.toString(), po);
        }
    }

}
