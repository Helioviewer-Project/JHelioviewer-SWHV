package org.helioviewer.jhv.plugins.eveplugin.settings;

/**
 * @author Freek Verstringe
 * 
 */
public abstract class APIAbstract {
    private String baseUrl;

    /**
     * This returns the request url for a given dataset and is dependent on a
     * given API.
     */
    public abstract String getUrl();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

}
