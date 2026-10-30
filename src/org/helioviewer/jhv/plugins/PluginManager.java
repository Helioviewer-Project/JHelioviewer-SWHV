package org.helioviewer.jhv.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.helioviewer.jhv.app.Log;

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

    public static <T extends Plugin> T getPlugin(Class<T> type) {
        for (Plugin plugin : plugins) {
            if (plugin.getClass() == type)
                return type.cast(plugin);
        }
        throw new IllegalStateException("Plugin is not registered: " + type.getName());
    }

    public static void addPlugin(Plugin plugin) {
        for (Plugin existing : plugins) {
            if (existing.getClass() == plugin.getClass())
                return;
        }
        plugins.add(plugin);
        try {
            plugin.start(guiEnabled);
        } catch (RuntimeException e) {
            Log.warn("Failed to start " + plugin.getName(), e);
        }
    }

    public static void setActive(Plugin plugin, boolean active) {
        plugin.setActive(active, guiEnabled);
    }

    public static void loadState(JSONObject jo) {
        for (Plugin plugin : plugins)
            plugin.loadState(jo);
    }

    public static void saveState(JSONObject jo) {
        for (Plugin plugin : plugins)
            plugin.saveState(jo);
    }

    private PluginManager() {}
}
