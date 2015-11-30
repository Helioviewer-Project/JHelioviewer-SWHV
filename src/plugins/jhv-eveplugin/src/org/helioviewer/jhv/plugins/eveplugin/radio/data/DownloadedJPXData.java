package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Date;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;

public class DownloadedJPXData implements ImageDataHandler {

    private JP2ViewCallisto view;
    private final long imageID;
    private final Date startDate;
    private final Date endDate;
    private final RadioDataManager radioDataManager;
    private final long downloadID;

    public DownloadedJPXData(JP2ViewCallisto _view, long _imageID, Date _startDate, Date _endDate, long _downloadID) {
        super();
        radioDataManager = RadioDataManager.getSingletonInstance();

        view = _view;
        view.setDataHandler(this);

        imageID = _imageID;
        startDate = _startDate;
        endDate = _endDate;
        downloadID = _downloadID;
    }

    public JP2ViewCallisto getView() {
        return view;
    }

    public long getImageID() {
        return imageID;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void remove() {
        radioDataManager.finishedDownloadingID(imageID, downloadID);

        if (view != null) {
            view.removeDataHandler();
            view.abolish();
            view = null;
        }
    }

    @Override
    public void handleData(ImageData imageData) {
        if (imageData instanceof SingleChannelByte8ImageData) {
            if (imageData.getWidth() < 1 || imageData.getHeight() < 1) {
                Log.error("width: " + imageData.getWidth() + " height: " + imageData.getHeight());
                return;
            }
            byte[] data = (byte[]) imageData.getBuffer().array();
            radioDataManager.dataForIDReceived(data, imageID, downloadID, new Rectangle(imageData.getWidth(), imageData.getHeight()));
            radioDataManager.finishedDownloadingID(imageID, downloadID);
        }
    }

}
