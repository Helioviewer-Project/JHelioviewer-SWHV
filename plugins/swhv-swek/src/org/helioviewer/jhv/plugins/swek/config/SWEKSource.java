/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of SWEK source. A SWEK source it an space weather event provider.
 * A SWEK source has a name, provider name, defines a downloader, an event parser, a base URL and general parameters.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKSource {
    /** The name of this source */
    private String sourceName;

    /** The name of the provider*/
    private String providerName;

    /** The downloader for the events */
    private SWEKDownloaderDescription downloader;

    /** The parser of the events */
    private String eventParser;

    /** The base URL of the source*/
    private String baseURL;

    /** The general parameters of this source */
    private List<SWEKParameter> generalParameters;

    /**
     * Creates a SWEK source with an empty source name, provider name, downloader, parser, base URL and empty list of
     * general parameters.
     */
    public SWEKSource(){
        this.sourceName = "";
        this.providerName = "";
        this.downloader = new SWEKDownloaderDescription();
        this.eventParser = "";
        this.baseURL = "";
        this.generalParameters = new ArrayList<SWEKParameter>();
    }


    /**
     * Creates a SWEK source for the given source name and provider name, with the given downloader, event parser and general parameters.
     *
     * @param sourceName            The name of the SWEK source
     * @param providerName          The name of the provider
     * @param downloader            The description of this SWEK source downloader
     * @param eventParser           The event parser for this SWEK source parser
     * @param baseURL               The base URL needed to download the events
     * @param generalParameters     The general parameter for this SWEK source
     */
    public SWEKSource(String sourceName, String providerName, SWEKDownloaderDescription downloader, String eventParser, String baseURL, List<SWEKParameter> generalParameters) {
        super();
        this.sourceName = sourceName;
        this.providerName = providerName;
        this.downloader = downloader;
        this.eventParser = eventParser;
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
     * @param sourceName the sourceName to set
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * Gives the provider name of the SWEK source.
     *
     * @return the providerName     The name of the provider
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Sets the name of the provider.
     *
     * @param providerName the providerName to set
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    /**
     * Gets the downloader of the SWEK sources.
     *
     * @return the downloader   The downloader for events from this source
     */
    public SWEKDownloaderDescription getDownloader() {
        return downloader;
    }

    /**
     * Sets the downloader for this SWEK source
     *
     * @param downloader the downloader to set
     */
    public void setDownloader(SWEKDownloaderDescription downloader) {
        this.downloader = downloader;
    }

    /**
     * Gets the parser of the events for this source.
     *
     * @return the eventParser
     */
    public String getEventParser() {
        return eventParser;
    }

    /**
     * Sets the parser for this source.
     *
     * @param eventParser the eventParser to set
     */
    public void setEventParser(String eventParser) {
        this.eventParser = eventParser;
    }

    /**
     * Gets the base URL for this source.
     *
     * @return the baseURL  The URL for this source.
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * Sets the base URl for this source
     *
     * @param baseURL the baseURL to set
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * Gets the general parameters of this source
     *
     * @return the generalParameters    The general parameters of this source
     */
    public List<SWEKParameter> getGeneralParameters() {
        return generalParameters;
    }

    /**
     * Sets the general parameters of this SWEK source.
     *
     * @param generalParameters the generalParameters to set
     */
    public void setGeneralParameters(List<SWEKParameter> generalParameters) {
        this.generalParameters = generalParameters;
    }
}
