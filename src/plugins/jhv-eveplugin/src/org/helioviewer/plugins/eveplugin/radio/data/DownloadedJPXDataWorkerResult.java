package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;

public class DownloadedJPXDataWorkerResult {
    private final int[] data;
    private final long imageID;
    private final long downloadID;
    private final Rectangle dataSize;

    public DownloadedJPXDataWorkerResult(int[] data, long imageID, long downloadID, Rectangle dataSize) {
        super();
        this.data = data;
        this.imageID = imageID;
        this.downloadID = downloadID;
        this.dataSize = dataSize;
    }

    /**
     * @return the data
     */
    public int[] getData() {
        return data;
    }

    /**
     * @return the imageID
     */
    public long getImageID() {
        return imageID;
    }

    /**
     * @return the downloadID
     */
    public long getDownloadID() {
        return downloadID;
    }

    /**
     * @return the dataSize
     */
    public Rectangle getDataSize() {
        return dataSize;
    }

}
