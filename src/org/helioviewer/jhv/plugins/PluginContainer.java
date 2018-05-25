package org.helioviewer.jhv.plugins;

// The basic class which manages the interface between JHV and the contained
// plugin. It manages the current status of the corresponding plug-in.
class PluginContainer {

    private final Plugin plugin;
    private boolean active;

    public PluginContainer(Plugin _plugin) {
        plugin = _plugin;
        active = PluginSettings.getSingletonInstance().isPluginActivated(plugin.getClass().getSimpleName());
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

    public void setActive(boolean _active) {
        if (active != _active) {
            active = _active;
            PluginSettings.getSingletonInstance().save(this);
            if (active)
                plugin.installPlugin();
            else
                plugin.uninstallPlugin();
        }
    }

    @Override
    public String toString() {
        return plugin.getClass().getSimpleName();
    }

}
