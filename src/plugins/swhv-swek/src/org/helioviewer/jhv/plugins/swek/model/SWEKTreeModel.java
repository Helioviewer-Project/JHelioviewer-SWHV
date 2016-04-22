package org.helioviewer.jhv.plugins.swek.model;

import java.util.HashSet;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;

/**
 * This model manages all the SWEKEventTypeTreeModels and delegate events. This
 * was primarily created to handle the selection in the distributed event trees.
 *
 * The SWEKTreeModel is the central point of access.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKTreeModel {

    private static SWEKTreeModel singletonInstance;
    private static final HashSet<SWEKTreeModelListener> listeners = new HashSet<SWEKTreeModelListener>();

    private SWEKTreeModel() {
    }

    public static SWEKTreeModel getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new SWEKTreeModel();
        }
        return singletonInstance;
    }

    public void addSWEKTreeModelListener(SWEKTreeModelListener swekTreeModelListener) {
        listeners.add(swekTreeModelListener);
    }

    public void removeSWEKTreeModelListener(SWEKTreeModelListener swekTreeModelListener) {
        listeners.remove(swekTreeModelListener);
    }

    public void subTreeCollapsed() {
        fireTreeExpansionChanged();
    }

    public void subTreeExpanded() {
        fireTreeExpansionChanged();
    }

    public void setStartLoading(SWEKEventType eventType) {
        for (SWEKTreeModelListener l : listeners) {
            l.startedDownloadingEventType(eventType);
        }
    }

    public void setStopLoading(SWEKEventType eventType) {
        for (SWEKTreeModelListener l : listeners) {
            l.stoppedDownloadingEventType(eventType);
        }
    }

    public void resetEventType(SWEKEventType eventType) {
        setStopLoading(eventType);
    }

    private void fireTreeExpansionChanged() {
        for (SWEKTreeModelListener l : listeners) {
            l.expansionChanged();
        }
    }

}
