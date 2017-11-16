package org.helioviewer.jhv.data.event.filter;

import org.helioviewer.jhv.data.event.SWEKSupplier;

public interface FilterManagerListener {

    void filtersChanged(SWEKSupplier supplier);

}
