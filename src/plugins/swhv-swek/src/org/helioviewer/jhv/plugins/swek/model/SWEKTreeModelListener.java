package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.data.event.SWEKEventType;

public interface SWEKTreeModelListener {

    void startedDownloadingEventType(SWEKEventType eventType);

    void stoppedDownloadingEventType(SWEKEventType eventType);

}
