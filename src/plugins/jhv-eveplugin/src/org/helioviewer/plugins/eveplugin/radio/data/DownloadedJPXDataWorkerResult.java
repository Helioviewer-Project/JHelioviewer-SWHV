package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;

public class DownloadedJPXDataWorkerResult {
    private final byte[] data;
    private final long imageID;
    private final long downloadID;
    private final Rectangle dataSize;

    public DownloadedJPXDataWorkerResult(byte[] data, long imageID, long downloadID, Rectangle dataSize) {
        super();
        this.data = data;
        this.imageID = imageID;
        this.downloadID = downloadID;
        this.dataSize = dataSize;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
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
