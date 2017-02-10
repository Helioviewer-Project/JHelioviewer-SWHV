package org.helioviewer.jhv.base.plugin.controller;

import java.net.URI;

import org.helioviewer.jhv.base.plugin.interfaces.Plugin;

// The basic class which manages the interface between JHV and the contained
// plugin. It manages the current status of the corresponding plug-in.
public class PluginContainer {

    private final Plugin plugin;
    private final URI pluginLocation;
    private boolean pluginActive;

    public PluginContainer(Plugin _plugin, URI _pluginLocation, boolean _pluginActive) {
        plugin = _plugin;
        pluginLocation = _pluginLocation;
        pluginActive = _pluginActive;
    }

    public String getName() {
        return plugin.getName();
    }

    public String getDescription() {
        return plugin.getDescription();
    }

    public boolean isActive() {
        return pluginActive;
    }

    public void setActive(boolean active) {
        if (pluginActive == active)
            return;

        pluginActive = active;
        if (active)
            plugin.installPlugin();
        else
            plugin.uninstallPlugin();
    }

    public void changeSettings() {
        PluginSettings.getSingletonInstance().pluginSettingsToXML(this);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public URI getPluginLocation() {
        return pluginLocation;
    }

    public String toString() {
        return plugin.getName();
    }

}
