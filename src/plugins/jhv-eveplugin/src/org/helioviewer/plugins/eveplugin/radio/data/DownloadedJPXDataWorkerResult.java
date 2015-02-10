package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;

public class DownloadedJPXDataWorkerResult {
    private final int[] data;
    private final long imageID;
    private final long downloadID;
    private final Rectangle dataSize;
    private final boolean isByteData;
    private final byte[] byteData;

    public DownloadedJPXDataWorkerResult(int[] data, long imageID, long downloadID, Rectangle dataSize) {
        super();
        this.data = data;
        this.imageID = imageID;
        this.downloadID = downloadID;
        this.dataSize = dataSize;
        isByteData = false;
        byteData = new byte[0];
    }

    public DownloadedJPXDataWorkerResult(byte[] data, Long imageID, Long downloadID, Rectangle dataSize) {
        super();
        this.data = new int[0];
        this.imageID = imageID;
        this.downloadID = downloadID;
        this.dataSize = dataSize;
        isByteData = true;
        byteData = data;
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

    /**
     * Is the data byte data or int data
     * 
     * @return true in the case of byte data, false if integer data
     */
    public boolean isByteData() {
        return isByteData;
    }

    /**
     * Gets the byte data.
     * 
     * @return The byte data.
     */
    public byte[] getByteData() {
        return byteData;
    }
}
