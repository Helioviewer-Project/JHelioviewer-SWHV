package org.helioviewer.jhv.plugins.swek.download;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;

// Implemented by a listener interested in information coming from the filter manager
interface FilterManagerListener {

    void filtersChanged(SWEKEventType swekEventType);

}
