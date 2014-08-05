package org.helioviewer.jhv.plugins.swek.sources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Properties;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.plugins.swek.settings.SWEKSettings;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manages all the downloaders and downloads of the SWEK plugin.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKSourceManager {
    /** Singleton instance of the swek download manager */
    private static SWEKSourceManager instance;

    /** Are the sources loaded */
    private boolean sourcesLoaded;

    /**  */
    private URL sourcesConfigURL;

    /** The swek properties */
    private final Properties swekProperties;

    /** The URL classloader */

    /**
     * private constructor
     */
    private SWEKSourceManager() {
        this.sourcesLoaded = false;
        this.swekProperties = new Properties();
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
     * Loads the SWEK sources.
     */
    public void loadSources() {
        if (!this.sourcesLoaded) {
            // Check for user set download file and download the jars
            boolean isSourcesConfigParsed;
            loadPluginSettings();
            if (checkAndOpenUserSetSourcesConfig()) {
                isSourcesConfigParsed = parseSourcesConfig();
            } else if (checkAndOpenHomeDirectorySourcesConfig()) {
                isSourcesConfigParsed = parseSourcesConfig();
            } else if (checkAndOpenOnlineSourcesConfig()) {
                isSourcesConfigParsed = parseSourcesConfig();
            } else {
                isSourcesConfigParsed = false;
            }
            if (isSourcesConfigParsed) {
                if (prepareDownloadersClassLoader()) {
                    this.sourcesLoaded = true;
                } else {
                    this.sourcesLoaded = false;
                }
            } else {
                this.sourcesLoaded = false;
            }
        }
    }

    /**
     * Loads the overall plugin settings.
     */
    private void loadPluginSettings() {
        InputStream defaultPropStream = SWEKPlugin.class.getResourceAsStream("/SWEK.properties");
        try {
            this.swekProperties.load(defaultPropStream);
        } catch (IOException ex) {
            Log.error("Could not load the swek settings : ", ex);
        }
    }

    /**
     * Parses the SWEK settings.
     */
    private boolean parseSourcesConfig() {
        try {
            InputStream configIs = this.sourcesConfigURL.openStream();
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(configIs));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject configJSON = new JSONObject(sb.toString());
            return parseJSONConfig(configJSON);
        } catch (IOException e) {
            Log.debug("The configuration file could not be parsed : " + e);
        } catch (JSONException e) {
            Log.debug("Could not parse the given JSON : " + e);
        }
        return false;
    }

    private boolean parseJSONConfig(JSONObject configJSON) {
        return true;
    }

    private boolean prepareDownloadersClassLoader() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Downloads the sources from the Internet and saves it in the plugin home
     * directory.
     * 
     * @return true if the the file was found and copied to the home directory,
     *         false if the file could not be found, copied or something else
     *         went wrong.
     */
    private boolean checkAndOpenOnlineSourcesConfig() {
        Log.debug("Download the sources file from the net and copy it to the plugin home directory");
        try {
            URL url = new URL(this.swekProperties.getProperty("plugin.swek.sources"));
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            String saveFile = SWEKSettings.SWEK_HOME + this.swekProperties.getProperty("plugin.swek.sourcesfilename");
            FileOutputStream fos = new FileOutputStream(saveFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            this.sourcesConfigURL = new URL("file://" + saveFile);
            return true;
        } catch (MalformedURLException e) {
            Log.debug("Could not create a URL from the value found in the properties file: "
                    + this.swekProperties.getProperty("plugin.swek.sources") + " : " + e);
        } catch (IOException e) {
            Log.debug("Something went wrong downloading the configuration file from the server or saving it to the local machine : " + e);
        }
        return false;
    }

    /**
     * Checks the home directory of the plugin (normally
     * ~/JHelioviewer/Plugins/swek-plugin/) for the existence of the
     * sources.json file.
     * 
     * @return true if the file was found and useful, false if the file was not
     *         found.
     */
    private boolean checkAndOpenHomeDirectorySourcesConfig() {
        String configFile = SWEKSettings.SWEK_HOME + this.swekProperties.getProperty("plugin.swek.sourcesfilename");
        try {
            File f = new File(configFile);
            if (f.exists()) {
                this.sourcesConfigURL = new URL("file://" + configFile);
                return true;
            } else {
                Log.debug("File created from the settings : " + configFile + " does not exists on this system.");
            }
        } catch (MalformedURLException e) {
            Log.debug("File at possition " + configFile + " could not be parsed into an URL");
        }
        return false;
    }

    /**
     * Checks the jhelioviewer settings file for a swek sources configuration
     * file.
     * 
     * @return true if the file as found and useful, false if the file was not
     *         found.
     */
    private boolean checkAndOpenUserSetSourcesConfig() {
        Log.debug("Search for a user defined configuration file in the JHelioviewer setting file.");
        Settings jhvSettings = Settings.getSingletonInstance();
        String fileName = jhvSettings.getProperty("plugin.swek.sourcesconfigfile");
        if (fileName == null) {
            Log.debug("No configured filename found.");
            return false;
        } else {
            try {
                URI fileLocation = new URI(fileName);
                this.sourcesConfigURL = fileLocation.toURL();
                Log.debug("Config file : " + this.sourcesConfigURL.toString());
                return true;
            } catch (URISyntaxException e) {
                Log.debug("Wrong URI syntax for the found file name : " + fileName);
            } catch (MalformedURLException e) {
                Log.debug("Could not convert the URI in a correct URL. The found file name : " + fileName);
            }
            return false;
        }
    }
}
