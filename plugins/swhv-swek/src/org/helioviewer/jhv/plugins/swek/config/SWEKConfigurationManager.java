/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

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
 * @author Bram.Bourgognie@oma.be
 *
 */
public class SWEKConfigurationManager {

    /** Singleton instance */
    private static SWEKConfigurationManager singletonInstance;

    /** Config loaded*/
    private final boolean configLoaded;

    /** Config file URL*/
    private URL configFileURL;

    /** The swek properties*/
    private final Properties swekProperties;


    /**
     * private constructor
     */
    private SWEKConfigurationManager(){
        this.configLoaded = false;
        this.swekProperties = new Properties();
    }

    public static SWEKConfigurationManager getSingletonInstance(){
        if(singletonInstance == null){
            singletonInstance = new SWEKConfigurationManager();
        }
        return singletonInstance;
    }

    /**
     * Loads the configuration.
     *
     * If no configuration file is set by the user, the program downloads
     * the configuration file online and saves it the JHelioviewer/Plugins/SWEK-plugin
     * folder.
     *
     */
    public void loadConfiguration() {
        if(!configLoaded) {
            Log.debug("Load the swek internal settings");
            this.loadPluginSettings();
            Log.debug("search and open the configuration file");
            if(checkAndOpenUserSetFile()) {
                parseConfigFile();
            } else if (checkAndOpenHomeDirectoryFile()) {
                parseConfigFile();
            } else if (checkAndOpenOnlineFile()) {
                parseConfigFile();
            } else {
                //TODO config file could not be loaded.
            }
        }
    }

   /**
    * Loads the overall plugin settings.
    */
   private void loadPluginSettings() {
       InputStream defaultPropStream = SWEKPlugin.class.getResourceAsStream("/SWEK.properties");
       try {
           swekProperties.load(defaultPropStream);
       } catch (IOException ex) {
           Log.error("Could not load the swek settings : ", ex);
       }
   }


    /**
     * Downloads the SWEK configuration from the Internet and saves it in the plugin home directory.
     *
     * @return  true if the the file was found and copied to the home directory, false if the
     *          file could not be found, copied or something else went wrong.
     */
    private boolean checkAndOpenOnlineFile() {
        Log.debug("Download the configuration file from the net and copy it to the plugin home directory");
        try {
            URL url = new URL(swekProperties.getProperty("plugin.swek.onlineconfigfile"));
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            String saveFile = SWEKSettings.SWEK_HOME + swekProperties.getProperty("plugin.swek.configfilename");
            FileOutputStream fos = new FileOutputStream(saveFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            configFileURL = new URL("file://" + saveFile);
            return true;
        } catch (MalformedURLException e) {
            Log.debug("Could not create a URL from the value found in the properties file: "+
                    swekProperties.getProperty("plugin.swek.onlineconfigfile") + " : " + e);
        } catch (IOException e) {
            Log.debug("Something went wrong downloading the configuration file from the server or saving it to the local machine : "+ e);
        }
        return false;
    }

    /**
     * Checks the home directory of the plugin (normally ~/JHelioviewer/Plugins/swek-plugin/
     * for the existence of the SWEKSettings.json file.
     *
     * @return  true if the file was found and useful, false if the file was not found.
     */
    private boolean checkAndOpenHomeDirectoryFile() {
        String configFile = SWEKSettings.SWEK_HOME + swekProperties.getProperty("plugin.swek.configfilename");
        try {
            File f = new File(configFile);
            if(f.exists()){
                this.configFileURL = new URL("file://"+ configFile);
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
     * Checks the jhelioviewer settings file for a swek configuration file.
     *
     * @return true if the file as found and useful, false if the file was not found.
     */
    private boolean checkAndOpenUserSetFile() {
        Log.debug("Search for a user define configuration file in the JHelioviewer setting file.");
        Settings jhvSettings = Settings.getSingletonInstance();
        String fileName = jhvSettings.getProperty("plugin.swek.configfile");
        if (fileName == null) {
            Log.debug("No configured filename found.");
            return false;
        } else {
            try {
                URI fileLocation = new URI(fileName);
                configFileURL = fileLocation.toURL();
                Log.debug("Config file : " + configFileURL.toString());
                return true;
            } catch (URISyntaxException e) {
                Log.debug("Wrong URI syntax for the found file name : " + fileName);
                return false;
            } catch (MalformedURLException e) {
                Log.debug("Could not convert the URI in a correct URL. The found file name : " + fileName);
                return false;
            }
        }
    }

    /**
     * Parses the SWEK settings.
     */
    private boolean parseConfigFile() {
        try {
            InputStream configIs = configFileURL.openStream();
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(configIs));
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject configJSON = new JSONObject(sb.toString());
            return true;
        } catch (IOException e) {
            Log.debug("The configuration file could not be parsed : "+ e);
            System.exit(1);
        } catch (JSONException e) {
            Log.debug("Could not parse the given JSON : " + e);
            System.exit(1);
        }
        return false;
    }

}
