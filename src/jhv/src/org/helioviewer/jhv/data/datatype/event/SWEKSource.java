package org.helioviewer.jhv.data.datatype.event;

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
    private final String sourceName;

    /** The name of the provider */
    private final String providerName;

    /** The downloader for the events */
    private final String downloaderClass;

    /** The parser of the events */
    private final String eventParserClass;

    /** The location of the jar with the downloader and the parser */
    private final String jarLocation;

    /** The base URL of the source */
    private final String baseURL;

    /** The general parameters of this source */
    private final List<SWEKParameter> generalParameters;

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
     * Gives the provider name of the SWEK source.
     *
     * @return the providerName The name of the provider
     */
    public String getProviderName() {
        return providerName;
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
     * Gets the parser class of the events for this source.
     *
     * @return the eventParser class
     */
    public String getEventParserClass() {
        return eventParserClass;
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
     * Gets the general parameters of this source
     *
     * @return the generalParameters The general parameters of this source
     */
    public List<SWEKParameter> getGeneralParameters() {
        return generalParameters;
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
     * Contains this source the following parameter.
     *
     * @param name
     *            the name of the parameter
     * @return true if the parameter is configured for this source, false if the
     *         parameter is not configured for this source
     */
    public boolean containsParameter(String name) {
        for (SWEKParameter parameter : generalParameters) {
            if (parameter.getParameterName().equalsIgnoreCase(name)) {
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
            if (parameter.getParameterName().equalsIgnoreCase(name)) {
                return parameter;
            }
        }
        return null;
    }

}
