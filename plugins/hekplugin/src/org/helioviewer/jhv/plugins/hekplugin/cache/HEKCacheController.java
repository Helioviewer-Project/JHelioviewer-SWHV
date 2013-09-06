package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.helioviewer.base.math.Interval;

public class HEKCacheController {

    /**
     * Instance of the HEKCache to control
     */
    private HEKCache cache;

    /**
     * Constructor
     * 
     * @param cache
     *            - cache instance to control
     */
    public HEKCacheController(HEKCache cache) {
        this.cache = cache;
    }

    /**
     * Initiate request for the given HEKPath-Interval tuples
     * 
     * @param needed
     *            - hashmap of HEKPath, Interval tuples
     */
    public void requestEvents(final HashMap<HEKPath, Vector<Interval<Date>>> needed) {
        HashMap<HEKPath, Vector<Interval<Date>>> reallyneeded = cache.needed(needed);
        HEKStupidDownloader.getSingletonInstance().requestEvents(cache.getController(), reallyneeded);
    }

    /**
     * Initiate structure request for the given interval
     * 
     * @param needed
     */
    public void requestStructure(final Interval<Date> needed) {
        cache.getModel().clearPartial();
        HEKStupidDownloader.getSingletonInstance().requestStructure(cache.getController(), needed);
    }

    public void setCurInterval(Interval<Date> newPosition) {
        cache.getModel().setCurInterval(newPosition);
    }

    public HEKPath getRootPath() {
        return new HEKPath(cache);
    }

    public void setState(HEKPath rootPath, int state) {
        cache.getLoadingModel().setState(rootPath, state);
    }

    public void feedStructure(Vector<HEKPath> paths, Interval<Date> timeRange) {
        cache.getModel().feedStructure(paths, timeRange);
        cache.getModel().fireStructureChanged(cache.getModel().getRoot());
    }

    public void feedEvents(HashMap<HEKPath, HEKEvent> events, Interval<Date> interval) {
        Vector<HEKPath> changedPaths = cache.getModel().feedEvents(events, interval);
        for (HEKPath changedPath : changedPaths) {
            cache.getModel().fireEventsChanged(changedPath);
        }
    }

    /*
     * public void fireTreeStructureChangedLazy(HEKPath path) { HEKPath visible
     * = cache.getExpansionModel().getFirstVisiblePath(path);
     * this.fireTreeStructureChanged(visible); }
     * 
     * public void fireTreeStructureChanged(final HEKPath hekPath) {
     * cache.getTreeModel().fireTreeStructureChanged(hekPath); }
     */
    public void expandToLevel(int level, boolean override, boolean collapseBelow) {
        cache.getExpansionModel().expandToLevel(level, override, collapseBelow);
    }

    public void setStates(int state) {
        cache.getSelectionModel().setStates(state);
    }

    public void fireStructureChanged(HEKPath selPath) {
        cache.getModel().fireStructureChanged(selPath);
    }

    public void fireEventsChanged(HEKPath selPath) {
        cache.getModel().fireEventsChanged(selPath);
    }

}
