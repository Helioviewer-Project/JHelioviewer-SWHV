package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Date;

import javax.swing.SwingWorker;

import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2CallistoView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2ViewDataHandler;

public class DownloadedJPXData implements JHVJP2ViewDataHandler {

    private JHVJP2CallistoView view;
    private Long imageID;
    private Date startDate;
    private Date endDate;
    private final RadioDataManager radioDataManager;
    private final Long downloadID;
    private SwingWorker<DownloadedJPXDataWorkerResult, Void> worker;

    public DownloadedJPXData(JHVJP2CallistoView view, Long imageID, Date startDate, Date endDate, Long downloadID) {
        super();
        radioDataManager = RadioDataManager.getSingletonInstance();

        this.view = view;
        this.view.setDataHandler(this);
        this.imageID = imageID;
        this.startDate = startDate;
        this.endDate = endDate;
        this.downloadID = downloadID;
    }

    public JHVJP2CallistoView getView() {
        return view;
    }

    public void setView(JHVJP2CallistoView view) {
        this.view = view;
    }

    public Long getImageID() {
        return imageID;
    }

    public void setImageID(Long id) {
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

        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
        worker = null;

        if (view != null) {
            view.removeDataHandler();
            view.abolish();
        }
        view = null;
    }

    private class DownloadedJPXDataWorkerResult {
        private final long imageID;
        private final long downloadID;
        private final Rectangle dataSize;
        private final byte[] byteData;

        public DownloadedJPXDataWorkerResult(byte[] data, Long imageID, Long downloadID, Rectangle dataSize) {
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
    public void handleData(JHVJP2View callistoView, ImageData imageData) {
        if (imageData instanceof SingleChannelByte8ImageData) {
            byte[] data = ((Byte8ImageTransport) imageData.getImageTransport()).getByte8PixelData();
            DownloadedJPXDataWorkerResult result = new DownloadedJPXDataWorkerResult(data, imageID, downloadID, new Rectangle(imageData.getWidth(), imageData.getHeight()));

            radioDataManager.dataForIDReceived(result.getByteData(), result.getImageID(), result.getDownloadID(), result.getDataSize());
            radioDataManager.finishedDownloadingID(imageID, downloadID);
        }
    }

}
