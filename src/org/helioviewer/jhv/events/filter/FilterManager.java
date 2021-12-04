package org.helioviewer.jhv.events.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.events.SWEK;
import org.helioviewer.jhv.events.SWEKSupplier;

public class FilterManager {

    public interface Listener {
        void filtersChanged(SWEKSupplier supplier);
    }

    private static final Map<SWEKSupplier, Map<SWEK.Parameter, List<SWEK.Param>>> filters = new HashMap<>();
    private static final ArrayList<Listener> listeners = new ArrayList<>();

    public static void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    static void addFilter(SWEKSupplier supplier, SWEK.Parameter parameter, SWEK.Param filter) {
        Map<SWEK.Parameter, List<SWEK.Param>> filteredParameter = getFilter(supplier);
        filteredParameter.computeIfAbsent(parameter, k -> new ArrayList<>()).add(filter);
    }

    static void removeFilters(SWEKSupplier supplier) {
        filters.remove(supplier);
    }

    static void fireFilters(SWEKSupplier supplier) {
        listeners.forEach(listener -> listener.filtersChanged(supplier));
    }

    public static Map<SWEK.Parameter, List<SWEK.Param>> getFilter(SWEKSupplier supplier) {
        return filters.computeIfAbsent(supplier, k -> new HashMap<>());
    }
/*
    public static boolean isFiltered(SWEKSupplier supplier, SWEKParameter parameter) {
        return filters.containsKey(supplier) && filters.get(supplier).containsKey(parameter);
    }
*/
}
