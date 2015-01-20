package org.helioviewer.plugins.eveplugin.controller;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.ViewListenerDistributor;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.plugins.eveplugin.draw.DrawableType;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;

public class DrawController implements ZoomControllerListener, LineDataSelectorModelListener, ViewListener, JHVEventHighlightListener {

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
        ViewListenerDistributor.getSingletonInstance().addViewListener(this);
    }

    public static DrawController getSingletonInstance() {
        if (instance == null) {
            instance = new DrawController();
        }
        return instance;
    }

    private DrawControllerData getDrawControllerData(String identifier) {
        synchronized (drawControllerData) {
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
    }

    public void addDrawControllerListenerForAllIdentifiers(DrawControllerListener listener) {
        forAllPlotIdentifiers.add(listener);
        synchronized (drawControllerData) {
            for (String identifier : drawControllerData.keySet()) {
                DrawControllerData dcd = getDrawControllerData(identifier);
                dcd.addDrawControllerListener(listener);
            }
        }
    }

    public void removeDrawControllerListenerForAllIdentifiers(DrawControllerListener listener) {
        forAllPlotIdentifiers.remove(listener);
        synchronized (drawControllerData) {
            for (String identifier : drawControllerData.keySet()) {
                DrawControllerData dcd = getDrawControllerData(identifier);
                dcd.removeDrawControllerListener(listener);
            }
        }
    }

    public void addDrawControllerListener(DrawControllerListener listener, String identifier) {
        synchronized (drawControllerData) {
            DrawControllerData dcd = getDrawControllerData(identifier);
            dcd.addDrawControllerListener(listener);
            listener.drawRequest();
        }
    }

    public void removeDrawControllerListener(DrawControllerListener listener, String identifier) {
        synchronized (drawControllerData) {
            DrawControllerData dcd = getDrawControllerData(identifier);
            dcd.removeDrawControllerListener(listener);
        }
    }

    public void addDrawableElement(DrawableElement element, String identifier) {
        synchronized (drawControllerData) {
            addDrawableElement(element, identifier, true);
        }
    }

    public void updateDrawableElement(DrawableElement drawableElement, String identifier) {
        synchronized (drawControllerData) {
            removeDrawableElement(drawableElement, identifier, false);
            this.addDrawableElement(drawableElement, identifier, false);
        }
        this.fireRedrawRequest(identifier);
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
        synchronized (drawControllerData) {
            removeDrawableElement(element, identifier, true);
        }
    }

    public int getNumberOfYAxis(String identifier) {
        synchronized (drawControllerData) {
            DrawControllerData dcd = getDrawControllerData(identifier);
            return dcd.getyAxisSet().size();
        }
    }

    public Set<YAxisElement> getYAxisElements(String identifier) {
        synchronized (drawControllerData) {
            DrawControllerData dcd = getDrawControllerData(identifier);
            return dcd.getyAxisSet();
        }
    }

    public Map<DrawableType, Set<DrawableElement>> getDrawableElements(String identifier) {
        synchronized (drawControllerData) {
            DrawControllerData dcd = getDrawControllerData(identifier);
            return dcd.getDrawableElements();
        }
    }

    public List<DrawableElement> getAllDrawableElements(String identifier) {
        synchronized (drawControllerData) {
            Collection<Set<DrawableElement>> allValues = getDrawableElements(identifier).values();
            ArrayList<DrawableElement> deList = new ArrayList<DrawableElement>();
            for (Set<DrawableElement> tempList : allValues) {
                deList.addAll(tempList);
            }
            return deList;
        }
    }

    public boolean hasElementsToBeDrawn(String identifier) {
        synchronized (drawControllerData) {
            List<DrawableElement> allElements = this.getAllDrawableElements(identifier);
            return !allElements.isEmpty();
        }
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

    public void setAvailableRange(Range availableRange) {
        fireRedrawRequest();
    }

    public void setSelectedRange(Range selectedRange) {
        fireRedrawRequest();
    }

    public void setInterval(Interval<Date> interval) {
        this.interval = interval;
        fireRedrawRequest();
    }

    private void fireRedrawRequest() {
        synchronized (drawControllerData) {
            for (DrawControllerData dcd : drawControllerData.values()) {
                // Log.info("DrawController listeners size: " +
                // dcd.getListeners().size());
                synchronized (dcd.getListeners()) {
                    for (DrawControllerListener l : dcd.getListeners()) {
                        // Log.info("Draw Controller listener : " + l);
                        l.drawRequest();
                    }
                }
            }
        }
    }

    private void fireRedrawRequest(String identifier) {
        synchronized (drawControllerData) {
            DrawControllerData dcd = getDrawControllerData(identifier);
            synchronized (dcd.getListeners()) {
                for (DrawControllerListener l : dcd.getListeners()) {
                    l.drawRequest();
                }
            }
        }
    }

    @Override
    public void availableIntervalChanged(Interval<Date> newInterval) {
        // TODO Auto-generated method stub

    }

    @Override
    public void selectedIntervalChanged(Interval<Date> newInterval, boolean keepFullValueSpace) {
        setInterval(newInterval);
    }

    @Override
    public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        fireRedrawRequest(element.getPlotIdentifier());

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
            synchronized (dcd.getListeners()) {
                for (DrawControllerListener l : dcd.getListeners()) {
                    l.drawMovieLineRequest(time);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.helioviewer.viewmodel.view.ViewListener#viewChanged(org.helioviewer
     * .viewmodel.view.View, org.helioviewer.viewmodel.changeevent.ChangeEvent)
     */
    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
        if ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView())) {
            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(new Runnable() {
                    private Date date;

                    @Override
                    public void run() {
                        fireRedrawRequestMovieFrameChanged(date);
                    }

                    public Runnable init(Date date) {
                        this.date = date;
                        return this;
                    }
                }.init(timestampReason.getNewDateTime().getTime()));
            } else {
                fireRedrawRequestMovieFrameChanged(timestampReason.getNewDateTime().getTime());
            }
        }
    }

    public Date getLastDateWithData() {
        synchronized (drawControllerData) {
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
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        fireRedrawRequest();
    }
}
