package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.container.cache.JHVEventCacheResult;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.viewmodel.view.View;

/**
 * This class intercepts changes of the layers and request data from the
 * JHVEventContainer.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWHVHEKData implements LayersListener, JHVEventHandler {

    private static SWHVHEKData instance;
    private Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> data;
    private Date beginDate = null;
    private Date endDate = null;

    private SWHVHEKData() {
    }

    public static SWHVHEKData getSingletonInstance() {
        if (instance == null) {
            instance = new SWHVHEKData();
        }
        return instance;
    }

    public void reset() {
        instance = null;
    }

    public void requestEvents() {
        boolean request = false;
        JHVDate first = Layers.getStartDate();
        JHVDate last = Layers.getEndDate();

        if (first != null) {
            if (beginDate == null || first.getTime() < beginDate.getTime()) {
                beginDate = first.getDate();
                request = true;
            }
        }

        if (last != null) {
            if (endDate == null || last.getTime() > endDate.getTime()) {
                endDate = last.getDate();
                request = true;
            }
        }

        if (request) {
            JHVEventContainer.getSingletonInstance().requestForInterval(beginDate, endDate, SWHVHEKData.this);
        }
    }

    @Override
    public void layerAdded(View view) {
        requestEvents();
    }

    @Override
    public void activeLayerChanged(View view) {
    }

    @Override
    public void newEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventList) {
        requestEvents();
        displayEvents();
    }

    @Override
    public void cacheUpdated() {
        if (beginDate != null && endDate != null) {
            beginDate = null;
            endDate = null;
            requestEvents();
            displayEvents();
        }
    }

    private void displayEvents() {
        if (beginDate != null && endDate != null) {
            JHVEventCacheResult result = JHVEventCache.getSingletonInstance().get(beginDate, endDate, beginDate, endDate);
            data = result.getAvailableEvents();
            Displayer.display();
        }
    }

    public Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> getData() {
        return data;
    }

    public ArrayList<JHVEvent> getActiveEvents(Date currentDate) {
        ArrayList<JHVEvent> activeEvents = new ArrayList<JHVEvent>();
        if (data != null) {
            for (NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>> v1 : data.values()) {
                for (NavigableMap<Date, List<JHVEvent>> v2 : v1.values()) {
                    for (List<JHVEvent> v3 : v2.values()) {
                        for (JHVEvent event : v3) {
                            if (event != null && event.getStartDate() != null && event.getEndDate() != null) {
                                if (event.getStartDate().getTime() <= currentDate.getTime() && event.getEndDate().getTime() >= currentDate.getTime()) {
                                    activeEvents.add(event);
                                }
                                event.addHighlightListener(Displayer.getSingletonInstance());
                            } else {
                                Log.warn("Possibly something strange is going on with incoming events. Either the date or the event is null");
                            }
                        }
                    }
                }
            }
        }
        return activeEvents;
    }

    public static double readCMESpeed(Map<String, JHVEventParameter> params) {
        double speed = 500;
        try {
            if (params.containsKey("cme_radiallinvel"))
                speed = Double.parseDouble(params.get("cme_radiallinvel").getParameterValue());
        } catch (Exception e) {

        }
        return speed;
    }

    public static double readCMEPrincipalAngleDegree(Map<String, JHVEventParameter> params) {
        double principalAngle = 0;
        try {
            if (params.containsKey("event_coord1"))
                principalAngle = Double.parseDouble(params.get("event_coord1").getParameterValue()) + 90;
        } catch (Exception e) {
        }
        return principalAngle;
    }

    public static double readCMEAngularWidthDegree(Map<String, JHVEventParameter> params) {
        double angularWidthDegree = 0;
        try {
            if (params.containsKey("cme_angularwidth"))
                angularWidthDegree = Double.parseDouble(params.get("cme_angularwidth").getParameterValue());
        } catch (Exception e) {
        }
        return angularWidthDegree;
    }

}
