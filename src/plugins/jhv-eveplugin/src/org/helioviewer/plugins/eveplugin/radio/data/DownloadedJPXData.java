package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Date;

import javax.swing.SwingWorker;

import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Int32ImageTransport;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2CallistoView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2CallistoViewDataHandler;

public class DownloadedJPXData implements JHVJP2CallistoViewDataHandler {

    private JHVJP2CallistoView view;
    private Long imageID;
    private Date startDate;
    private Date endDate;
    private final RadioDataManager radioDataManager;
    private final Long downloadID;
    private SwingWorker<DownloadedJPXDataWorkerResult, Void> worker;

    public DownloadedJPXData(JHVJP2CallistoView view, Long imageID, Date startDate, Date endDate, Long downloadID) {
        super();
        this.view = view;
        this.view.setJHVJP2CallistoViewDataHandler(this);
        this.imageID = imageID;
        this.startDate = startDate;
        this.endDate = endDate;
        radioDataManager = RadioDataManager.getSingletonInstance();
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

    private DownloadedJPXDataWorkerResult getJPXData(View view) {
        if (view != null) {
            JHVJP2CallistoView jp2CallistoView = (JHVJP2CallistoView) view;
            if (jp2CallistoView != null) {
                // ImageData imData =
                // FilterModel.getInstance().colorFilter(jp2CallistoView.getSubimageData());
                ImageData imData = jp2CallistoView.getImageData();
                if (imData instanceof ARGBInt32ImageData) {
                    ARGBInt32ImageData imageData = (ARGBInt32ImageData) imData;
                    Int32ImageTransport bytetrs = (Int32ImageTransport) imageData.getImageTransport();

                    int[] data = bytetrs.getInt32PixelData();
                    return new DownloadedJPXDataWorkerResult(data, imageID, downloadID, new Rectangle(imageData.getWidth(), imageData.getHeight()));
                } else if (imData instanceof SingleChannelByte8ImageData) {
                    SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData) imData;
                    Byte8ImageTransport bytetrs = (Byte8ImageTransport) imageData.getImageTransport();

                    byte[] data = bytetrs.getByte8PixelData();
                    return new DownloadedJPXDataWorkerResult(data, imageID, downloadID, new Rectangle(imageData.getWidth(), imageData.getHeight()));
                }
            }
        }
        return null;
    }

    public void remove() {
        radioDataManager.finishedDownloadingID(imageID, downloadID);
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
            worker = null;
        } else {
            worker = null;
        }
        if (view != null) {
            view.abolish();
        }
        view.removeJHVJP2DataHandler();
        view = null;
    }

    public void setInitialData() {
        DownloadedJPXDataWorkerResult result = getJPXData(view);
        if (result != null) {
            radioDataManager.dataForIDReceived(result.getByteData(), result.getImageID(), result.getDownloadID(), result.getDataSize());
        }
    }

    private class DownloadedJPXDataWorkerResult {
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

    @Override
    public void handleData(JHVJP2CallistoView callistoView) {
        DownloadedJPXDataWorkerResult result = getJPXData(callistoView);
        if (result != null) {
            radioDataManager.dataForIDReceived(result.getByteData(), result.getImageID(), result.getDownloadID(), result.getDataSize());
            radioDataManager.finishedDownloadingID(imageID, downloadID);
        }
    }

}
