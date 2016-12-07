package org.helioviewer.jhv.base.plugin.controller;

import java.net.URI;

import org.helioviewer.jhv.base.plugin.interfaces.Container;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;

/**
 * The basic class which manages the interface between JHV and the contained
 * plugin. It manages the current status of the corresponding plug-in.
 */
public class PluginContainer implements Container {

    private final Plugin plugin;
    private final URI pluginLocation;
    private boolean pluginActive;

    public PluginContainer(Plugin _plugin, URI _pluginLocation, boolean _pluginActive) {
        plugin = _plugin;
        pluginLocation = _pluginLocation;
        pluginActive = _pluginActive;
    }

    @Override
    public String getName() {
        return plugin.getName();
    }

    @Override
    public String getDescription() {
        return plugin.getDescription();
    }

    @Override
    public boolean isActive() {
        return pluginActive;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the new active status is the same as the current status the method
     * will do nothing. Otherwise it installs the plug-in if the new status is
     * true or uninstalls the plug-in if the new status is false.
     */
    @Override
    public void setActive(boolean active) {
        if (pluginActive == active)
            return;

        pluginActive = active;
        if (active)
            plugin.installPlugin();
        else
            plugin.uninstallPlugin();
    }

    @Override
    public void changeSettings() {
        PluginSettings.getSingletonInstance().pluginSettingsToXML(this);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public URI getPluginLocation() {
        return pluginLocation;
    }

    @Override
    public String toString() {
        return plugin.getName();
    }

}
