package org.helioviewer.jhv.plugin.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.plugin.interfaces.Plugin;

/**
 * This class is responsible to manage all plug-ins for JHV. It loads available
 * plug-ins and provides methods to access the loaded plug-ins.
 *
 * @author Stephan Pagel
 */
public class PluginManager {

    private static PluginManager singletonInstance = new PluginManager();

    private final PluginSettings pluginSettings = PluginSettings.getSingletonInstance();
    private final Map<Plugin, PluginContainer> plugins = new HashMap<Plugin, PluginContainer>();

    private PluginManager() {
    }

    /**
     * Method returns the sole instance of this class.
     *
     * @return the only instance of this class.
     * */
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
     * Method searches for files which contains JHV plug-ins and loads the
     * plug-in if applicable.
     *
     * @param file
     *            File object where to search in (usually it describes a
     *            folder).
     * @param recursively
     *            Specifies if to search for plug-ins recursively in sub
     *            folders.
     * @param deactivatedPlugins
     *            Set with file names of plugins which should be deactivated
     * @throws IOException
     *             This exception will be thrown if at least one plug-in could
     *             not be loaded. The relevant plug-ins are named in the
     *             exception message.
     */
    public void searchForPlugins(File file, boolean recursively, Set<String> deactivatedPlugins) throws IOException {
        // search and load plug-ins
        LinkedList<String> list = searchAndLoadPlugins(file, recursively, deactivatedPlugins);

        // if there is at least one plug-in which could not be loaded throw an
        // exception and add plug-in names to the message of the exception.
        if (list.size() > 0) {
            String message = "";
            for (String item : list) {
                message += item + "\n";
            }
            throw new IOException(message);
        }
    }

