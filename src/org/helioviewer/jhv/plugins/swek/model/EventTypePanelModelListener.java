package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.event.SWEKSupplier;

public interface EventTypePanelModelListener {

    void groupAndSupplierActive(SWEKGroup group, SWEKSupplier supplier);

    void groupAndSupplierInactive(SWEKGroup group, SWEKSupplier supplier);

}
