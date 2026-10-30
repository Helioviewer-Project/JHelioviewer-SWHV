package org.helioviewer.jhv.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;

import org.json.JSONObject;

public final class PluginManager {

    private static final List<Plugin> plugins = new ArrayList<>();
    private static boolean guiEnabled;

    public static void setGUIEnabled(boolean _guiEnabled) {
        guiEnabled = _guiEnabled;
    }

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
            plugin.activate(guiEnabled);
    }

    public static void setActive(Plugin plugin, boolean active) {
        plugin.setActive(active, guiEnabled);
    }

    public static void loadState(JSONObject jo) {
        for (Plugin plugin : plugins) {
            JSONObject po = switch (plugin) {
                case EVEPlugin _ -> jo.optJSONObject("timelines", jo.optJSONObject("EVEPlugin"));
                case SWEKPlugin _ -> jo.optJSONObject("events", jo.optJSONObject("SWEKPlugin"));
                case PfssPlugin _ -> jo.optJSONObject("pfss", jo.optJSONObject("PfssPlugin"));
                default -> null;
            };
            if (po != null)
                plugin.loadState(po);
        }
    }

    public static void saveState(JSONObject jo) {
        for (Plugin plugin : plugins) {
            String key = switch (plugin) {
                case EVEPlugin _ -> "timelines";
                case SWEKPlugin _ -> "events";
                case PfssPlugin _ -> "pfss";
                default -> throw new IllegalStateException("Unknown plugin type: " + plugin.getClass().getName());
            };
            JSONObject po = new JSONObject();
            plugin.saveState(po);
            jo.put(key, po);
        }
    }

    private PluginManager() {}
}
