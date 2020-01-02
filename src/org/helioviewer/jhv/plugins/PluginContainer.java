package org.helioviewer.jhv.plugins;

import org.helioviewer.jhv.Settings;
import org.json.JSONObject;

public class PluginContainer {

    private final Plugin plugin;
    private boolean active;

    PluginContainer(Plugin _plugin) {
        plugin = _plugin;
        active = getActive();
        if (active)
            plugin.installPlugin();
    }

    void saveState(JSONObject jo) {
        plugin.saveState(jo);
    }

    void loadState(JSONObject jo) {
        plugin.loadState(jo);
    }

    public String getName() {
        return plugin.getName();
    }

    public String getDescription() {
        return plugin.getDescription();
    }

    public boolean isActive() {
        return active;
    }

    private boolean getActive() {
        String p = Settings.getProperty("plugins." + this + ".active");
        return p == null || Boolean.parseBoolean(p);
    }

    public void toggleActive() {
        active = !active;
        Settings.setProperty("plugins." + this + ".active", Boolean.toString(active));
        if (active)
            plugin.installPlugin();
        else
            plugin.uninstallPlugin();
    }

    @Override
    public String toString() {
        return plugin.getClass().getSimpleName();
    }

}
