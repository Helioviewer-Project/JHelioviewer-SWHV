package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.gui.components.tristateCheckbox.TristateCheckBox;

public class HEKCacheSelectionModel implements HEKCacheListener {

    HEKCache cache;
    HEKCacheModel cacheModel;

    private HashMap<HEKPath, Integer> selectionState = new HashMap<HEKPath, Integer>();

    public HEKCacheSelectionModel(HEKCache cache) {
        this.cache = cache;
        this.cacheModel = cache.getModel();
        this.cacheModel.addCacheListener(this);
    }

    /**
     * Return a hashmap of keys and simple interval to be requested, based on
     * the current tree selection
     */
    public HashMap<HEKPath, Vector<Interval<Date>>> getSelection(Interval<Date> curInterval) {

        // TODO: Malte Nuhn - Implement the CacheModelLOCK
        HashMap<HEKPath, Vector<Interval<Date>>> result = new HashMap<HEKPath, Vector<Interval<Date>>>();

        result.putAll(getSelection(cache.getTrackPaths().iterator(), curInterval));

        return result;
    }

    /**
     * Update selection states... Current implementations forces all
     * subdirectories to take same selection as the supernode
     * 
     * @param selPath
     */
    public void invertState(HEKPath selPath) {

        int state = getState(selPath);

        if (state == TristateCheckBox.CHECKED) {
            state = TristateCheckBox.UNCHECKED;
        } else if (state == TristateCheckBox.UNCHECKED) {
            state = TristateCheckBox.CHECKED;
        } else if (state == TristateCheckBox.INDETERMINATE) {
            state = TristateCheckBox.CHECKED;
        }

        setState(selPath, state);
        cache.getController().fireEventsChanged(selPath);

    }

    /**
     * This method tries to find a good selection state for a new path
     * <p>
     * <li>If the parent is in INDETERMINATE state, use the DEFAULT state</li>
     * <li>In all other cases copy the parent's selection state</li>
     * 
     * @param path
     *            - path to find the selection state for
     */
    private void inheritState(HEKPath path) {
        HEKPath parent = path.getParent();

        while (parent != null && parent.isVirtual()) {
            parent = parent.getParent();
        }

        if (parent == null)
            parent = cacheModel.getRoot();

        int parentState = getState(parent);

        // take selection from parent
        if (cacheModel.getChildren(parent, true).size() == 0) {
            setState(path, parentState);
            parent.setVirtual(true);
        } else {
            if (parentState == TristateCheckBox.INDETERMINATE) {
                setState(path, TristateCheckBox.DEFAULT);
            } else {
                setState(path, parentState);
            }
        }
    }

    /**
     * Return a HashMap of selected HEKPaths (keys) with a Vector containing the
     * given timeRange as value
     * 
     * @param result
     * @param keyIterator
     * @param timeRange
     */
    private HashMap<HEKPath, Vector<Interval<Date>>> getSelection(Iterator<HEKPath> keyIterator, Interval<Date> timeRange) {

        HashMap<HEKPath, Vector<Interval<Date>>> result = new HashMap<HEKPath, Vector<Interval<Date>>>();

        while (keyIterator.hasNext()) {

            HEKPath key = keyIterator.next();

            if (selectionState.containsKey(key) && selectionState.get(key) == TristateCheckBox.CHECKED) {

                if (!result.containsKey(key)) {
                    result.put(key, new Vector<Interval<Date>>());
                }

                result.get(key).add(timeRange);

            }
        }

        return result;

    }

    /**
     * Recursively set the state of a given path
     * 
     * @param path
     *            - path that is to be recursively updated
     * @param state
     *            - new selection state
     */
    public void setState(HEKPath path, int state) {
        // TODO: Malte Nuhn - Implement the CacheModelLOCK
        Vector<HEKPath> children = cacheModel.getChildren(path, true);
        for (HEKPath child : children) {
            setState(child, state);
        }

        if (!path.isVirtual()) {
            this.selectionState.put(path, state);
        }

    }

    /**
     * Set states of all paths
     * 
     * @param state
     *            - new selection state
     */
    public void setStates(int state) {
        this.setState(cacheModel.getRoot(), state);
    }

    /**
     * Calculate the selection state of the given path
     * 
     * This recursively calculates the selection state for virtual paths, and
     * returns the stored value for non virtual paths. If no stored state is
     * available for the given path, the DEFAULT state is set, and returned for
     * the given path
     * 
     * @param path
     *            - path of which the selection state is to be determined
     * @return - selection state of the path
     */
    public int getState(HEKPath path) {

        Vector<HEKPath> children = cacheModel.getChildren(path, true);

        // this is a virtual path, that does not exist in the cache
        if (path.isVirtual() && children.size() > 0) {

            int state = TristateCheckBox.DEFAULT;
            int checked = 0;
            int total = 0;

            for (HEKPath child : children) {
                total++;
                state = getState(child);
                if (state == TristateCheckBox.CHECKED)
                    checked++;

                // if one child is INDETERMINATE, the parent is automatically
                // INDETERMINATE, too
                if (state == TristateCheckBox.INDETERMINATE)
                    return TristateCheckBox.INDETERMINATE;
            }

            // if no children exist, this parent is UNCHECKED
            if (total == 0) {
                return TristateCheckBox.UNCHECKED;
            } else {

                // if all children are UNCHECKED, then this parent is also
                // UNCHECKED
                if (checked == 0) {

                    return TristateCheckBox.UNCHECKED;

                    // if all children are CHECKED, this path should be CHECKED,
                    // too
                } else if (checked == total) {

                    return TristateCheckBox.CHECKED;

                }
                return TristateCheckBox.INDETERMINATE;
            }

            // if this path is not virtual
        } else {

            // set it to DEFAULT if the state is not stored
            if (!this.selectionState.containsKey(path)) {
                selectionState.put(path, TristateCheckBox.DEFAULT);
            }

            // return the stored state
            return selectionState.get(path);

        }
    }

    /**
     * Remove all items from the given set of items, that are not selected NOTE:
     * This method works IN PLACE!
     * 
     * @param activeEventCandidates
     * @return only those items that are NOT UNCHECKED
     */
    public void filterSelectedEvents(Vector<HEKEvent> activeEventCandidates) {
        Iterator<HEKEvent> activeEventCandidatesIterator = activeEventCandidates.iterator();
        while (activeEventCandidatesIterator.hasNext()) {
            HEKEvent activeEventCandidate = activeEventCandidatesIterator.next();
            HEKPath activeEventParentPath = activeEventCandidate.getPath().getParent();
            if (this.getState(activeEventParentPath) == TristateCheckBox.UNCHECKED) {
                activeEventCandidatesIterator.remove();
            }
        }
    }

    public void filterSelectedPaths(Vector<HEKPath> activeEventCandidates) {
        Iterator<HEKPath> activeEventCandidatesIterator = activeEventCandidates.iterator();
        while (activeEventCandidatesIterator.hasNext()) {
            HEKPath activePathCandidate = activeEventCandidatesIterator.next();
            HEKPath activeEventParentPath = activePathCandidate.getParent();
            if (this.getState(activeEventParentPath) == TristateCheckBox.UNCHECKED) {
                activeEventCandidatesIterator.remove();
            }
        }
    }

    public void cacheStateChanged() {
    }

    public void eventsChanged(HEKPath path) {
        if (!path.isVirtual() && !selectionState.containsKey(path)) {
            inheritState(path);
        }
    }

    public void structureChanged(HEKPath path) {
    }

}
