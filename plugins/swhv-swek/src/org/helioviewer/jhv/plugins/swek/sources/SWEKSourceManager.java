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
import java.util.Properties;

import org.helioviewer.base.logging.Log;
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

    /** Instance of the SWEK configuration manager */
    private final SWEKConfigurationManager configManager;

    /** Are the sources loaded */
    private final boolean sourcesLoaded;

    /** The swek properties */
    private final Properties swekProperties;

    private final List<URL> jarURLList;

    /** The URL classloader */
    private URLClassLoader urlClassLoader;

    /**
     * private constructor
     */
    private SWEKSourceManager() {
        this.sourcesLoaded = false;
        this.swekProperties = new Properties();
        this.configManager = SWEKConfigurationManager.getSingletonInstance();
        this.jarURLList = new ArrayList<URL>();
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

    /**
     * Loads the SWEK sources. If the download source is remote, the jar are
     * downloaded to the home directory of the swekplugin and loaded from there.
     * Local plugins will be loaded from disc.
     */
    public void loadSources() {
        if (!this.sourcesLoaded) {
            // Check the sweksettings file for the downloaders
            if (checkAndDownloadJars()) {
                prepareDownloadersClassLoader();
            } else {

            }
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
        for (SWEKSource source : this.configManager.getSources().values()) {
            URI sourceURI;
            try {
                sourceURI = new URI(source.getJarLocation());
                if (sourceURI.getScheme().equals("file")) {
                    this.jarURLList.add(sourceURI.toURL());
                } else {
                    if (downloadJar(sourceURI)) {
                        downloadJarOK = downloadJarOK && true;
                    } else {
                        Log.error("Source with name "
                                + source.getSourceName()
                                + " is not loaded. Promblem downloading jar on location: "
                                + source.getJarLocation()
                                + ". This will probably cause the programm not being able to check for events deliverd by this source. Check your configuration file and restart.");
                        downloadJarOK = false;
                    }
                }
            } catch (URISyntaxException e) {
                Log.error("Source with name "
                        + source.getSourceName()
                        + " is not loaded. Promblem downloading jar on location: "
                        + source.getJarLocation()
                        + ". This will probably cause the programm not being able to check for events deliverd by this source. Check your configuration file and restart.");
                downloadJarOK = false;
            } catch (MalformedURLException e) {
                Log.error("Source with name "
                        + source.getSourceName()
                        + " is not loaded. Promblem downloading jar on location: "
                        + source.getJarLocation()
                        + ". This will probably cause the programm not being able to check for events deliverd by this source. Check your configuration file and restart.");
                downloadJarOK = false;
            }
        }
        return downloadJarOK;
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
            ReadableByteChannel rbc = Channels.newChannel(sourceURI.toURL().openStream());
            String filename = extractFileName(sourceURI);
            File dest = new File(SWEKSettings.SWEK_SOURCES + filename);
            FileOutputStream fos = new FileOutputStream(dest);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            this.jarURLList.add(dest.toURI().toURL());
            return true;
        } catch (MalformedURLException e) {
            Log.error("The URL was malformed for source " + sourceURI + ". Given error was " + e);
        } catch (IOException e) {
            Log.error("IO exception received for source " + sourceURI + ". Given error was " + e);
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
        URL[] urls = this.jarURLList.toArray(new URL[0]);
        this.urlClassLoader = URLClassLoader.newInstance(urls, Thread.currentThread().getContextClassLoader());
        return false;
    }

    /**
     * Gives the downloader object for the given swek source.
     * 
     * @param swekSource
     */
    public void getDownloader(SWEKSource swekSource) {

    }
}
