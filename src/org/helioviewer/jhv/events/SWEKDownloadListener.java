package org.helioviewer.jhv.events;

public interface SWEKDownloadListener {

    void startedDownload(SWEKGroup group);

    void stoppedDownload(SWEKGroup group);

}
