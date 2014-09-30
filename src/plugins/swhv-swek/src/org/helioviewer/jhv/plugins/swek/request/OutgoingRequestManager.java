package org.helioviewer.jhv.plugins.swek.request;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.swek.receive.SWEKEventHandler;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;

/**
 * This class intercepts changes of the layers and request data from the
 * JHVEventContainer.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class OutgoingRequestManager implements LayersListener {

    /** The singleton instance of the outgoing request manager */
    private static OutgoingRequestManager instance;

    /** instance of the swek event handler */
    private final SWEKEventHandler swekEventHandler;

    /**
     * private constructor
     */
    private OutgoingRequestManager() {
        swekEventHandler = SWEKEventHandler.getSingletonInstace();
    }

    /**
     * Gets the singleton instance of the outgoing request manager
     * 
     * @return the singleton instance
     */
    public static OutgoingRequestManager getSingletonInstance() {
        if (instance == null) {
            instance = new OutgoingRequestManager();
        }
        return instance;
    }

    @Override
    public void layerAdded(int idx) {
        try {
            View activeView = LayersModel.getSingletonInstance().getActiveView();
            List<Date> requestDates = new ArrayList<Date>();
            JHVJPXView jpxView = activeView.getAdapter(JHVJPXView.class);
            if (jpxView != null) {
                for (int frame = 1; frame <= jpxView.getMaximumFrameNumber(); frame++) {
                    String dateOBS = jpxView.getJP2Image().getValueFromXML("DATE-OBS", "fits", frame);
                    if (dateOBS == null) {
                        dateOBS = jpxView.getJP2Image().getValueFromXML("DATE_OBS", "fits", frame);
                    }
                    if (dateOBS != null) {
                        Date parsedDate = parseDate(dateOBS);
                        if (parsedDate != null) {
                            requestDates.add(parsedDate);
                        }
                    } else {
                        Log.error("Destroy myself with handgranade. No date-obs in whatever dialect could be found");
                    }
                }
            }
            JHVEventContainer.getSingletonInstance().requestForDateList(requestDates, swekEventHandler);
        } catch (JHV_KduException ex) {
            Log.error("Received an kakadu exception. " + ex);
        }
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void layerChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void activeLayerChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void viewportGeometryChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void timestampChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void subImageDataChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void layerDownloaded(int idx) {
        // TODO Auto-generated method stub

    }

    /**
     * Parses a date in string with the format yyyy-MM-dd'T'HH:mm:ss.SSS into a
     * date object.
     * 
     * @param dateOBS
     *            the date to parse
     * @return The parsed date
     */
    private Date parseDate(String dateOBS) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        try {
            return sdf.parse(dateOBS);
        } catch (ParseException e) {
            Log.warn("Could not parse date:" + dateOBS + ". Returned null.");
            return null;
        }
    }

}
