package org.helioviewer.jhv.data.event.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.data.event.SWEKParam;
import org.helioviewer.jhv.data.event.SWEKParameter;
import org.helioviewer.jhv.data.event.SWEKSupplier;

public class FilterManager {

    private static final Map<SWEKSupplier, Map<SWEKParameter, List<SWEKParam>>> filters = new HashMap<>();
    private static final HashSet<FilterManagerListener> listeners = new HashSet<>();

    public static void addFilterManagerListener(FilterManagerListener listener) {
        listeners.add(listener);
    }

    public static void removeFilterManagerListener(FilterManagerListener listener) {
        listeners.remove(listener);
    }

    public static void addFilter(SWEKSupplier supplier, SWEKParameter parameter, SWEKParam filter) {
        Map<SWEKParameter, List<SWEKParam>> filteredParameterPerEventType = getFilter(supplier);
        filters.put(supplier, filteredParameterPerEventType);
        if (!filteredParameterPerEventType.containsKey(parameter)) {
            filteredParameterPerEventType.put(parameter, new ArrayList<>());
        }
        filteredParameterPerEventType.get(parameter).add(filter);
    }

    public static void removeFilters(SWEKSupplier supplier) {
        filters.remove(supplier);
    }

    public static void fireFilters(SWEKSupplier supplier) {
        for (FilterManagerListener fml : listeners) {
            fml.filtersChanged(supplier);
        }
    }

    public static Map<SWEKParameter, List<SWEKParam>> getFilter(SWEKSupplier supplier) {
        return filters.containsKey(supplier) ? filters.get(supplier) : new HashMap<>();
    }

    public static boolean isFiltered(SWEKSupplier supplier, SWEKParameter parameter) {
        return filters.containsKey(supplier) && filters.get(supplier).containsKey(parameter);
    }

}
