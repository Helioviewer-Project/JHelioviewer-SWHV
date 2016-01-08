package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Date;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;

public class DownloadedJPXData implements ImageDataHandler {

    private JP2ViewCallisto view;
    private final long imageID;
    private final Date startDate;
    private final Date endDate;
    private final RadioDataManager radioDataManager;

    public DownloadedJPXData(JP2ViewCallisto _view, long _imageID, Date _startDate, Date _endDate) {
        super();
        radioDataManager = RadioDataManager.getSingletonInstance();

        view = _view;
        view.setDataHandler(this);

        imageID = _imageID;
        startDate = _startDate;
        endDate = _endDate;
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
        radioDataManager.finishedDownloadingID(imageID);

        if (view != null) {
            view.setDataHandler(null);
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
            Region r = imageData.getRegion();
            ResolutionLevel rl = view.getJP2Image().getResolutionSet(0).getResolutionLevel(0);

            radioDataManager.dataForIDReceived(data, imageID, new Rectangle(imageData.getWidth(), imageData.getHeight()), new Rectangle((int) r.llx, (int) r.lly, (int) r.width, (int) r.height), rl.height);
            radioDataManager.finishedDownloadingID(imageID);
        }
    }
}
