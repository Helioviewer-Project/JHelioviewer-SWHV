package org.helioviewer.jhv.plugins.swek.settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;

/**
 * Gives access to the swek properties.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKProperties {
    /** The singleton instance */
    private static SWEKProperties singletonInstance;

    /** The swek properties */
    private final Properties swekProperties;

    /**
     * private default constructor
     */
    private SWEKProperties() {
        this.swekProperties = new Properties();
        loadPluginSettings();
    }

    /**
     * Gives the singleton instance of the SWEK properties.
     * 
     * @return the singleton instance
     */
    public static SWEKProperties getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new SWEKProperties();
        }
        return singletonInstance;
    }

    /**
     * Gets the swek properties.
     * 
     * @return The swek properties.
     */
    public Properties getSWEKProperties() {
        return this.swekProperties;
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
}
