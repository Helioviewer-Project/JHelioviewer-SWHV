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
    private static final Map<SWEKEventType, Map<SWEKParameter, List<SWEKParam>>> filters = new HashMap<>();
    private static final HashSet<FilterManagerListener> listeners = new HashSet<>();

    public static void addFilterManagerListener(FilterManagerListener listener) {
        listeners.add(listener);
    }

    public static void removeFilterManagerListener(FilterManagerListener listener) {
        listeners.remove(listener);
    }

    public static void addFilter(SWEKEventType swekEventType, SWEKParameter parameter, SWEKParam filter) {
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

    public static void removeFilters(SWEKEventType swekEventType) {
        filters.remove(swekEventType);
    }

    public static void fireFilters(SWEKEventType swekEventType) {
        fireFilterChanged(swekEventType);
    }

    public static Map<SWEKParameter, List<SWEKParam>> getFilterForEventType(SWEKEventType eventType) {
        if (filters.containsKey(eventType)) {
            return filters.get(eventType);
        }
        return new HashMap<>();
    }

    public static boolean isFiltered(SWEKEventType eventType, SWEKParameter parameter) {
        return filters.containsKey(eventType) && filters.get(eventType).containsKey(parameter);
    }

    private static void fireFilterChanged(SWEKEventType swekEventType) {
        for (FilterManagerListener fml : listeners) {
            fml.filtersChanged(swekEventType);
        }
    }

}
