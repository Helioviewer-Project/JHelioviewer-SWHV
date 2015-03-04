package org.helioviewer.viewmodelplugin.controller;

import java.awt.EventQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodelplugin.filter.FilterContainer;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;

/**
 * This class is responsible to manage all plug-ins for JHV. It loads available
 * plug-ins and provides methods to access the loaded plug-ins.
 * 
 * @author Stephan Pagel
 */
public class PluginManager {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static PluginManager singeltonInstance = new PluginManager();

    private PluginSettings pluginSettings = PluginSettings.getSingletonInstance();
    private Map<Plugin, PluginContainer> plugins = new HashMap<Plugin, PluginContainer>();
    private AbstractList<FilterContainer> pluginFilters = new LinkedList<FilterContainer>();
    private AbstractList<OverlayContainer> pluginOverlays = new LinkedList<OverlayContainer>();

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * The private constructor to support the singleton pattern.
     * */
    private PluginManager() {
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static PluginManager getSingletonInstance() {
        return singeltonInstance;
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

                if (!loadPlugin(f.toURI())) {
                    result.add(f.getPath());
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
        return this.plugins.values().toArray(new PluginContainer[0]);
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
            if (container.isActive() == activated)
                result.add(container);
        }

        return result;
    }

    /**
     * Adds a container with a filter to the list of all filters.
     * 
     * @param container
     *            Filter container to add to the list.
     */
    public void addFilterContainer(FilterContainer container) {
        pluginFilters.add(container);
    }

    /**
     * Removes a container with a filter from the list of all filters.
     * 
     * @param container
     *            Filter container to remove from the list.
     */
    public void removeFilterContainer(FilterContainer container) {
        container.setActive(false);
        container.setPosition(-1);
        container.changeSettings();

        pluginFilters.remove(container);
    }

    /**
     * Returns the number of all available filter.
     * 
     * @return Number of available filter.
     * */
    public int getNumberOfFilter() {
        return pluginFilters.size();
    }

    /**
     * Returns a list with all filter which have the passed active status. If
     * the active status is true all activated filters will be returned
     * otherwise all available and not activated filters will be returned.
     * <p>
     * If a list of all activated filters is requested the list is ordered by
     * the user specified position.
     * 
     * @param activated
     *            Indicates if all available (false) or all activated (true)
     *            filters have to be returned.
     * @return list with all filters which have the passed active status.
     */
    public AbstractList<FilterContainer> getFilterContainers(boolean activated) {
        AbstractList<FilterContainer> result = new LinkedList<FilterContainer>();

        for (FilterContainer fc : pluginFilters) {

            if (activated) {
                if (fc.isActive() == true) {

                    int position = fc.getPosition();

                    if (position < 0)
                        result.add(fc);
                    else {
                        boolean added = false;
                        for (int i = 0; i < result.size(); i++) {
                            if (position < result.get(i).getPosition() || result.get(i).getPosition() < 0) {
                                result.add(i, fc);
                                added = true;
                                break;
                            }
                        }

                        if (!added)
                            result.add(fc);
                    }
                }
            } else {
                if (fc.isActive() == false)
                    result.add(fc);
            }
        }

        return result;
    }

    /**
     * Adds a container with a overlay to the list of all overlays.
     * 
     * @param container
     *            Overlay container to add to the list.
     */
    public void addOverlayContainer(OverlayContainer container) {
        pluginOverlays.add(container);
    }

    /**
     * Removes a container with a overlay from the list of all overlays.
     * 
     * @param container
     *            Overlay container to remove from the list.
     */
    public void removeOverlayContainer(OverlayContainer container) {
        container.setActive(false);
        container.changeSettings();

        pluginOverlays.remove(container);
    }

    /**
     * Returns the number of all available overlays.
     * 
     * @return Number of available overlays.
     * */
    public int getNumberOfOverlays() {
        return pluginOverlays.size();
    }

    /**
     * Returns a list with all overlays which have the passed active status. If
     * the active status is true all activated overlays will be returned
     * otherwise all available and not activated overlays will be returned.
     * 
     * @param activated
     *            Indicates if all available (false) or all activated (true)
     *            overlays have to be returned.
     * @return list with all overlays which have the passed active status.
     */
    public AbstractList<OverlayContainer> getOverlayContainers(boolean activated) {
        AbstractList<OverlayContainer> result = new LinkedList<OverlayContainer>();

        for (OverlayContainer oc : pluginOverlays) {
            if (oc.isActive() == activated)
                result.add(oc);
        }

        return result;
    }

    /**
     * Returns an input stream to a resource within a plugin jar file. \n The
     * path must begin with a slash and contain all subfolders, e.g.:\n
     * /images/sample_image.png
     * 
     * @param plugin
     *            The plugin where the resources are stored
     * @param resourcePath
     *            The path to the resource
     * @return An InputStream to the resource
     */
    public InputStream getResourceInputStream(Plugin plugin, String resourcePath) {
        return plugins.get(plugin).getClassLoader().getResourceAsStream(resourcePath);
    }

    /**
     * Returns an URL to a resource within a plugin jar.\n The path must begin
     * with a slash and contain all subfolders, e.g.:\n /images/sample_image.png
     * 
     * @param plugin
     *            The plugin where the resources are stored
     * @param resourcePath
     *            The path to the resource
     * @return An URL to the resource
     */
    public URL getResourceUrl(Plugin plugin, String resourcePath) {
        return plugins.get(plugin).getClassLoader().getResource(resourcePath);
    }

    /**
     * Tries to open the given file and load the expected plug-in.
     * 
     * @param pluginLocation
     *            Specifies the location of the file which contains the plug-in.
     * @return true if the plug-in could be loaded successfully; false
     *         otherwise.
     */
    private boolean loadPlugin_raw(URI pluginLocation) {
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

            if (o instanceof Plugin) {
                addPlugin(classLoader, (Plugin) o, pluginLocation);
                return true;
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

    // http://kotek.net/blog/swingutilities.invokeandwait_with_return_value
    private static <E> E invokeAndWait(final Callable<E> r) {
        final AtomicReference<E> ret = new AtomicReference<E>();
        final AtomicReference<Exception> except = new AtomicReference<Exception>();

        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        ret.set(r.call());
                    } catch (Exception e) {
                        //pass exception to original thread
                        except.set(e);
                    }
                }});
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (except.get() != null)
            //there was an exception in EDT thread, rethrow it
            throw new RuntimeException(except.get());
        else
            return ret.get();
    }

    private class LoadPluginCall implements Callable<Boolean> {
        final AtomicReference<URI> theURI = new AtomicReference<URI>();

        public LoadPluginCall(URI location) {
            this.theURI.set(location);
        }

        public Boolean call() {
            return loadPlugin_raw(this.theURI.get());
        }
    }

    public boolean loadPlugin(URI pluginLocation) {
        return invokeAndWait(new LoadPluginCall(pluginLocation));
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
        final File file = new File(container.getPluginLocation());

        if (!file.delete()) {
            // when JAR file cannot be deleted note file by using a temporary
            // file
            // in order to delete it when restarting JHV
            try {
                final FileWriter tempFileWriter = new FileWriter(tempFile, true);
                tempFileWriter.write(container.getPluginLocation().getPath() + ";");
                tempFileWriter.flush();
                tempFileWriter.close();
            } catch (final IOException e) {
                return false;
            }
        }

        return true;
    }

}
