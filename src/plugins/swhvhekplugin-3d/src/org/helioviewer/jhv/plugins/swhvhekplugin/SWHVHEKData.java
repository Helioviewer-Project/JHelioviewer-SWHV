package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.container.cache.JHVEventCacheResult;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
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
    private JHVDate beginDate = null;
    private JHVDate endDate = null;

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
            if (beginDate == null || first.milli < beginDate.milli) {
                beginDate = first;
                request = true;
            }
        }

        if (last != null) {
            if (endDate == null || last.milli > endDate.milli) {
                endDate = last;
                request = true;
            }
        }

        if (request && endDate != null && beginDate != null) {
            JHVEventContainer.getSingletonInstance().requestForInterval(beginDate.milli, endDate.milli, SWHVHEKData.this);
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
    public void newEventsReceived(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventList) {
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
            Displayer.display();
        }
    }

    public ArrayList<JHVRelatedEvents> getActiveEvents(long timestamp) {
        ArrayList<JHVRelatedEvents> activeEvents = new ArrayList<JHVRelatedEvents>();
        if (beginDate != null && endDate != null) {
            JHVEventCacheResult result = JHVEventCache.getSingletonInstance().get(beginDate.milli, endDate.milli, beginDate.milli, endDate.milli);
            Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> data = result.getAvailableEvents();
            for (Map.Entry<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> v1 : data.entrySet()) {
                for (Map.Entry<JHVEventCache.SortedDateInterval, JHVRelatedEvents> v2 : v1.getValue().entrySet()) {
                    JHVRelatedEvents evr = v2.getValue();
                    if (evr.getStart() <= timestamp && timestamp <= evr.getEnd())
                        activeEvents.add(evr);
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
