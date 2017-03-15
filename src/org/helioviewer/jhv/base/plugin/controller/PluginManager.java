package org.helioviewer.jhv.base.plugin.controller;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;

/**
 * This class is responsible to manage all plug-ins for JHV. It loads available
 * plug-ins and provides methods to access the loaded plug-ins.
 *
 * @author Stephan Pagel
 */
public class PluginManager {

    private static final PluginManager singletonInstance = new PluginManager();

    private final PluginSettings pluginSettings = PluginSettings.getSingletonInstance();
    private final HashMap<Plugin, PluginContainer> plugins = new HashMap<>();

    private PluginManager() {
    }

    public static PluginManager getSingletonInstance() {
        return singletonInstance;
    }

    /**
     * Loads the saved settings from the corresponding file.
     *
     * @param settingsFilePath
     *            Path of the directory where the plug-in settings file is
     *            saved.
     */
    public void loadSettings(String settingsFilePath) {
        pluginSettings.loadPluginSettings(settingsFilePath);
    }

    /**
     * Saves the settings of all loaded plug-ins to a file. The file will be
     * saved in the directory which was specified in
     * {@link #loadSettings(String)}.
     */
    public void saveSettings() {
        pluginSettings.savePluginSettings();
    }

    /**
     * Returns a list with all loaded plug-ins.
     *
     * @return a list with all loaded plug-ins.
     */
    public PluginContainer[] getAllPlugins() {
        Collection<PluginContainer> col = plugins.values();
        return col.toArray(new PluginContainer[col.size()]);
    }

    /**
     * Tries to open the given file and load the expected plug-in.
     *
     * @param pluginLocation
     *            Specifies the location of the file which contains the plug-in.
     */
    public void loadPlugin(URI pluginLocation) {
        Log.info("PluginManager is trying to load the plugin located at " + pluginLocation);

        File file = new File(pluginLocation);
        try (JarFile jarFile = new JarFile(file)) {
            Manifest manifest = jarFile.getManifest();

            String className = null;
            if (manifest != null) {
                className = manifest.getMainAttributes().getValue("Main-Class");
            }

            if (className == null) {
                String name = file.getName().substring(0, file.getName().length() - 4);
                className = "org.helioviewer.plugins." + name + '.' + name;
                Log.debug("No Manifest Information Found, Fallback: Main-Class: " + className);
            }

            URL[] urls = new URL[1];
            urls[0] = pluginLocation.toURL();

            URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
            Object obj = classLoader.loadClass(className).getConstructor().newInstance();
            // Log.info("PluginManager: Load plugin class: " + className);
            if (obj instanceof Plugin) {
                addPlugin((Plugin) obj, file.getName());
            } else {
                Log.debug("Load failed, was trying to load something that is not a plugin: " + className);
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException | IOException e) {
            Log.error("PluginManager.loadPlugin(" + pluginLocation + ") > Error loading plugin:", e);
        }
    }

    /**
     * Adds a plug-in to the list of all loaded plug-ins. By default a plug-in
     * is not activated. If there is a plug-in entry in the plug-in settings
     * file the status of the plug-in will be set to this value.
     *
     * @param plugin
     *            Plug-in to add to the list.
     * @param pluginLocation
     *            Location of the corresponding file of the plug-in.
     */
    public void addPlugin(Plugin plugin, String jarName) {
        PluginContainer pluginContainer = new PluginContainer(plugin, jarName, pluginSettings.isPluginActivated(jarName));
        plugins.put(plugin, pluginContainer);
        if (pluginContainer.isActive()) {
            plugin.installPlugin();
        }
    }

}
