package org.helioviewer.jhv.plugins.swek.download;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.plugins.swek.SWEKPluginLocks;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;

/**
 * Manages the filters for the different event types.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class FilterManager {
    /** Singleton instance */
    private static FilterManager instance;

    /** the filters */
    private final Map<SWEKEventType, Map<SWEKParameter, List<SWEKParam>>> filters;

    /** The listeners */
    private final Set<FilterManagerListener> listeners;

    private FilterManager() {
        filters = new HashMap<SWEKEventType, Map<SWEKParameter, List<SWEKParam>>>();
        listeners = new HashSet<FilterManagerListener>();
    }

    /**
     * Gets the singleton instance of the filter manager.
     * 
     * @return the singleton instance of the filter manager
     */
    public static FilterManager getSingletonInstance() {
        if (instance == null) {
            instance = new FilterManager();
        }
        return instance;
    }

    /**
     * Adds a FilterManager listener to the Filter manager.
     * 
     * @param listener
     *            the listener to add
     */
    public void addFilterManagerListener(FilterManagerListener listener) {
        synchronized (SWEKPluginLocks.filterManagerLock) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a FilterManager listener from the Filter manager.
     * 
     * @param listener
     *            the listener to remove
     */
    public void removeFilterManagerListener(FilterManagerListener listener) {
        synchronized (SWEKPluginLocks.filterManagerLock) {
            listeners.remove(listener);
        }
    }

    /**
     * Adds a new filter to the list of filters. If the parameter was already
     * filtered this filter is replaced.
     * 
     * @param swekEventType
     *            the event type for which this list of filters is meant
     * @param parameter
     *            the parameter for which this list of filters is meant
     * @param filters
     *            the list with filters
     */
    public void addFilter(SWEKEventType swekEventType, SWEKParameter parameter, List<SWEKParam> downloadFilters) {
        synchronized (SWEKPluginLocks.filterManagerLock) {
            Map<SWEKParameter, List<SWEKParam>> filteredParameterPerEventType = new HashMap<SWEKParameter, List<SWEKParam>>();
            if (filters.containsKey(swekEventType)) {
                filteredParameterPerEventType = filters.get(swekEventType);
            }
            filteredParameterPerEventType.put(parameter, downloadFilters);
            filters.put(swekEventType, filteredParameterPerEventType);
            fireFilterAdded(swekEventType);
        }
    }

    /**
     * Removes the filters for a Swek event type parameter.
     * 
     * @param swekEventType
     *            the event type for which to remove the parameter filter
     * @param parameter
     *            the parameter for which to remove the parameter filter
     */
    public void removedFilter(SWEKEventType swekEventType, SWEKParameter parameter) {
        synchronized (SWEKPluginLocks.filterManagerLock) {
            Map<SWEKParameter, List<SWEKParam>> filteredParameterPerEventType = new HashMap<SWEKParameter, List<SWEKParam>>();
            if (filters.containsKey(swekEventType)) {
                filteredParameterPerEventType = filters.get(swekEventType);
                filteredParameterPerEventType.remove(parameter);
            }
            filters.put(swekEventType, filteredParameterPerEventType);
            fireFilterRemoved(swekEventType, parameter);
        }
    }

    /**
     * Gets the list of filters for for the given event type.
     * 
     * @param eventType
     *            event type for which the filters are needed
     * @return the list with parameter filters
     */
    public Map<SWEKParameter, List<SWEKParam>> getFilterForEventType(SWEKEventType eventType) {
        synchronized (SWEKPluginLocks.filterManagerLock) {
            Map<SWEKParameter, List<SWEKParam>> filtersPerEventType = new HashMap<SWEKParameter, List<SWEKParam>>();

            if (filters.containsKey(eventType)) {
                filtersPerEventType = filters.get(eventType);
            }
            return filtersPerEventType;
        }
    }

    /**
     * Inform the listeners about newly added filters.
     * 
     * @param swekEventType
     *            the event type for which the events were added
     */
    private void fireFilterAdded(SWEKEventType swekEventType) {
        for (FilterManagerListener fml : listeners) {
            fml.filtersAdded(swekEventType);
        }

    }

    /**
     * Inform the listeners about removed filters.
     * 
     * @param swekEventType
     *            the event type for which the filter was removed
     * @param parameter
     *            the parameter for which the filter was removed
     */
    private void fireFilterRemoved(SWEKEventType swekEventType, SWEKParameter parameter) {
        for (FilterManagerListener fml : listeners) {
            fml.filtersRemoved(swekEventType, parameter);
        }

    }
}