    /**
     * Method searches for files which contains JHV plug-ins and loads the
     * plug-in if applicable.
     *
     * @param file
     *            File object where to search in (usually it describes a
     *            folder).
     * @param recursivly
     *            Specifies if to search for plug-ins recursively in sub
     *            folders.
     * @param deactivedPlugins
     *            Set with file names of plugins which should be deactivated
     * @return list with file names where the plug-in could not be loaded.
     */
    private LinkedList<String> searchAndLoadPlugins(File file, boolean recursivly, Set<String> deactivedPlugins) {
        LinkedList<String> result = new LinkedList<String>();
        File[] files = file.listFiles();

        for (File f : files) {
            if (f.isDirectory()) {
                result.addAll(searchAndLoadPlugins(f, recursivly, deactivedPlugins));
            } else if (f.isFile() && f.getName().toLowerCase().endsWith(".jar") && !deactivedPlugins.contains(f.getName())) {

                Log.debug("Found Plugin Jar File: " + f.toURI());

                try {
                    if (!loadPlugin(f.toURI())) {
                        result.add(f.getPath());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return result;
    }

    /**
     * Returns a list with all loaded plug-ins.
     *
     * @return a list with all loaded plug-ins.
     */
    public PluginContainer[] getAllPlugins() {
        return plugins.values().toArray(new PluginContainer[0]);
    }

    /**
     *
     * @param plugin
     *            A loaded plugin
     * @return The corresponding plugin container or null if the plugin was not
     *         loaded
     */
    public PluginContainer getPluginContainer(Plugin plugin) {
        return plugins.get(plugin);
    }

    /**
     * Returns a list with all plug-ins which have the passed active status. If
     * the active status is true all activated plug-ins will be returned
     * otherwise all available and not activated plug-ins will be returned.
     *
     * @param activated
     *            Indicates if all available (false) or all activated (true)
     *            plug-ins have to be returned.
     * @return list with all plug-ins which have the passed active status.
     */
    public AbstractList<PluginContainer> getPlugins(boolean activated) {
        AbstractList<PluginContainer> result = new LinkedList<PluginContainer>();

        for (PluginContainer container : plugins.values()) {
            if (container.isActive() == activated) {
                result.add(container);
            }
        }

        return result;
    }

    /**
     * Tries to open the given file and load the expected plug-in.
     *
     * @param pluginLocation
     *            Specifies the location of the file which contains the plug-in.
     * @return true if the plug-in could be loaded successfully; false
     *         otherwise.
     */
    public boolean loadPlugin(URI pluginLocation) {
        URL[] urls = new URL[1];
        try {
            urls[0] = pluginLocation.toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Log.info("PluginManager is trying to load the plugin located at " + pluginLocation);

        File file = new File(pluginLocation);
        try {
            JarFile jarFile = new JarFile(file);
            Manifest manifest = jarFile.getManifest();

            String className = null;
            if (manifest != null) {
                className = manifest.getMainAttributes().getValue("Main-Class");
                Log.debug("Found Manifest: Main-Class:" + className);
            }

            if (className == null) {
                String name = file.getName().substring(0, file.getName().length() - 4);
                className = "org.helioviewer.plugins." + name + "." + name;
                Log.debug("No Manifest Information Found, Fallback: Main-Class:" + className);
            }

            URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
            Log.info("PluginManager: Load plugin class :" + className);
            Object o = classLoader.loadClass(className).newInstance();

            jarFile.close();

            if (o instanceof Plugin) {
                addPlugin(classLoader, (Plugin) o, pluginLocation);
                return true;
            } else {
                Log.debug("Failed trying to load something that is not a plugin " + className);
            }

        } catch (InstantiationException e) {
            Log.error(">> PluginManager.loadPlugin(" + pluginLocation + ") > Error loading plugin:", e);
        } catch (IllegalAccessException e) {
            Log.error(">> PluginManager.loadPlugin(" + pluginLocation + ") > Error loading plugin:", e);
        } catch (ClassNotFoundException e) {
            Log.error(">> PluginManager.loadPlugin(" + pluginLocation + ") > Error loading plugin:", e);
        } catch (IOException e) {
            Log.error(">> PluginManager.loadPlugin(" + pluginLocation + ") > Error loading plugin:", e);
        }

        return false;
    }

    /**
     * Adds an internal plug-in to the list of all loaded plug-ins. Internal
     * plug-ins are installed and activated by default.
     *
     * @param classLoader
     *            The class loader used to load the plugin classes
     * @param plugin
     *            internal plug-in to add to the list of all loaded plug-ins.
     */
    public void addInternalPlugin(ClassLoader classLoader, Plugin plugin) {
        try {
            PluginContainer pluginContainer = new PluginContainer(classLoader, plugin, new URI("internal"), true);
            plugins.put(plugin, pluginContainer);
            PluginSettings.getSingletonInstance().pluginSettingsToXML(pluginContainer);
            plugin.installPlugin();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a plug-in to the list of all loaded plug-ins. By default a plug-in
     * is not activated. If there is a plug-in entry in the plug-in settings
     * file the status of the plug-in will be set to this value.
     *
     * @param classLoader
     *            The class loader used to load the plugin classes
     * @param plugin
     *            Plug-in to add to the list.
     * @param pluginLocation
     *            Location of the corresponding file of the plug-in.
     */
    public void addPlugin(ClassLoader classLoader, Plugin plugin, URI pluginLocation) {
        PluginContainer pluginContainer = new PluginContainer(classLoader, plugin, pluginLocation, pluginSettings.isPluginActivated(pluginLocation));
        plugins.put(plugin, pluginContainer);
        if (pluginContainer.isActive()) {
            plugin.installPlugin();
        }
    }

    /**
     * Removes a container with a plug-in from the list of all plug-ins.
     *
     * @param container
     *            Plug-in container to remove from the list.
     */
    public void removePluginContainer(PluginContainer container) {
        plugins.remove(container.getPlugin());
        pluginSettings.removePluginFromXML(container);
    }

    public boolean deletePlugin(final PluginContainer container, final File tempFile) {
        // deactivate plug-in if it is still active
        if (container.isActive()) {
            container.setActive(false);
            container.changeSettings();
        }

        // remove plug-in
        PluginManager.getSingletonInstance().removePluginContainer(container);

        // delete corresponding JAR file
        File file = new File(container.getPluginLocation());
        if (!file.delete()) {
            // when JAR file cannot be deleted note file by using a temporary
            // file
            // in order to delete it when restarting JHV
            try {
                FileWriter tempFileWriter = new FileWriter(tempFile, true);
                tempFileWriter.write(container.getPluginLocation().getPath() + ";");
                tempFileWriter.flush();
                tempFileWriter.close();
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }

}
