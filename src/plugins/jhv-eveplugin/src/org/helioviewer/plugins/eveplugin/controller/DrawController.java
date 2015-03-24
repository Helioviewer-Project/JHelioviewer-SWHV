package org.helioviewer.plugins.eveplugin.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.TimeListener;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.plugins.eveplugin.draw.DrawableType;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.viewmodel.view.View;

public class DrawController implements ZoomControllerListener, LineDataSelectorModelListener, JHVEventHighlightListener, LayersListener, TimeListener {

    private static DrawController instance;
    private final Map<String, DrawControllerData> drawControllerData;
    private Interval<Date> interval;
    private final List<DrawControllerListener> forAllPlotIdentifiers;
    private Thread viewChangedThread;

    private DrawController() {
        drawControllerData = Collections.synchronizedMap(new HashMap<String, DrawControllerData>());
        ZoomController.getSingletonInstance().addZoomControllerListener(this);
        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);
        forAllPlotIdentifiers = new ArrayList<DrawControllerListener>();
        LayersModel.getSingletonInstance().addLayersListener(this);
        Displayer.addTimeListener(this);
    }

    public static DrawController getSingletonInstance() {
        if (instance == null) {
            instance = new DrawController();
        }
        return instance;
    }

    private DrawControllerData getDrawControllerData(String identifier) {
        Log.debug("Called by " + Thread.currentThread().getName());
        Thread.dumpStack();
        DrawControllerData dcd = new DrawControllerData();
        if (drawControllerData.containsKey(identifier)) {
            dcd = drawControllerData.get(identifier);
        } else {
            for (DrawControllerListener l : forAllPlotIdentifiers) {
                dcd.addDrawControllerListener(l);
            }
            drawControllerData.put(identifier, dcd);
        }
        return dcd;
    }

    public void addDrawControllerListenerForAllIdentifiers(DrawControllerListener listener) {
        forAllPlotIdentifiers.add(listener);
        for (String identifier : drawControllerData.keySet()) {
            DrawControllerData dcd = getDrawControllerData(identifier);
            dcd.addDrawControllerListener(listener);
        }

    }

    public void removeDrawControllerListenerForAllIdentifiers(DrawControllerListener listener) {
        forAllPlotIdentifiers.remove(listener);
        for (String identifier : drawControllerData.keySet()) {
            DrawControllerData dcd = getDrawControllerData(identifier);
            dcd.removeDrawControllerListener(listener);
        }

    }

    public void addDrawControllerListener(DrawControllerListener listener, String identifier) {
        DrawControllerData dcd = getDrawControllerData(identifier);
        dcd.addDrawControllerListener(listener);
    }

    public void removeDrawControllerListener(DrawControllerListener listener, String identifier) {
        DrawControllerData dcd = getDrawControllerData(identifier);
        dcd.removeDrawControllerListener(listener);
    }

    public void updateDrawableElement(DrawableElement drawableElement, String identifier) {
        removeDrawableElement(drawableElement, identifier, false);
        this.addDrawableElement(drawableElement, identifier, false);

        if (drawableElement.hasElementsToDraw()) {
            this.fireRedrawRequest(identifier);
        }
    }

    private void addDrawableElement(DrawableElement element, String identifier, boolean redraw) {
        DrawControllerData dcd = getDrawControllerData(identifier);
        dcd.addDrawableElement(element);
        if (redraw) {
            this.fireRedrawRequest(identifier);
        }
    }

    private void removeDrawableElement(DrawableElement element, String identifier, boolean redraw) {
        DrawControllerData dcd = getDrawControllerData(identifier);
        dcd.removeDrawableElement(element);
        if (redraw) {
            this.fireRedrawRequest(identifier);
        }
    }

    public void removeDrawableElement(DrawableElement element, String identifier) {
        removeDrawableElement(element, identifier, true);
    }

    public int getNumberOfYAxis(String identifier) {
        DrawControllerData dcd = getDrawControllerData(identifier);
        return dcd.getyAxisSet().size();
    }

    public Set<YAxisElement> getYAxisElements(String identifier) {
        DrawControllerData dcd = getDrawControllerData(identifier);
        return dcd.getyAxisSet();
    }

    public Map<DrawableType, Set<DrawableElement>> getDrawableElements(String identifier) {
        DrawControllerData dcd = getDrawControllerData(identifier);
        return dcd.getDrawableElements();
    }

    public List<DrawableElement> getAllDrawableElements(String identifier) {
        Collection<Set<DrawableElement>> allValues = getDrawableElements(identifier).values();
        ArrayList<DrawableElement> deList = new ArrayList<DrawableElement>();
        for (Set<DrawableElement> tempList : allValues) {
            deList.addAll(tempList);
        }
        return deList;
    }

    public boolean hasElementsToBeDrawn(String identifier) {
        List<DrawableElement> allElements = this.getAllDrawableElements(identifier);
        return !allElements.isEmpty();
    }

    public boolean getIntervalAvailable() {
        if (interval == null) {
            return false;
        } else {
            return interval.getStart() != null && interval.getEnd() != null;
        }
    }

    public Interval<Date> getInterval() {
        return interval;
    }

    public void setSelectedRange(Range selectedRange) {
        fireRedrawRequest();
    }

    public void setInterval(Interval<Date> interval) {
        if (this.interval == null || !this.interval.equals(interval)) {
            this.interval = interval;
            fireRedrawRequest();
        }
    }

    private void fireRedrawRequest() {
        for (DrawControllerData dcd : drawControllerData.values()) {
            for (DrawControllerListener l : dcd.getListeners()) {
                l.drawRequest();
            }
        }
    }

    private void fireRedrawRequest(String identifier) {
        DrawControllerData dcd = getDrawControllerData(identifier);
        for (DrawControllerListener l : dcd.getListeners()) {
            l.drawRequest();
        }
    }

    @Override
    public void availableIntervalChanged(Interval<Date> newInterval) {
    }

    @Override
    public void selectedIntervalChanged(Interval<Date> newInterval, boolean keepFullValueSpace) {
        setInterval(newInterval);
    }

    @Override
    public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
    }

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        fireRedrawRequest(element.getPlotIdentifier());
    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
        fireRedrawRequest(element.getPlotIdentifier());
    }

    private void fireRedrawRequestMovieFrameChanged(final Date time) {
        for (DrawControllerData dcd : drawControllerData.values()) {
            for (DrawControllerListener l : dcd.getListeners()) {
                l.drawMovieLineRequest(time);
            }
        }
    }

    @Override
    public void timeChanged(Date date) {
        fireRedrawRequestMovieFrameChanged(date);
    }

    public Date getLastDateWithData() {
        Date lastDate = null;
        for (DrawControllerData dcd : drawControllerData.values()) {
            if (dcd.getLastDateWithData() != null) {
                if (lastDate == null || lastDate.before(dcd.getLastDateWithData())) {
                    lastDate = dcd.getLastDateWithData();
                }
            }
        }
        return lastDate;
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        fireRedrawRequest();
    }

    @Override
    public void layerAdded(int idx) {
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
        View activeView = LayersModel.getSingletonInstance().getActiveView();
        if (activeView == null) {
            fireRedrawRequestMovieFrameChanged(null);
        }
    }

    @Override
    public void layerChanged(int idx) {
    }

    @Override
    public void activeLayerChanged(int idx) {
    }

}
