package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;

public class ZoomManagerData implements PlotAreaSpaceListener {

    private Map<Long, ZoomDataConfig> zoomDataConfigMap;
    private boolean isAreaInitialized;
    private List<ZoomManagerListener> listeners;
    private Rectangle displaySize;

    public ZoomManagerData() {
        zoomDataConfigMap = new HashMap<Long, ZoomDataConfig>();
        listeners = new ArrayList<ZoomManagerListener>();
        isAreaInitialized = false;
        this.displaySize = new Rectangle();
    }

    public Map<Long, ZoomDataConfig> getZoomDataConfigMap() {
        return zoomDataConfigMap;
    }

    public void setZoomDataConfigMap(Map<Long, ZoomDataConfig> zoomDataConfigMap) {
        this.zoomDataConfigMap = zoomDataConfigMap;
    }

    public boolean isAreaInitialized() {
        return isAreaInitialized;
    }

    public void setAreaInitialized(boolean isAreaInitialized) {
        this.isAreaInitialized = isAreaInitialized;
    }

    public List<ZoomManagerListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<ZoomManagerListener> listeners) {
        this.listeners = listeners;
    }

    public void addZoomManagerListener(ZoomManagerListener listener) {
        listeners.add(listener);
    }

    public void removeZoomManagerListener(ZoomManagerListener listener) {
        listeners.remove(listener);
    }

    public Rectangle getDisplaySize() {
        return displaySize;
    }

    public void setDisplaySize(Rectangle displaySize) {
        this.displaySize = displaySize;
        for (ZoomDataConfig zsc : zoomDataConfigMap.values()) {
            zsc.setDisplaySize(displaySize);
        }
        isAreaInitialized = true;
    }

    public void addToZoomDataConfigMap(long iD, ZoomDataConfig config) {
        zoomDataConfigMap.put(iD, config);
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime) {
        // TODO Auto-generated method stub

    }

}
