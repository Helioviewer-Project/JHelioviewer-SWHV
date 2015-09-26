package org.helioviewer.jhv.plugins.eveplugin.settings;

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
