package org.helioviewer.jhv.plugins.swek.model;

import java.util.HashSet;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.data.event.SWEKEventType;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.json.JSONObject;

/**
 * The model of the event type panel. This model is a TreeModel and is used by
 * the tree on the event type panel.
 */
public class EventTypePanelModel implements TreeModel {

    /** The event type for this model */
    private final SWEKEventType eventType;

    // private final List<TreeModelListener> listeners = new HashSet<TreeModelListener>();

    /** Holds the EventPanelModelListeners */
    private final HashSet<EventTypePanelModelListener> panelModelListeners = new HashSet<>();

    /**
     * Creates a SWEKTreeModel for the given SWEK event type.
     *
     * @param _eventType
     *            The event type for which to create the tree model
     */
    public EventTypePanelModel(SWEKEventType _eventType) {
        eventType = _eventType;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.
     * TreeModelListener)
     */
    @Override
    public void addTreeModelListener(TreeModelListener l) {
        // listeners.add(l);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.
     * TreeModelListener)
     */
    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        // listeners.remove(l);
    }

    /**
     * Adds a new event panel model listener.
     *
     * @param listener
     *            the listener to add
     */
    public void addEventPanelModelListener(EventTypePanelModelListener listener) {
        panelModelListeners.add(listener);
    }

    /**
     * Removes an event panel model listener.
     *
     * @param listener
     *            the listener to remove
     */
    public void removeEventPanelModelListener(EventTypePanelModelListener listener) {
        panelModelListeners.remove(listener);
    }

    /**
     * Informs the model about the row that was clicked. The clicked row will be
     * selected or unselected if it previously respectively was unselected or
     * selected.
     *
     * @param row
     *            The row that was selected
     */
    public void rowClicked(int row) {
        if (row == 0) {
            eventType.setCheckboxSelected(!eventType.isCheckboxSelected());
            for (SWEKSupplier supplier : eventType.getSuppliers()) {
                supplier.setCheckboxSelected(eventType.isCheckboxSelected());
            }
            if (eventType.isCheckboxSelected()) {
                fireNewEventTypeActive(eventType);
            } else {
                fireNewEventTypeInactive(eventType);
            }
        } else if (row > 0 && row <= eventType.getSuppliers().size()) {
            SWEKSupplier supplier = eventType.getSuppliers().get(row - 1);
            supplier.setCheckboxSelected(!supplier.isCheckboxSelected());
            if (supplier.isCheckboxSelected()) {
                eventType.setCheckboxSelected(true);
            } else {
                boolean eventTypeSelected = false;
                for (SWEKSupplier stms : eventType.getSuppliers()) {
                    eventTypeSelected = eventTypeSelected || stms.isCheckboxSelected();
                }
                SWEKTreeModel.resetEventType(eventType);
                eventType.setCheckboxSelected(eventTypeSelected);
            }
            if (supplier.isCheckboxSelected()) {
                fireNewEventTypeAndSourceActive(eventType, supplier);
            } else {
                fireNewEventTypeAndSourceInactive(eventType, supplier);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
     */
    @Override
    public Object getChild(Object parent, int index) {
        return parent instanceof SWEKEventType ? ((SWEKEventType) parent).getSuppliers().get(index) : null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
     */
    @Override
    public int getChildCount(Object parent) {
        return parent instanceof SWEKEventType ? ((SWEKEventType) parent).getSuppliers().size() : 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object,
     * java.lang.Object)
     */
    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if ((parent instanceof SWEKEventType) && (child instanceof SWEKSupplier)) {
            return ((SWEKEventType) parent).getSuppliers().indexOf(child);
        }
        return -1;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.tree.TreeModel#getRoot()
     */
    @Override
    public Object getRoot() {
        return eventType;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
     */
    @Override
    public boolean isLeaf(Object node) {
        return !(node instanceof SWEKEventType);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath,
     * java.lang.Object)
     */
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    /**
     * Informs the listeners about an event type and source that became active.
     *
     * @param swekEventType
     *            the event type that became active
     * @param swekSupplier
     *            the supplier that became active
     */
    private void fireNewEventTypeAndSourceActive(SWEKEventType swekEventType, SWEKSupplier swekSupplier) {
        for (EventTypePanelModelListener l : panelModelListeners) {
            l.newEventTypeAndSourceActive(swekEventType, swekSupplier);
        }
    }

    /**
     * Informs the listeners about an event type and source that became
     * inactive.
     *
     * @param swekEventType
     *            the event type that became inactive
     * @param supplier
     *            the source that became inactive
     */
    private void fireNewEventTypeAndSourceInactive(SWEKEventType swekEventType, SWEKSupplier supplier) {
        for (EventTypePanelModelListener l : panelModelListeners) {
            l.newEventTypeAndSourceInactive(swekEventType, supplier);
        }
    }

    /**
     * Informs the listeners about an event type that became active.
     *
     * @param swekEventType
     *            the event type that became active
     */
    private void fireNewEventTypeActive(SWEKEventType swekEventType) {
        for (SWEKSupplier supplier : swekEventType.getSuppliers()) {
            fireNewEventTypeAndSourceActive(swekEventType, supplier);
        }
    }

    /**
     * Informs the listeners about an event type that became inactive.
     *
     * @param swekEventType
     *            the event type that became inactive
     */
    private void fireNewEventTypeInactive(SWEKEventType swekEventType) {
        for (SWEKSupplier supplier : swekEventType.getSuppliers()) {
            fireNewEventTypeAndSourceInactive(swekEventType, supplier);
        }
    }

    public void serialize(JSONObject swekObject) {
        eventType.serialize(swekObject);
    }

    public void deserialize(JSONObject swekObject) {
        eventType.deserialize(swekObject);
    }

}
