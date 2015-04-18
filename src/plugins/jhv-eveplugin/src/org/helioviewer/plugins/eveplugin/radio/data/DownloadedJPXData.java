package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Date;

import javax.swing.SwingWorker;

import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Int32ImageTransport;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2CallistoView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

public class DownloadedJPXData {
    private JHVJP2View view;
    private Long imageID;
    private Date startDate;
    private Date endDate;
    private final RadioDataManager radioDataManager;
    private final Long downloadID;
    private SwingWorker<DownloadedJPXDataWorkerResult, Void> worker;

    public DownloadedJPXData(JHVJP2View view, Long imageID, Date startDate, Date endDate, Long downloadID) {
        super();
        this.view = view;
        this.imageID = imageID;
        this.startDate = startDate;
        this.endDate = endDate;
        radioDataManager = RadioDataManager.getSingletonInstance();
        this.downloadID = downloadID;
    }

    public JHVJP2View getView() {
        return view;
    }

    public void setView(JHVJP2View view) {
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

    public void viewChanged(final View sender) {
        DownloadedJPXDataWorkerResult result = getJPXData(sender);
        if (result != null) {
            radioDataManager.dataForIDReceived(result.getByteData(), result.getImageID(), result.getDownloadID(), result.getDataSize());
            radioDataManager.finishedDownloadingID(imageID, downloadID);
        }
    }

    private DownloadedJPXDataWorkerResult getJPXData(View view) {
        if (view != null) {
            JHVJP2CallistoView jp2CallistoView = (JHVJP2CallistoView) view;
            if (jp2CallistoView != null) {
                // ImageData imData =
                // FilterModel.getInstance().colorFilter(jp2CallistoView.getSubimageData());
                ImageData imData = jp2CallistoView.getSubimageData();
                if (imData instanceof ARGBInt32ImageData) {
                    int[] data = new int[0];
                    ARGBInt32ImageData imageData = (ARGBInt32ImageData) (imData);
                    if (imageData != null) {
                        Int32ImageTransport bytetrs = (Int32ImageTransport) imageData.getImageTransport();
                        data = bytetrs.getInt32PixelData();
                        int[] copyData = Arrays.copyOf(data, data.length);
                        data = new int[0];
                        return new DownloadedJPXDataWorkerResult(copyData, imageID, downloadID, new Rectangle(imageData.getWidth(), imageData.getHeight()));
                    }
                } else if (imData instanceof SingleChannelByte8ImageData) {
                    byte[] data = new byte[0];
                    SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData) (jp2CallistoView.getSubimageData());
                    if (imageData != null) {
                        Byte8ImageTransport bytetrs = (Byte8ImageTransport) imageData.getImageTransport();
                        data = bytetrs.getByte8PixelData();
                        // Log.debug("Data size : " + data.length +
                        // " Image size: " + imageData.getHeight() + " x " +
                        // imageData.getWidth());
                        byte[] copyData = Arrays.copyOf(data, data.length);
                        data = new byte[0];
                        // Log.debug("dworker" + nr + ": new result");
                        return new DownloadedJPXDataWorkerResult(copyData, imageID, downloadID, new Rectangle(imageData.getWidth(), imageData.getHeight()));
                    }
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
            ((JHVJP2CallistoView) view).abolish();
        }
        view = null;
    }

}
