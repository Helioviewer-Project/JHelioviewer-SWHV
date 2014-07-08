/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes a swek downloader.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKDownloaderDescription {
    /** The classname of the downloader. */
    private String downloaderClass;

    /** The parametermap */
    private Map<String, String> parameters;

    /**
     * Creates a SWEK downloader description with an empty downloader class and empty parameter map.
     *
     */
    public SWEKDownloaderDescription(){
        this.downloaderClass = "";
        this.parameters = new HashMap<String,String>();
    }


    /**
     * Creates a SWEK downloader description based on the given downloader class with the given parameters map.
     *
     * @param downloaderClass   The class name of the class containing all the download functionality
     * @param parameters        The parameter the class needs to do the downloading
     */
    public SWEKDownloaderDescription(String downloaderClass, Map<String, String> parameters) {
        this.downloaderClass = downloaderClass;
        this.parameters = parameters;
    }



    /**
     * The class name of the class containing all the functionality of the downloader.
     *
     * @return the downloaderClass  The name of the downloader class
     */
    public String getDownloaderClass() {
        return downloaderClass;
    }

    /**
     * Set the name of the downloader class.
     *
     * @param downloaderClass the downloaderClass to set
     */
    public void setDownloaderClass(String downloaderClass) {
        this.downloaderClass = downloaderClass;
    }

    /**
     * Gets the map of parameters needed by the downloader class.
     *
     * @return the parameters   The parameters needed by the downloader.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters of needed by the downloader class.
     *
     * @param parameters the parameters to set
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }


}
