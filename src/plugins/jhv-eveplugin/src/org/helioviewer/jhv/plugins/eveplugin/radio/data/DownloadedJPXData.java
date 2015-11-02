package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Date;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewDataHandler;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2CallistoView;

public class DownloadedJPXData implements ViewDataHandler {

    private JP2CallistoView view;
    private long imageID;
    private Date startDate;
    private Date endDate;
    private final RadioDataManager radioDataManager;
    private final long downloadID;

    public DownloadedJPXData(JP2CallistoView view, long imageID, Date startDate, Date endDate, long downloadID) {
        super();
        radioDataManager = RadioDataManager.getSingletonInstance();

        this.view = view;
        this.view.setDataHandler(this);
        this.imageID = imageID;
        this.startDate = startDate;
        this.endDate = endDate;
        this.downloadID = downloadID;
    }

    public JP2CallistoView getView() {
        return view;
    }

    public void setView(JP2CallistoView view) {
        this.view = view;
    }

    public long getImageID() {
        return imageID;
    }

    public void setImageID(long id) {
        imageID = id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void remove() {
        radioDataManager.finishedDownloadingID(imageID, downloadID);

        if (view != null) {
            view.removeDataHandler();
            view.abolish();
        }
        view = null;
    }

    private static class DownloadedJPXDataWorkerResult {
        private final long imageID;
        private final long downloadID;
        private final Rectangle dataSize;
        private final byte[] byteData;

        public DownloadedJPXDataWorkerResult(byte[] data, long imageID, long downloadID, Rectangle dataSize) {
            super();
            this.imageID = imageID;
            this.downloadID = downloadID;
            this.dataSize = dataSize;
            byteData = data;
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
         * Gets the byte data.
         *
         * @return The byte data.
         */
        public byte[] getByteData() {
            return byteData;
        }
    }

    @Override
    public void handleData(View callistoView, ImageData imageData) {
        if (callistoView instanceof JP2CallistoView && imageData instanceof SingleChannelByte8ImageData) {
            if (imageData.getWidth() < 1 || imageData.getHeight() < 1) {
                Log.error("width: " + imageData.getWidth() + " height: " + imageData.getHeight());
                return;
            }
            byte[] data = (byte[]) imageData.getBuffer().array();
            DownloadedJPXDataWorkerResult result = new DownloadedJPXDataWorkerResult(data, imageID, downloadID, new Rectangle(imageData.getWidth(), imageData.getHeight()));

            radioDataManager.dataForIDReceived(result.getByteData(), result.getImageID(), result.getDownloadID(), result.getDataSize());
            radioDataManager.finishedDownloadingID(imageID, downloadID);
        }
    }

}
