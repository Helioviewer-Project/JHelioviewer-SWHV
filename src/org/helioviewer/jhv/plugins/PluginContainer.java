package org.helioviewer.jhv.plugins;

import org.helioviewer.jhv.Settings;

// The basic class which manages the interface between JHV and the contained
// plugin. It manages the current status of the corresponding plug-in.
class PluginContainer {

    private final Plugin plugin;
    private boolean active;

    PluginContainer(Plugin _plugin) {
        plugin = _plugin;
        active = getActive();
        if (active)
            plugin.installPlugin();
    }

    public String getName() {
        return plugin.getName();
    }

    public String getDescription() {
        return plugin.getDescription();
    }

    public String getAboutLicenseText() {
        return plugin.getAboutLicenseText();
    }

    public boolean isActive() {
        return active;
    }

    private boolean getActive() {
        String p = Settings.getProperty("plugins." + this + ".active");
        return p == null ? true : Boolean.parseBoolean(p);
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
