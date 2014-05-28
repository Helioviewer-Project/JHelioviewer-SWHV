package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Date;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class DownloadedJPXData implements ViewListener {
    private ImageInfoView view;
    private Long imageID;
    private Date startDate;
    private Date endDate;
    private String plotIdentifier;
    private RadioDataManager radioDataManager;
    private Long downloadID;

    public DownloadedJPXData(ImageInfoView view, Long imageID, Date startDate, Date endDate, String plotIdentifier, Long downloadID) {
        super();
        this.view = view;
        this.imageID = imageID;
        this.startDate = startDate;
        this.endDate = endDate;
        this.plotIdentifier = plotIdentifier;
        this.radioDataManager = RadioDataManager.getSingletonInstance();
        this.downloadID = downloadID;
        view.addViewListener(this);
    }

    public ImageInfoView getView() {
        return view;
    }

    public void setView(ImageInfoView view) {
        this.view = view;
    }

    public Long getImageID() {
        return imageID;
    }

    public void setImageID(Long id) {
        this.imageID = id;
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

    public String getPlotIdentifier() {
        return plotIdentifier;
    }

    public void setPlotIdentifier(String plotIdentifier) {
        this.plotIdentifier = plotIdentifier;
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        for (ViewportChangedReason cr : aEvent.getAllChangedReasonsByType(ViewportChangedReason.class)) {
            radioDataManager.finishedDownloadingID(imageID, downloadID);
        }
        JHVJPXView jpxView = sender.getAdapter(JHVJPXView.class);
        if (jpxView != null) {
            byte[] data = new byte[0];
            SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData) (jpxView.getSubimageData());
            if (imageData != null) {
                MetaDataView metaDataView = sender.getAdapter(MetaDataView.class);
                Byte8ImageTransport bytetrs = (Byte8ImageTransport) imageData.getImageTransport();
                data = bytetrs.getByte8PixelData();
                HelioviewerMetaData md = (HelioviewerMetaData) metaDataView.getMetaData();
                Double mpp = md.getUnitsPerPixel();
                byte[] copyData = Arrays.copyOf(data, data.length);
                data = new byte[0];
                radioDataManager.dataForIDReceived(copyData, imageID, downloadID, new Rectangle(imageData.getWidth(), imageData.getHeight()));
            }
        } else {
            JHVJP2View jp2View = sender.getAdapter(JHVJP2View.class);
            MetaDataView metaDataView = sender.getAdapter(MetaDataView.class);
            byte[] data = new byte[0];
            SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData) (jp2View.getSubimageData());
            if (imageData != null) {
                Byte8ImageTransport bytetrs = (Byte8ImageTransport) imageData.getImageTransport();
                data = bytetrs.getByte8PixelData();
                HelioviewerMetaData md = (HelioviewerMetaData) metaDataView.getMetaData();
                Double mpp = md.getUnitsPerPixel();
                byte[] copyData = Arrays.copyOf(data, data.length);
                data = new byte[0];
                radioDataManager.dataForIDReceived(copyData, imageID, downloadID, new Rectangle(imageData.getWidth(), imageData.getHeight()));
            }
        }
    }
}
