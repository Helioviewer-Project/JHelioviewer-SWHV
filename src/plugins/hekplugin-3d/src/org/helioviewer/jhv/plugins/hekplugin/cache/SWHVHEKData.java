package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.datatype.JHVEvent;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
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
public class SWHVHEKData implements LayersListener, JHVEventHandler {

    /** The singleton instance of the outgoing request manager */
    private static SWHVHEKData instance;
    private Date beginDate;
    private Date endDate;
    private Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> data;
    private ArrayList<JHVEvent> events;

    /** instance of the swek event handler */

    /**
     * private constructor
     */
    private SWHVHEKData() {
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    /**
     * Gets the singleton instance of the outgoing request manager
     *
     * @return the singleton instance
     */
    public static SWHVHEKData getSingletonInstance() {
        if (instance == null) {
            instance = new SWHVHEKData();
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
                        Log.error("Destroy myself with handgrenade. No date-obs in whatever dialect could be found");
                    }
                }
            }
            if (this.beginDate == null || requestDates.get(0).getTime() < this.beginDate.getTime()) {
                this.beginDate = requestDates.get(0);
            }
            if (this.endDate == null || requestDates.get(0).getTime() > this.endDate.getTime()) {
                this.endDate = requestDates.get(requestDates.size() - 1);
            }
            JHVEventContainer.getSingletonInstance().requestForInterval(beginDate, endDate, this);

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

    @Override
    public void newEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventList) {
    }

    @Override
    public void cacheUpdated() {
        data = JHVEventCache.getSingletonInstance().get(beginDate, endDate);
        ArrayList<JHVEvent> events = new ArrayList<JHVEvent>();
        for (String eventType : data.keySet()) {
            for (Date sDate : data.get(eventType).keySet()) {
                for (Date eDate : data.get(eventType).get(sDate).keySet()) {
                    for (JHVEvent event : data.get(eventType).get(sDate).get(eDate)) {
                        events.add(event);
                    }
                }
            }
        }
        this.events = events;
        Displayer.getSingletonInstance().display();
    }

    public Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> getData() {
        return data;
    }

    public ArrayList<JHVEvent> getActiveEvents(Date currentDate) {
        ArrayList<JHVEvent> activeEvents = new ArrayList<JHVEvent>();
        if (this.events != null) {
            for (JHVEvent event : this.events) {
                if (event != null && event.getStartDate() != null && event.getEndDate() != null) {
                    if (event.getStartDate().getTime() < currentDate.getTime() && event.getEndDate().getTime() > currentDate.getTime()) {
                        activeEvents.add(event);
                    }
                } else {
                    Log.warn("Possibly something strange is going on with incoming events. Either the date or the event is null");
                }
            }
        }
        return activeEvents;
    }

}
