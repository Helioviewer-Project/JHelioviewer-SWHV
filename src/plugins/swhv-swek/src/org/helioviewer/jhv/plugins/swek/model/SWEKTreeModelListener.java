package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;

public interface SWEKTreeModelListener {

    void expansionChanged();

    void startedDownloadingEventType(SWEKEventType eventType);

    void stoppedDownloadingEventType(SWEKEventType eventType);

}
