package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;

public interface SWEKTreeModelListener {

    public abstract void expansionChanged();

    public abstract void startedDownloadingEventType(SWEKEventType eventType);

    public abstract void stoppedDownloadingEventType(SWEKEventType eventType);

}
