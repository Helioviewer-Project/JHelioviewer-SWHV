package org.helioviewer.jhv.plugins.swek.download;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;

// Manages the filters for the different event types.
public class FilterManager {

    private static FilterManager instance;
    private final Map<SWEKEventType, Map<SWEKParameter, List<SWEKParam>>> filters = new HashMap<>();
    private final HashSet<FilterManagerListener> listeners = new HashSet<>();

    private FilterManager() {
    }

    public static FilterManager getSingletonInstance() {
        if (instance == null) {
            instance = new FilterManager();
        }
        return instance;
    }

    public void addFilterManagerListener(FilterManagerListener listener) {
        listeners.add(listener);
    }

    public void removeFilterManagerListener(FilterManagerListener listener) {
        listeners.remove(listener);
    }

    public void addFilter(SWEKEventType swekEventType, SWEKParameter parameter, SWEKParam filter) {
        Map<SWEKParameter, List<SWEKParam>> filteredParameterPerEventType;
        if (filters.containsKey(swekEventType))
            filteredParameterPerEventType = filters.get(swekEventType);
        else
            filteredParameterPerEventType = new HashMap<>();
        filters.put(swekEventType, filteredParameterPerEventType);
        if (!filteredParameterPerEventType.containsKey(parameter)) {
            filteredParameterPerEventType.put(parameter, new ArrayList<>());
        }
        filteredParameterPerEventType.get(parameter).add(filter);
    }

    public void removeFilters(SWEKEventType swekEventType) {
        filters.remove(swekEventType);
    }

    public void fireFilters(SWEKEventType swekEventType) {
        fireFilterChanged(swekEventType);
    }

    public Map<SWEKParameter, List<SWEKParam>> getFilterForEventType(SWEKEventType eventType) {
        if (filters.containsKey(eventType)) {
            return filters.get(eventType);
        }
        return new HashMap<>();
    }

    public boolean isFiltered(SWEKEventType eventType, SWEKParameter parameter) {
        return filters.containsKey(eventType) && filters.get(eventType).containsKey(parameter);
    }

    private void fireFilterChanged(final SWEKEventType swekEventType) {
        for (FilterManagerListener fml : listeners) {
            fml.filtersChanged(swekEventType);
        }
    }

}
