package org.helioviewer.jhv.events.filter;

import org.helioviewer.jhv.events.SWEKSupplier;

public interface FilterManagerListener {

    void filtersChanged(SWEKSupplier supplier);

}
