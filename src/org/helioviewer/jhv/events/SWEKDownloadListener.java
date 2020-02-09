package org.helioviewer.jhv.events;

interface SWEKDownloadListener {

    void startedDownload(SWEKGroup group);

    void stoppedDownload(SWEKGroup group);

}
