package org.helioviewer.swhv.objects3d;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.swhv.metadata.SWHVMetadataContainer;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.ViewportAdapter;

public class JPXViewWatcher implements ViewListener {

    private JHVJPXView jpxView;
    private SolarObject solarObject;
    private String cadence;
    private String measurement;
    private String detector;
    private String instrument;
    private String observation;
    private String endTime;
    private String startTime;
    private Integer target = 0;
    private boolean active;

    public JPXViewWatcher(SolarObject solarObject, String cadence, String startTime, String endTime, String observation, String instrument, String detector, String measurement) {
        this.cadence = cadence;
        this.startTime = startTime;
        this.endTime = endTime;
        this.observation = observation;
        this.instrument = instrument;
        this.detector = detector;
        this.measurement = measurement;
        this.solarObject = solarObject;
        this.loadMovie();
    }

    public JPXViewWatcher(File file) {
        this.setJPXView(file);
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (active && aEvent.reasonOccurred(SubImageDataChangedReason.class)) {
            JHVJPXView jpxView = sender.getAdapter(JHVJPXView.class);
            SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData) (jpxView.getSubimageData());
            Byte8ImageTransport bytetrs = (Byte8ImageTransport) imageData.getImageTransport();
            byte[] data = bytetrs.getByte8PixelData();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int width = imageData.getWidth();
            int height = imageData.getHeight();
            solarObject.updateBufferData(target, width, height, buffer);
        }
    }

    public void setJPXView(File file) {
        ImageInfoView view;
        try {
            view = APIRequestManager.newLoad(file.toURI(), true, null);
            jpxView = view.getAdapter(JHVJPXView.class);
            target = SWHVMetadataContainer.getSingletonInstance().parseMetadata(this);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if (jpxView != null) {
            JP2Image image = jpxView.getJP2Image();
            ResolutionSet rs = image.getResolutionSet();
            for (int i = 0; i < rs.getMaxResolutionLevels(); i++) {
                Log.debug("resolution level " + i + " : " + rs.getResolutionLevel(i));
            }
            this.jpxView.getJP2Image().getResolutionSet().getNextResolutionLevel(new Dimension(1024, 1024));
            ChangeEvent e = new ChangeEvent();

            this.jpxView.setViewport(new ViewportAdapter(new StaticViewport(1024, 1024)), e);
            Interval<Integer> interval = image.getCompositionLayerRange();
            Log.debug("the interval is : " + interval);
            Log.debug("the start of the interval : " + interval.getStart());
            Log.debug("the end of the interval : " + interval.getEnd());
        }
        jpxView.addViewListener(this);
        this.jpxView.setCurrentFrame(1, new ChangeEvent(), true);
    }

    private void loadMovie() {
        try {
            ImageInfoView view;
            view = APIRequestManager.requestAndOpenRemoteFile(false, getCadence(), getStartTime(), getEndTime(), getObservation(), getInstrument(), getDetector(), getMeasurement(), true);
            jpxView = view.getAdapter(JHVJPXView.class);
            target = SWHVMetadataContainer.getSingletonInstance().parseMetadata(this);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file!", e);
            Message.err("An error occured while opening the remote file!", e.getMessage(), false);
        } finally {
            if (this.jpxView != null) {
                JP2Image image = this.jpxView.getJP2Image();
                ResolutionSet rs = image.getResolutionSet();
                for (int i = 0; i < rs.getMaxResolutionLevels(); i++) {
                    Log.debug("resolution level " + i + " : " + rs.getResolutionLevel(i));
                }
                this.jpxView.getJP2Image().getResolutionSet().getNextResolutionLevel(new Dimension(1024, 1024));
                ChangeEvent e = new ChangeEvent();

                this.jpxView.setViewport(new ViewportAdapter(new StaticViewport(1024, 1024)), e);
                Interval<Integer> interval = image.getCompositionLayerRange();
                Log.debug("the interval is : " + interval);
                Log.debug("the start of the interval : " + interval.getStart());
                Log.debug("the end of the interval : " + interval.getEnd());
            }
            this.jpxView.addViewListener(this);
        }
    }

    protected String getMeasurement() {
        return this.measurement;
    }

    protected String getDetector() {
        return this.detector;
    }

    protected String getInstrument() {
        return this.instrument;
    }

    protected String getObservation() {
        return this.observation;
    }

    protected String getEndTime() {
        return this.endTime;
    }

    protected String getStartTime() {
        return this.startTime;
    }

    protected String getCadence() {
        return this.cadence;
    }

    public JHVJPXView getJPXView() {
        return this.jpxView;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
