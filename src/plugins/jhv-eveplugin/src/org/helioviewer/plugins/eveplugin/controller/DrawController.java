package org.helioviewer.plugins.eveplugin.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final DrawControllerData drawControllerData;
    private Interval<Date> interval;
    private final List<DrawControllerListener> forAllPlotIdentifiers;

    private DrawController() {
        drawControllerData = new DrawControllerData();
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

    public void addDrawControllerListener(DrawControllerListener listener) {
        drawControllerData.addDrawControllerListener(listener);
    }

    public void removeDrawControllerListener(DrawControllerListener listener) {
        drawControllerData.removeDrawControllerListener(listener);
    }

    public void updateDrawableElement(DrawableElement drawableElement) {
        removeDrawableElement(drawableElement, false);
        this.addDrawableElement(drawableElement, false);

        if (drawableElement.hasElementsToDraw()) {
            this.fireRedrawRequest();
        }
    }

    private void addDrawableElement(DrawableElement element, boolean redraw) {
        drawControllerData.addDrawableElement(element);
        if (redraw) {
            this.fireRedrawRequest();
        }
    }

    private void removeDrawableElement(DrawableElement element, boolean redraw) {
        drawControllerData.removeDrawableElement(element);
        if (redraw) {
            this.fireRedrawRequest();
        }
    }

    public void removeDrawableElement(DrawableElement element) {
        removeDrawableElement(element, true);
    }

    public Set<YAxisElement> getYAxisElements() {
        return drawControllerData.getyAxisSet();
    }

    public Map<DrawableType, Set<DrawableElement>> getDrawableElements() {
        return drawControllerData.getDrawableElements();
    }

    public List<DrawableElement> getAllDrawableElements() {
        Collection<Set<DrawableElement>> allValues = getDrawableElements().values();
        ArrayList<DrawableElement> deList = new ArrayList<DrawableElement>();
        for (Set<DrawableElement> tempList : allValues) {
            deList.addAll(tempList);
        }
        return deList;
    }

    public boolean hasElementsToBeDrawn() {
        List<DrawableElement> allElements = this.getAllDrawableElements();
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
        for (DrawControllerListener l : drawControllerData.getListeners()) {
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
        fireRedrawRequest();
    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
        fireRedrawRequest();
    }

    private void fireRedrawRequestMovieFrameChanged(final Date time) {
        for (DrawControllerListener l : drawControllerData.getListeners()) {
            l.drawMovieLineRequest(time);
        }
    }

    @Override
    public void timeChanged(Date date) {
        fireRedrawRequestMovieFrameChanged(date);
    }

    public Date getLastDateWithData() {
        Date lastDate = null;
        if (drawControllerData.getLastDateWithData() != null) {
            if (lastDate == null || lastDate.before(drawControllerData.getLastDateWithData())) {
                lastDate = drawControllerData.getLastDateWithData();
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
