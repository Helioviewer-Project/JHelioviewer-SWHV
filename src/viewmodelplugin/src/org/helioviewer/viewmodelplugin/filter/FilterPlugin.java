package org.helioviewer.viewmodelplugin.filter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractList;
import java.util.LinkedList;

import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.controller.PluginSettings;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;

/**
 * This is the basic class for all plug-ins which supplies filter for JHV. It
 * handles the registration of the filters in JHV.
 * 
 * @author Stephan Pagel
 */
public abstract class FilterPlugin implements Plugin {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    protected AbstractList<FilterContainer> filterContainerList = new LinkedList<FilterContainer>();
    protected URI pluginLocation;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     */
    public FilterPlugin() {
        try {
            pluginLocation = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Adds all supplied filters to the list of available and active filters.
     */
    public void installPlugin() {
        for (FilterContainer filter : filterContainerList) {
            filter.setActive(PluginSettings.getSingletonInstance().isFilterInPluginActivated(pluginLocation, filter.getFilterClass(), false));
            filter.setPosition(PluginSettings.getSingletonInstance().getFilterPosition(pluginLocation, filter.getFilterClass()));
            PluginManager.getSingletonInstance().addFilterContainer(filter);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes all supplied filters from the list of available and active
     * filters.
     */
    public void uninstallPlugin() {

        for (FilterContainer filter : filterContainerList) {
            PluginManager.getSingletonInstance().removeFilterContainer(filter);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * FilterPlugins usually do not manage a state, since filters manage their
     * state by themselves. Thus, by default, this functions does nothing.
     */
    public void setState(String state) {

    }

    /**
     * {@inheritDoc}
     * 
     * FilterPlugins usually do not manage a state, since filters manage their
     * state by themselves. Thus, by default, this functions does nothing and
     * returns an empty string.
     */
    public String getState() {
        return new String();
    }

    /**
     * Adds a filter container to the internal list of filters which the plug-in
     * supplies.
     * 
     * @param filterContainer
     *            Filter container which contains the filter which should be
     *            supplied by the plug-in.
     */
    protected void addFilterContainer(FilterContainer filterContainer) {
        filterContainer.setPluginLocation(pluginLocation);
        filterContainerList.add(filterContainer);
    }
}
