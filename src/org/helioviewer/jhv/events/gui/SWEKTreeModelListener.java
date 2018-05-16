package org.helioviewer.jhv.events.gui;

import org.helioviewer.jhv.events.SWEKGroup;

interface SWEKTreeModelListener {

    void startedDownloadingGroup(SWEKGroup group);

    void stoppedDownloadingGroup(SWEKGroup group);

}
