package org.helioviewer.plugins.eveplugin.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * @author Stephan Pagel
 * */
public class DrawController implements BandControllerListener, ZoomControllerListener, EVECacheControllerListener, LayersListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private final String identifier;
    
    private final LinkedList<DrawControllerListener> listeners = new LinkedList<DrawControllerListener>();
    private final HashMap<Band, EVEValues> dataMap = new HashMap<Band, EVEValues>();
    
    private Interval<Date> interval = new Interval<Date>(null, null);
    private Range selectedRange = new Range();
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public DrawController(final String identifier) {
        this.identifier = identifier;
        
        BandController.getSingletonInstance().addBandControllerListener(this);
        ZoomController.getSingletonInstance().addZoomControllerListener(this);
        EVECacheController.getSingletonInstance().addControllerListener(this);
        LayersModel.getSingletonInstance().addLayersListener(this);
    }
    
    public void addDrawControllerListener(final DrawControllerListener listener) {
        listeners.add(listener);
    }
    
    public void removeDrawControllerListener(final DrawControllerListener listener) {
        listeners.remove(listener);
    }
    
    private void addToMap(final Band band) {     
        dataMap.put(band, retrieveData(band, interval));
        fireRedrawRequest(true);
    }
    
    private void removeFromMap(final Band band) {
        if (dataMap.containsKey(band)) {
            dataMap.remove(band);
            
            fireRedrawRequest(true);
        }
    }
    
    private void updateBand(final Band band) {
        dataMap.put(band, retrieveData(band, interval));
    }
    
    private void updateBands() {
        for (final Band band : dataMap.keySet())
            updateBand(band);
        
        fireRedrawRequest(true);
    }
    
    public void setSelectedRange(final Range newSelectedRange) {
        selectedRange = new Range(newSelectedRange);
        
        fireRedrawRequest(false);
    }
    
    public void setSelectedRangeMaximal() {
        fireRedrawRequest(true);
    }
    
    private void fireRedrawRequest(final boolean maxRange) {
        final Band[] bands = dataMap.keySet().toArray(new Band[0]);
        final LinkedList<EVEValues> values = new LinkedList<EVEValues>();
        final Range availableRange = new Range();
        
        for (EVEValues v : dataMap.values()) {
            if (v != null) {
            	
                availableRange.setMin(v.getMinimumValue());
                availableRange.setMax(v.getMaximumValue());
                
                values.add(v);
            }
        }
        
        if (maxRange)
            selectedRange = new Range();
        
        adjustAvailableRangeBorders(availableRange);
        checkSelectedRange(availableRange, selectedRange);
        
        for (DrawControllerListener listener : listeners) {
            listener.drawRequest(interval, bands, values.toArray(new EVEValues[0]), availableRange, selectedRange);
        }
    }
    
    private void adjustAvailableRangeBorders(final Range availableRange) {
        final double minLog10 = Math.log10(availableRange.min);
        final double maxLog10 = Math.log10(availableRange.max);
        
        if (minLog10 < 0) {
            availableRange.min = Math.pow(10, ((int)(minLog10 * 100 - 1)) / 100.0);    
        } else {
            availableRange.min = Math.pow(10, ((int)(minLog10 * 100)) / 100.0);
        }
            
        if (maxLog10 < 0) {
            availableRange.max = Math.pow(10, ((int)(maxLog10 * 100)) / 100.0);
        } else {
            availableRange.max = Math.pow(10, ((int)(maxLog10 * 100 + 1)) / 100.0);    
        }
    }
    
    private void checkSelectedRange(final Range availableRange, final Range selectedRange) {
        if (selectedRange.min > availableRange.max || selectedRange.max < availableRange.min) {
            selectedRange.min = availableRange.min;
            selectedRange.max = availableRange.max;
            
            return;
        }
        
        if (selectedRange.min < availableRange.min) {
            selectedRange.min = availableRange.min;
        }
        
        if (selectedRange.max > availableRange.max) {
            selectedRange.max = availableRange.max;
        }
    }
    
    private void fireRedrawRequestMovieFrameChanged(final Date time) {
        for (DrawControllerListener listener : listeners) {
            listener.drawRequest(time);
        }
    }
    
    private final EVEValues retrieveData(final Band band, final Interval<Date> interval) {
        return EVECacheController.getSingletonInstance().getDataInInterval(band, interval);
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Zoom Controller Listener
    // //////////////////////////////////////////////////////////////////////////////
    public void availableIntervalChanged(final Interval<Date> newInterval) {}
    
    public void selectedIntervalChanged(final Interval<Date> newInterval) {
        interval = newInterval;
        
        updateBands();
    }
    
    public void selectedResolutionChanged(final API_RESOLUTION_AVERAGES newResolution) {}
    
    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void bandAdded(final Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            addToMap(band);    
        }
    }

    public void bandRemoved(final Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            removeFromMap(band);    
        }
    }

    public void bandUpdated(final Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            if (band.isVisible())
                addToMap(band);
            else
                removeFromMap(band);    
        }
    }
    
    public void bandGroupChanged(final String identifier) {
        if (this.identifier.equals(identifier)) {
            dataMap.clear();
            
            final Band[] activeBands = BandController.getSingletonInstance().getBands(identifier);
            
            for (final Band band : activeBands) {
                dataMap.put(band, retrieveData(band, interval));    
            }
            
            fireRedrawRequest(true);    
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // EVE Cache Controller Listener
    // //////////////////////////////////////////////////////////////////////////////
    
    public void dataAdded(final Band band) {        
        if (dataMap.containsKey(band)) {
            updateBand(band);
            fireRedrawRequest(true);
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Layers Listener
    // //////////////////////////////////////////////////////////////////////////////
    
    public void layerAdded(int idx) {}

    public void layerRemoved(View oldView, int oldIdx) {}

    public void layerChanged(int idx) {}

    public void activeLayerChanged(int idx) {}

    public void viewportGeometryChanged() {}

    public void timestampChanged(int idx) {
        final ImmutableDateTime timestamp = LayersModel.getSingletonInstance().getCurrentFrameTimestamp(idx);
        fireRedrawRequestMovieFrameChanged(timestamp.getTime());
    }

    public void subImageDataChanged() {}

    public void layerDownloaded(int idx) {}
}
