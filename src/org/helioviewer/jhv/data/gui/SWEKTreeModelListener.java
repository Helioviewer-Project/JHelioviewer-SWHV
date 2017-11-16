package org.helioviewer.jhv.data.gui;

import org.helioviewer.jhv.data.event.SWEKGroup;

public interface SWEKTreeModelListener {

    void startedDownloadingGroup(SWEKGroup group);

    void stoppedDownloadingGroup(SWEKGroup group);

}
