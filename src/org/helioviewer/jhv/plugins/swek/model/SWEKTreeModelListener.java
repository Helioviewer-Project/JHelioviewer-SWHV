package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.data.event.SWEKGroup;

public interface SWEKTreeModelListener {

    void startedDownloadingGroup(SWEKGroup group);

    void stoppedDownloadingGroup(SWEKGroup group);

}
