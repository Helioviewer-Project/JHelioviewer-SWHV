package org.helioviewer.jhv.plugins.swek.download;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.event.SWEKParam;
import org.helioviewer.jhv.data.event.SWEKParameter;

public class FilterManager {

    private static final Map<SWEKGroup, Map<SWEKParameter, List<SWEKParam>>> filters = new HashMap<>();
    private static final HashSet<FilterManagerListener> listeners = new HashSet<>();

    public static void addFilterManagerListener(FilterManagerListener listener) {
        listeners.add(listener);
    }

    public static void removeFilterManagerListener(FilterManagerListener listener) {
        listeners.remove(listener);
    }

    public static void addFilter(SWEKGroup group, SWEKParameter parameter, SWEKParam filter) {
        Map<SWEKParameter, List<SWEKParam>> filteredParameterPerEventType = filters.containsKey(group) ? filters.get(group) : new HashMap<>();
        filters.put(group, filteredParameterPerEventType);
        if (!filteredParameterPerEventType.containsKey(parameter)) {
            filteredParameterPerEventType.put(parameter, new ArrayList<>());
        }
        filteredParameterPerEventType.get(parameter).add(filter);
    }

    public static void removeFilters(SWEKGroup group) {
        filters.remove(group);
    }

    public static void fireFilters(SWEKGroup group) {
        fireFilterChanged(group);
    }

    public static Map<SWEKParameter, List<SWEKParam>> getFilterForGroup(SWEKGroup group) {
        if (filters.containsKey(group)) {
            return filters.get(group);
        }
        return new HashMap<>();
    }

    public static boolean isFiltered(SWEKGroup group, SWEKParameter parameter) {
        return filters.containsKey(group) && filters.get(group).containsKey(parameter);
    }

    private static void fireFilterChanged(SWEKGroup group) {
        for (FilterManagerListener fml : listeners) {
            fml.filtersChanged(group);
        }
    }

}
