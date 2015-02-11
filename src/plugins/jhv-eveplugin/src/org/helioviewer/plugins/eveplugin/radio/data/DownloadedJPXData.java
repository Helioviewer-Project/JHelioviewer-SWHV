package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.helioviewer.base.logging.Log;
import org.helioviewer.plugins.eveplugin.radio.filter.FilterModel;
import org.helioviewer.plugins.eveplugin.radio.filter.FilterModelListener;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Int32ImageTransport;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2CallistoView;

public class DownloadedJPXData implements ViewListener, FilterModelListener {
    private ImageInfoView view;
    private Long imageID;
    private Date startDate;
    private Date endDate;
    private String plotIdentifier;
    private final RadioDataManager radioDataManager;
    private final Long downloadID;
    private SwingWorker<DownloadedJPXDataWorkerResult, Void> worker;
    private int workernumber;

    public DownloadedJPXData(ImageInfoView view, Long imageID, Date startDate, Date endDate, String plotIdentifier, Long downloadID) {
        super();
        FilterModel.getInstance().addFilterModelListener(this);
        this.view = view;
        this.imageID = imageID;
        this.startDate = startDate;
        this.endDate = endDate;
        this.plotIdentifier = plotIdentifier;
        radioDataManager = RadioDataManager.getSingletonInstance();
        this.downloadID = downloadID;
        view.addViewListener(this);
        workernumber = 0;
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

    public String getPlotIdentifier() {
        return plotIdentifier;
    }

    public void setPlotIdentifier(String plotIdentifier) {
        this.plotIdentifier = plotIdentifier;
    }

    @Override
    public synchronized void viewChanged(final View sender, final ChangeEvent aEvent) {

        // Log.trace("View changed for image ID : " + imageID);
        // Log.debug("Event type: " + aEvent);
        // Log.debug("dworker dowloadedjpxdata : " + this);
        while (worker != null && !worker.isDone()) {
            try {
                // Log.debug("dworker is busy sleep 10ms");
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        worker = new SwingWorker<DownloadedJPXDataWorkerResult, Void>() {

            private int nr;

            @Override
            protected DownloadedJPXDataWorkerResult doInBackground() {
                // Log.trace("dworker " + nr + " View changed for image ID : " +
                // imageID);
                // Log.debug("dworker " + nr + " Event type: " + aEvent);
                for (ViewportChangedReason cr : aEvent.getAllChangedReasonsByType(ViewportChangedReason.class)) {
                    radioDataManager.finishedDownloadingID(imageID, downloadID);
                }

                return getJPXData(sender);
            }

            public SwingWorker<DownloadedJPXDataWorkerResult, Void> init(int workernumber) {
                nr = workernumber;
                return this;
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled()) {
                        DownloadedJPXDataWorkerResult result = get();
                        if (result != null) {
                            radioDataManager.dataForIDReceived(result.getData(), result.getImageID(), result.getDownloadID(), result.getDataSize());
                        } else {
                            // Log.debug("dWorker" + nr + " : Result is null");
                        }
                    } else {
                        // Log.debug("dWorker" + nr + " was cancelled");
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    Log.error("dWorker" + nr + " interrupted " + e.getMessage());
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    Log.error("dWorker " + nr + "execution error " + e.getMessage());
                    e.printStackTrace();
                }

            }

        }.init(workernumber++);

        worker.execute();
        /*
         * Log.trace("View changed for image ID : " + imageID);
         * Log.debug("Event type: " + aEvent); for (ViewportChangedReason cr :
         * aEvent.getAllChangedReasonsByType(ViewportChangedReason.class)) {
         * radioDataManager.finishedDownloadingID(imageID, downloadID); }
         * JHVJPXView jpxView = sender.getAdapter(JHVJPXView.class); if (jpxView
         * != null) { ImageCacheStatus status = jpxView.getImageCacheStatus();
         * if (status.getImageStatus(0) == CacheStatus.COMPLETE) { byte[] data =
         * new byte[0]; SingleChannelByte8ImageData imageData =
         * (SingleChannelByte8ImageData) (jpxView.getSubimageData()); if
         * (imageData != null) { MetaDataView metaDataView =
         * sender.getAdapter(MetaDataView.class); Byte8ImageTransport bytetrs =
         * (Byte8ImageTransport) imageData.getImageTransport(); data =
         * bytetrs.getByte8PixelData();
         * 
         * HelioviewerMetaData md = (HelioviewerMetaData)
         * metaDataView.getMetaData(); Double mpp = md.getUnitsPerPixel();
         * byte[] copyData = Arrays.copyOf(data, data.length); data = new
         * byte[0]; radioDataManager.dataForIDReceived(copyData, imageID,
         * downloadID, new Rectangle(imageData.getWidth(),
         * imageData.getHeight())); } } else {
         * Log.debug("Download not complete"); } } else { JHVJP2View jp2View =
         * sender.getAdapter(JHVJP2View.class); MetaDataView metaDataView =
         * sender.getAdapter(MetaDataView.class); byte[] data = new byte[0];
         * SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData)
         * (jp2View.getSubimageData()); if (imageData != null) {
         * Byte8ImageTransport bytetrs = (Byte8ImageTransport)
         * imageData.getImageTransport(); data = bytetrs.getByte8PixelData();
         * List<Byte> dataList = new ArrayList<Byte>(); for (Byte b : data) {
         * dataList.add(b); } if (Collections.min(dataList) !=
         * Collections.max(dataList)) {
         * 
         * HelioviewerMetaData md = (HelioviewerMetaData)
         * metaDataView.getMetaData(); Double mpp = md.getUnitsPerPixel();
         * byte[] copyData = Arrays.copyOf(data, data.length); data = new
         * byte[0]; radioDataManager.dataForIDReceived(copyData, imageID,
         * downloadID, new Rectangle(imageData.getWidth(),
         * imageData.getHeight())); } } }
         */
    }

    @Override
    public void colorLUTChanged() {
        while (worker != null && !worker.isDone()) {
            try {
                // Log.debug("dworker is busy sleep 10ms");
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        worker = new SwingWorker<DownloadedJPXDataWorkerResult, Void>() {

            private int nr;

            @Override
            protected DownloadedJPXDataWorkerResult doInBackground() {
                // Log.trace("dworker " + nr + " View changed for image ID : " +
                // imageID);
                // Log.debug("dworker " + nr + " Event type: " + aEvent);

                return getJPXData(view);
            }

            public SwingWorker<DownloadedJPXDataWorkerResult, Void> init(int workernumber) {
                nr = workernumber;
                return this;
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled()) {
                        DownloadedJPXDataWorkerResult result = get();
                        if (result != null) {
                            if (!result.isByteData()) {
                                radioDataManager.dataForIDReceived(result.getData(), result.getImageID(), result.getDownloadID(), result.getDataSize());
                            } else {
                                radioDataManager.dataForIDReceived(result.getByteData(), result.getImageID(), result.getDownloadID(), result.getDataSize());
                            }
                        } else {
                            // Log.debug("dWorker" + nr + " : Result is null");
                        }
                    } else {
                        // Log.debug("dWorker" + nr + " was cancelled");
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    Log.error("dWorker" + nr + " interrupted " + e.getMessage());
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    Log.error("dWorker " + nr + "execution error " + e.getMessage());
                    e.printStackTrace();
                }

            }

        }.init(workernumber++);

        worker.execute();

    }

    private DownloadedJPXDataWorkerResult getJPXData(View view) {
        if (view != null) {
            JHVJP2CallistoView jp2CallistoView = view.getAdapter(JHVJP2CallistoView.class);
            if (jp2CallistoView != null) {
                MetaDataView metaDataView = view.getAdapter(MetaDataView.class);
                ImageData imData = FilterModel.getInstance().colorFilter(jp2CallistoView.getSubimageData());
                if (imData instanceof ARGBInt32ImageData) {
                    int[] data = new int[0];
                    ARGBInt32ImageData imageData = (ARGBInt32ImageData) (imData);
                    if (imageData != null) {
                        Int32ImageTransport bytetrs = (Int32ImageTransport) imageData.getImageTransport();
                        data = bytetrs.getInt32PixelData();
                        HelioviewerMetaData md = (HelioviewerMetaData) metaDataView.getMetaData();
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
                        HelioviewerMetaData md = (HelioviewerMetaData) metaDataView.getMetaData();
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
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
            worker = null;
        } else {
            worker = null;
        }
        if (view != null) {
            view.removeViewListener(this);
            JHVJP2CallistoView calView = view.getAdapter(JHVJP2CallistoView.class);
            if (calView != null) {
                calView.abolish();
            }
        }
        FilterModel.getInstance().removeFilterModelListener(this);
        view = null;
    }
}
