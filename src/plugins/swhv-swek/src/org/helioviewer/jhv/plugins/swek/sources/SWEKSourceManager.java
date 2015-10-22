package org.helioviewer.jhv.plugins.swek.sources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.plugin.controller.PluginManager;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.settings.SWEKSettings;

/**
 * Manages all the downloaders and downloads of the SWEK plugin.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKSourceManager {
    /** Singleton instance of the swek download manager */
    private static SWEKSourceManager instance;

    private boolean loadExternalJars;

    /** Instance of the SWEK configuration manager */
    private final SWEKConfigurationManager configManager;

    /** Are the sources loaded */
    private boolean sourcesLoaded;

    private final List<URL> jarURLList;

    /** The URL classloader */
    private URLClassLoader urlClassLoader;

    private SWEKPlugin swekplugin;

    /**
     * private constructor
     */
    private SWEKSourceManager() {
        sourcesLoaded = false;
        loadExternalJars = true;
        configManager = SWEKConfigurationManager.getSingletonInstance();
        jarURLList = new ArrayList<URL>();
    }

    /**
     * Gets the singleton instance of the SWEK source manager.
     *
     * @return the instance of the SWEK source manager
     */
    public static SWEKSourceManager getSingletonInstance() {
        if (instance == null) {
            instance = new SWEKSourceManager();
        }
        return instance;
    }

    public void loadExternalJars(boolean loadExternalJars) {
        this.loadExternalJars = loadExternalJars;
    }

    /**
     * Loads the SWEK sources. If the download source is remote, the jar are
     * downloaded to the home directory of the swekplugin and loaded from there.
     * Local plugins will be loaded from disc.
     *
     * @return true if all the source were loaded, false if not.
     */
    public boolean loadSources() {
        if (!sourcesLoaded) {
            // Check the sweksettings file for the downloaders
            if (checkAndDownloadJars()) {
                if (prepareDownloadersClassLoader()) {
                    sourcesLoaded = true;
                }
            } else {
                sourcesLoaded = false;
            }
        }
        return sourcesLoaded;
    }

    /**
     * Gives the downloader object for the given swek source. This function
     * expects the loadSources is already called. A new call of loadSources is
     * not done in order to avoid an infinite amount of calls to the loadSource
     * function.
     *
     * @param swekSource
     *            the swek source for which the downloader was requested
     * @return the downloader or null if the sources were not loaded correctly
     *         or an error occurred.
     */
    public SWEKDownloader getDownloader(SWEKSource swekSource) {
        return loadClass(swekSource.getDownloaderClass(), SWEKDownloader.class);
    }

    /**
     * Gives the parser object for the given swek source. This function expects
     * the loadSources is already called. A new call of loadSources is not done
     * in order to avoid an inifinite amount of calls to the loadSource
     * function.
     *
     * @param swekSource
     *            the swek source for which the parser was requested
     * @return the parser or null if the sources were not loaded correctly or an
     *         error occurred.
     */
    public SWEKParser getParser(SWEKSource swekSource) {
        return loadClass(swekSource.getEventParserClass(), SWEKParser.class);
    }

    public void setPlugin(SWEKPlugin swekPlugin) {
        swekplugin = swekPlugin;
    }

    /**
     * Generic method that loads a class with the given name of the given
     * classType.
     *
     * @param className
     *            The name of the class to load
     * @param classType
     *            The type of the class to load
     * @return The class requested or null if an error occurred
     */
    private <T> T loadClass(String className, Class<T> classType) {
        if (loadExternalJars) {
            if (sourcesLoaded) {
                try {
                    Object object = urlClassLoader.loadClass(className).newInstance();
                    if (classType.isInstance(object)) {
                        return classType.cast(object);
                    } else {
                        Log.error("The class with name:" + className + " does not return a class of type " + classType.getName() + ". null returned");
                    }
                } catch (ClassNotFoundException e) {
                    Log.error("The class with name:" + className + " could not be loaded. Resulting error: " + e + ". null returned");
                } catch (InstantiationException e) {
                    Log.error("The class with name:" + className + " could not be instantiated. Resulting error: " + e + ". null returned");
                } catch (IllegalAccessException e) {
                    Log.error("The class with name:" + className + " could not be accessed. Resulting error: " + e + ". null returned");
                }
            } else {
                Log.error("The sources are not loaded. Check previous errors and fix the problem first. Null returned.");
            }
            return null;
        } else {
            try {
                Object object = Class.forName(className).newInstance();
                if (classType.isInstance(object)) {
                    return classType.cast(object);
                }
            } catch (InstantiationException e) {
                Log.error("The class with name:" + className + " could not be instantiated. Resulting error: " + e + ". null returned");
            } catch (IllegalAccessException e) {
                Log.error("The class with name:" + className + " could not be accessed. Resulting error: " + e + ". null returned");
            } catch (ClassNotFoundException e) {
                Log.error("The class with name:" + className + " could not be loaded. Resulting error: " + e + ". null returned");
            }
            return null;
        }
    }

    /**
     * Checks the swek configuration file for defined sources. It downloads the
     * jars to the home directory of the swek plugin if the location of the jar
     * is external. It builds up a list of URIs with the location of the jars.
     *
     * @return True if all the sources were downloaded and added to the list.
     */
    private boolean checkAndDownloadJars() {
        boolean downloadJarOK = true;
        for (SWEKSource source : configManager.getSources().values()) {
            URI sourceURI;
            try {
                sourceURI = new URI(source.getJarLocation());
                if (sourceURI.getScheme().equals("file")) {
                    jarURLList.add(sourceURI.toURL());
                } else if (sourceURI.getScheme().equals("jar")) {
                    copyJarFromJar(sourceURI);
                } else {
                    if (downloadJar(sourceURI)) {
                        downloadJarOK = downloadJarOK && true;
                    } else {
                        Log.error("Source with name " + source.getSourceName() + " is not loaded. Problem downloading jar on location: " + source.getJarLocation() + ". This will probably cause the programm not being able to check for events deliverd by this source. Check your configuration file and restart.");
                        downloadJarOK = false;
                    }
                }
            } catch (URISyntaxException e) {
                Log.error("Source with name " + source.getSourceName() + " is not loaded. Problem downloading jar on location: " + source.getJarLocation() + ". This will probably cause the programm not being able to check for events deliverd by this source. Check your configuration file and restart.");
                downloadJarOK = false;
            } catch (MalformedURLException e) {
                Log.error("Source with name " + source.getSourceName() + " is not loaded. Problem downloading jar on location: " + source.getJarLocation() + ". This will probably cause the programm not being able to check for events deliverd by this source. Check your configuration file and restart.");
                downloadJarOK = false;
            }
        }
        return downloadJarOK;
    }

    private void copyJarFromJar(URI sourceURI) {
        String fileName = extractFileName(sourceURI);
        copyToSourcesLocation(SWEKPlugin.class.getResource(sourceURI.getPath()), fileName);
    }

    private boolean copyToSourcesLocation(URL source, String fileName) {
        try {
            ReadableByteChannel rbc = Channels.newChannel(source.openStream());
            File dest = new File(SWEKSettings.SWEK_SOURCES + fileName);
            FileOutputStream fos = new FileOutputStream(dest);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            jarURLList.add(dest.toURI().toURL());
            return true;
        } catch (IOException e) {
            Log.error("IO exception received for source " + source.toString() + ". Given error was " + e);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Downloads the jar from the network and saves them in the home directory
     * of the swek plugin. The URI of the local file is added in the list of
     * jarURIs.
     *
     * @param sourceURI
     *            The URI describing the location of the jar
     * @return True if the jar was downloaded and added to the list of URIs.
     */
    private boolean downloadJar(URI sourceURI) {
        try {
            String fileName = extractFileName(sourceURI);
            return copyToSourcesLocation(sourceURI.toURL(), fileName);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Extracts the name of the file from the URI.
     *
     * @param sourceURI
     *            The URI from which to extract the file name.
     * @return The name of the file.
     */
    private String extractFileName(URI sourceURI) {
        String path = sourceURI.getPath();
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    /**
     * Creates a class loader from the given jars
     *
     * @return True if the classloader was created, false if the classloader was
     *         not created.
     */
    private boolean prepareDownloadersClassLoader() {
        URL[] urls = jarURLList.toArray(new URL[0]);
        if (loadExternalJars) {
            urlClassLoader = URLClassLoader.newInstance(urls, PluginManager.getSingletonInstance().getPluginContainer(swekplugin).getClassLoader());
        } else {
            urlClassLoader = URLClassLoader.newInstance(urls, Thread.currentThread().getContextClassLoader());
        }

        return true;
    }
}
