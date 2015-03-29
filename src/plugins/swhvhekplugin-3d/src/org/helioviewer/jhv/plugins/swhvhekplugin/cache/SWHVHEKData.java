package org.helioviewer.jhv.plugins.swhvhekplugin.cache;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.container.cache.JHVEventCacheResult;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

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
    private Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> data;
    private ArrayList<JHVEvent> events;
    private Date beginDate = null;
    private Date endDate = null;

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
        int numLayers = LayersModel.getSingletonInstance().getNumLayers();
        beginDate = null;
        endDate = null;
        for (int i = 0; i < numLayers; i++) {
            View nextView = LayersModel.getSingletonInstance().getLayer(i);
            JHVJPXView jpxView = nextView.getAdapter(JHVJPXView.class);
            if (jpxView != null) {
                for (int frame = 0; frame <= jpxView.getMaximumFrameNumber(); frame++) {
                    ImmutableDateTime date = jpxView.getFrameDateTime(frame);
                    if (beginDate == null || date.getTime().getTime() < beginDate.getTime()) {
                        beginDate = date.getTime();
                    }
                    if (endDate == null || date.getTime().getTime() > endDate.getTime()) {
                        endDate = date.getTime();
                    }
                }

                if (beginDate != null && endDate != null) {
                    JHVEventContainer.getSingletonInstance().requestForInterval(beginDate, endDate, SWHVHEKData.this);
                }
            }
        }
    }

    @Override
    public void layerRemoved(int oldIdx) {
    }

    @Override
    public void activeLayerChanged(View view) {
    }

    @Override
    public void newEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventList) {
    }

    @Override
    public void cacheUpdated() {
        if (beginDate != null && endDate != null) {
            JHVEventCacheResult result = JHVEventCache.getSingletonInstance().get(beginDate, endDate);
            data = result.getAvailableEvents();
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
            Displayer.display();
        }
    }

    public Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> getData() {
        return data;
    }

    public ArrayList<JHVEvent> getActiveEvents(Date currentDate) {
        ArrayList<JHVEvent> activeEvents = new ArrayList<JHVEvent>();
        if (events != null) {
            for (JHVEvent event : events) {
                if (event != null && event.getStartDate() != null && event.getEndDate() != null) {
                    if (event.getStartDate().getTime() < currentDate.getTime() && event.getEndDate().getTime() > currentDate.getTime()) {
                        activeEvents.add(event);
                    }
                    event.addHighlightListener(Displayer.getSingletonInstance());
                } else {
                    Log.warn("Possibly something strange is going on with incoming events. Either the date or the event is null");
                }
            }
        }
        return activeEvents;
    }

}
