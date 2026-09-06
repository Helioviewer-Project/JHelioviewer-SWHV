package org.helioviewer.jhv.event;

import java.util.List;
import java.util.Set;

// Selected IDs include rows that matched the query but could not be decoded into events.
// Associations cover the supplier and inclusive time range without applying the filters.
public record EventBatch(long sequence, EventBatch.Query query, Set<Integer> selectedIds, List<SolarEvent> events, List<SolarEvent.Link> associations) {

    public record Query(long start, long end, SWEKSupplier supplier, List<SWEK.Param> filters) {
        public Query {
            filters = List.copyOf(filters);
        }
    }

}
