package org.helioviewer.jhv.plugins.swek.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the configuration of the SWEK plugin
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKConfiguration {

    /** The configuration version */
    private String configurationVersion;

    /** Manually changed configuration */
    private boolean manuallyChanged;

    /** The SWEK sources */
    private List<SWEKSource> sources;

    /** The SWEK event types */
    private List<SWEKEventType> eventTypes;

    /** The SWEK related events */
    private List<SWEKRelatedEvents> relatedEvents;

    /**
     * Creates a SWEKConfiguration with an empty configuration version, not
     * manually changed with an empty list of sources, event types and related
     * events.
     *
     */
    public SWEKConfiguration() {
        super();
        configurationVersion = "";
        manuallyChanged = false;
        sources = new ArrayList<SWEKSource>();
        eventTypes = new ArrayList<SWEKEventType>();
        relatedEvents = new ArrayList<SWEKRelatedEvents>();
    }

    /**
     * Creates a SWEK configuration based on the given configuration version,
     * manually changed indication, list of sources, list of event types and
     * list of related events.
     *
     * @param configurationVersion
     *            The version of the configuration file
     * @param manuallyChanged
     *            True is manually changed, false if not
     * @param sources
     *            List of SWEK sources
     * @param eventTypes
     *            List of SWEK event types
     * @param relatedEvents
     *            List of related events
     */
    public SWEKConfiguration(String configurationVersion, boolean manuallyChanged, List<SWEKSource> sources, List<SWEKEventType> eventTypes, List<SWEKRelatedEvents> relatedEvents) {
        super();
        this.configurationVersion = configurationVersion;
        this.manuallyChanged = manuallyChanged;
        this.sources = sources;
        this.eventTypes = eventTypes;
        this.relatedEvents = relatedEvents;
    }

    /**
     * Gets the version of the configuration file.
     *
     * @return the configurationVersion
     */
    public String getConfigurationVersion() {
        return configurationVersion;
    }

    /**
     * Sets the version of the configuration file.
     *
     * @param configurationVersion
     *            the configurationVersion to set
     */
    public void setConfigurationVersion(String configurationVersion) {
        this.configurationVersion = configurationVersion;
    }

    /**
     * Is the configuration file manually changed.
     *
     * @return the manuallyChanged True if the configuration file was manually
     *         change, false if not.
     */
    public boolean isManuallyChanged() {
        return manuallyChanged;
    }

    /**
     * Sets the configuration file to manually changed.
     *
     * @param manuallyChanged
     *            True if the configuration file was manually changed, false if
     *            not.
     */
    public void setManuallyChanged(boolean manuallyChanged) {
        this.manuallyChanged = manuallyChanged;
    }

    /**
     * Gets the list of SWEK sources.
     *
     * @return the sources
     */
    public List<SWEKSource> getSources() {
        return sources;
    }

    /**
     * Sets the list of SWEK sources.
     *
     * @param sources
     *            the sources to set
     */
    public void setSources(List<SWEKSource> sources) {
        this.sources = sources;
    }

    /**
     * Gets the list of event types.
     *
     * @return the eventTypes
     */
    public List<SWEKEventType> getEventTypes() {
        return eventTypes;
    }

    /**
     * Sets the list of event types.
     *
     * @param eventTypes
     *            the eventTypes to set
     */
    public void setEventTypes(List<SWEKEventType> eventTypes) {
        this.eventTypes = eventTypes;
    }

    /**
     * Gets the list of related events.
     *
     * @return the relatedEvents
     */
    public List<SWEKRelatedEvents> getRelatedEvents() {
        return relatedEvents;
    }

    /**
     * Sets the list of related events.
     *
     * @param relatedEvents
     *            the relatedEvents to set
     */
    public void setRelatedEvents(List<SWEKRelatedEvents> relatedEvents) {
        this.relatedEvents = relatedEvents;
    }

    public SWEKSource getSWEKSource(String sourceName) {
        for (SWEKSource ss : sources) {
            if (ss.getSourceName().equals(sourceName)) {
                return ss;
            }
        }
        return null;
    }

    public SWEKSupplier getSWEKSupplier(String supplierName, String eventTypeName) {
        for (SWEKEventType set : eventTypes) {
            if (set.getEventName().equals(eventTypeName)) {
                for (SWEKSupplier ss : set.getSuppliers()) {
                    if (ss.getSupplierName().equals(supplierName)) {
                        return ss;
                    }
                }
            }
        }
        return null;
    }

    public SWEKEventType getSWEKEventType(String eventTypeName) {
        for (SWEKEventType set : eventTypes) {
            if (set.getEventName().equals(eventTypeName)) {
                return set;
            }
        }
        return null;
    }
}
