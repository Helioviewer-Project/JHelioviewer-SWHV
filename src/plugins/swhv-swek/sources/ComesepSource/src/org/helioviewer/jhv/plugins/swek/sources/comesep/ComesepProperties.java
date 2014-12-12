package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gives access to the COMESEP source properties
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class ComesepProperties {
    private static ComesepProperties singletonInstance;

    /** The COMESEP source properties */
    private final Properties comesepProperties;

    /**
     * Private default constructor.
     */
    private ComesepProperties() {
        comesepProperties = new Properties();
        loadProperties();
    }

    /**
     * Gets the singleton instance of the COMESEP source properties
     * 
     * @return the COMESEP source properties
     */
    public static ComesepProperties getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new ComesepProperties();
        }
        return singletonInstance;
    }

    /**
     * Gets the COMESEP source properties.
     * 
     * @return the comesep source properties
     */
    public Properties getComesepProperties() {
        return comesepProperties;
    }

    /**
     * Loads the overall comesep source settings.
     */
    private void loadProperties() {
        InputStream defaultPropStream = ComesepProperties.class.getResourceAsStream("/comesepsource.properties");
        try {
            comesepProperties.load(defaultPropStream);
        } catch (IOException ex) {
            System.out.println("Could not load the hek settings." + ex);
        }
    }
}
