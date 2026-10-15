package org.helioviewer.jhv.event.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKSupplier;

public class FilterManager {

    public interface Listener {
        void filtersChanged(SWEKSupplier supplier);
    }

    private static final Map<SWEKSupplier, List<SWEK.Param>> filters = new HashMap<>();
    private static final ArrayList<Listener> listeners = new ArrayList<>();

    public static void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public static void clear() {
        filters.clear();
    }

    static void addFilter(SWEKSupplier supplier, SWEK.Param filter) {
        filters.computeIfAbsent(supplier, _ -> new ArrayList<>()).add(filter);
    }

    static void removeFilters(SWEKSupplier supplier) {
        filters.remove(supplier);
    }

    static void fireFilters(SWEKSupplier supplier) {
        listeners.forEach(listener -> listener.filtersChanged(supplier));
    }

    public static List<SWEK.Param> getFilters(SWEKSupplier supplier) {
        return List.copyOf(filters.getOrDefault(supplier, List.of()));
    }
}
