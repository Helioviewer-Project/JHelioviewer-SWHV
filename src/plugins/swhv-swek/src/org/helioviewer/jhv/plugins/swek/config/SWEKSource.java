package org.helioviewer.jhv.plugins.swek.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of SWEK source. A SWEK source it an space weather event provider.
 * A SWEK source has a name, provider name, defines a downloader, an event
 * parser, a base URL and general parameters.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKSource {

    /** The name of this source */
    private String sourceName;

    /** The name of the provider */
    private String providerName;

    /** The downloader for the events */
    private String downloaderClass;

    /** The parser of the events */
    private String eventParserClass;

    /** The location of the jar with the downloader and the parser */
    private String jarLocation;

    /** The base URL of the source */
    private String baseURL;

    /** The general parameters of this source */
    private List<SWEKParameter> generalParameters;

    /**
     * Creates a SWEK source with an empty source name, provider name,
     * downloader, parser, base URL and empty list of general parameters.
     */
    public SWEKSource() {
        sourceName = "";
        providerName = "";
        downloaderClass = "";
        eventParserClass = "";
        jarLocation = "";
        baseURL = "";
        generalParameters = new ArrayList<SWEKParameter>();
    }

    /**
     * Creates a SWEK source for the given source name and provider name, with
     * the given downloader, event parser and general parameters.
     * 
     * @param sourceName
     *            The name of the SWEK source
     * @param providerName
     *            The name of the provider
     * @param downloaderClass
     *            The downloader class for this SWEK source
     * @param eventParser
     *            The event parser class for this SWEK source parser
     * @param jarLocation
     *            The location of the jar containing the downloader and parser
     *            classes
     * @param baseURL
     *            The base URL needed to download the events
     * @param generalParameters
     *            The general parameter for this SWEK source
     */
    public SWEKSource(String sourceName, String providerName, String downloaderClass, String jarLocation, String eventParserClass,
            String baseURL, List<SWEKParameter> generalParameters) {
        super();
        this.sourceName = sourceName;
        this.providerName = providerName;
        this.downloaderClass = downloaderClass;
        this.eventParserClass = eventParserClass;
        this.jarLocation = jarLocation;
        this.baseURL = baseURL;
        this.generalParameters = generalParameters;
    }

    /**
     * Gives the source name of the SWEK source
     * 
     * @return the sourceName
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Sets the source name of the SWEK source
     * 
     * @param sourceName
     *            the sourceName to set
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * Gives the provider name of the SWEK source.
     * 
     * @return the providerName The name of the provider
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Sets the name of the provider.
     * 
     * @param providerName
     *            the providerName to set
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    /**
     * Gets the downloader class of the SWEK sources.
     * 
     * @return The downloader class for events from this source
     */
    public String getDownloaderClass() {
        return downloaderClass;
    }

    /**
     * Sets the downloader class for this SWEK source
     * 
     * @param downloaderClass
     *            the downloader class to set
     */
    public void setDownloaderClass(String downloaderClass) {
        this.downloaderClass = downloaderClass;
    }

    /**
     * Gets the parser class of the events for this source.
     * 
     * @return the eventParser class
     */
    public String getEventParserClass() {
        return eventParserClass;
    }

    /**
     * Sets the parser class for this source.
     * 
     * @param eventParserClass
     *            the eventParser class to set
     */
    public void setEventParserClass(String eventParserClass) {
        this.eventParserClass = eventParserClass;
    }

    /**
     * Gets the base URL for this source.
     * 
     * @return the baseURL The URL for this source.
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * Sets the base URl for this source
     * 
     * @param baseURL
     *            the baseURL to set
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * Gets the general parameters of this source
     * 
     * @return the generalParameters The general parameters of this source
     */
    public List<SWEKParameter> getGeneralParameters() {
        return generalParameters;
    }

    /**
     * Sets the general parameters of this SWEK source.
     * 
     * @param generalParameters
     *            the generalParameters to set
     */
    public void setGeneralParameters(List<SWEKParameter> generalParameters) {
        this.generalParameters = generalParameters;
    }

    /**
     * Gets the location of the jar containing the downloader and parser classes
     * 
     * @return string containing the location of the jar
     */
    public String getJarLocation() {
        return jarLocation;
    }

    /**
     * Sets the location of the jar containing the dowloader and parser classes
     * 
     * @param jarLocation
     *            The location of the jar
     */
    public void setJarLocation(String jarLocation) {
        this.jarLocation = jarLocation;
    }

    /**
     * Contains this source the following parameter.
     * 
     * @param name
     *            the name of the parameter
     * @return true if the parameter is configured for this source, false if the
     *         parameter is not configured for this source
     */
    public boolean containsParameter(String name) {
        for (SWEKParameter parameter : generalParameters) {
            if (parameter.getParameterName().toLowerCase().equals(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the parameter if defined in the SWEK source.
     * 
     * @param name
     *            the name of the parameter
     * @return the parameter if the parameter is defined in the SWEKSource, null
     *         if the parameter was not found
     */
    public SWEKParameter getParameter(String name) {
        for (SWEKParameter parameter : generalParameters) {
            if (parameter.getParameterName().toLowerCase().equals(name.toLowerCase())) {
                return parameter;
            }
        }
        return null;
    }

}
