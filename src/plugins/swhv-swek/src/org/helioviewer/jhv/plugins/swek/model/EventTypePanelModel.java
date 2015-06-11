package org.helioviewer.jhv.plugins.swek.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;

/**
 * The model of the event type panel. This model is a TreeModel and is used by
 * the tree on the event type panel.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class EventTypePanelModel implements TreeModel, TreeExpansionListener {

    /** The event type for this model */
    private final SWEKTreeModelEventType eventType;

    /** Holds the TreeModelListeners */
    private final List<TreeModelListener> listeners;

    /** Holds the EventPanelModelListeners */
    private final List<EventTypePanelModelListener> panelModelListeners;

    /** Local instance of the tree model */
    private final SWEKTreeModel treeModelInstance;

    /**
     * Creates a SWEKTreeModel for the given SWEK event type.
     *
     * @param eventType
     *            The event type for which to create the tree model
     */
    public EventTypePanelModel(SWEKTreeModelEventType eventType) {
        this.eventType = eventType;
        listeners = new ArrayList<TreeModelListener>();
        treeModelInstance = SWEKTreeModel.getSingletonInstance();
        panelModelListeners = new ArrayList<EventTypePanelModelListener>();
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
            for (SWEKTreeModelSupplier supplier : eventType.getSwekTreeSuppliers()) {
                supplier.setCheckboxSelected(eventType.isCheckboxSelected());
            }
            if (eventType.isCheckboxSelected()) {
                fireNewEventTypeActive(eventType.getSwekEventType());
            } else {
                fireNewEventTypeInActive(eventType.getSwekEventType());
            }
        } else if (row > 0 && row <= eventType.getSwekTreeSuppliers().size()) {
            SWEKTreeModelSupplier supplier = eventType.getSwekTreeSuppliers().get(row - 1);
            supplier.setCheckboxSelected(!supplier.isCheckboxSelected());
            if (supplier.isCheckboxSelected()) {
                eventType.setCheckboxSelected(true);
            } else {
                boolean eventTypeSelected = false;
                for (SWEKTreeModelSupplier stms : eventType.getSwekTreeSuppliers()) {
                    eventTypeSelected = eventTypeSelected || stms.isCheckboxSelected();
                }
                SWEKTreeModel.getSingletonInstance().resetEventType(eventType.getSwekEventType());
                eventType.setCheckboxSelected(eventTypeSelected);
            }
            if (supplier.isCheckboxSelected()) {
                fireNewEventTypeAndSourceActive(eventType.getSwekEventType(), supplier.getSwekSupplier().getSource(), supplier.getSwekSupplier());
            } else {
                fireNewEventTypeAndSourceInActive(eventType.getSwekEventType(), supplier.getSwekSupplier().getSource(), supplier.getSwekSupplier());
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.
     * TreeModelListener)
     */
    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
     */
    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof SWEKTreeModelEventType) {
            return ((SWEKTreeModelEventType) parent).getSwekTreeSuppliers().get(index);
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
     */
    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof SWEKTreeModelEventType) {
            return ((SWEKTreeModelEventType) parent).getSwekEventType().getSuppliers().size();
        } else {
            return 0;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object,
     * java.lang.Object)
     */
    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if ((parent instanceof SWEKTreeModelEventType) && (child instanceof SWEKTreeModelSupplier)) {
            int count = 0;
            for (SWEKTreeModelSupplier supplier : ((SWEKTreeModelEventType) parent).getSwekTreeSuppliers()) {
                if (supplier.equals(child)) {
                    return count;
                } else {
                    count++;
                }
            }
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
        if (node instanceof SWEKTreeModelEventType) {
            return false;
        } else {
            return true;
        }
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
        listeners.remove(l);

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

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.swing.event.TreeExpansionListener#treeCollapsed(javax.swing.event
     * .TreeExpansionEvent)
     */
    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        treeModelInstance.subTreeCollapsed();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.swing.event.TreeExpansionListener#treeExpanded(javax.swing.event
     * .TreeExpansionEvent)
     */
    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        treeModelInstance.subTreeExpanded();
    }

    /**
     * Informs the listeners about an event type and source that became active.
     *
     * @param eventType
     *            the event type that became active
     * @param swekSource
     *            the source that became active
     * @param swekSupplier
     *            the supplier that became active
     */
    private void fireNewEventTypeAndSourceActive(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier swekSupplier) {
        for (EventTypePanelModelListener l : panelModelListeners) {
            l.newEventTypeAndSourceActive(eventType, swekSource, swekSupplier);
        }
    }

    /**
     * Informs the listeners about an event type and source that became
     * inactive.
     *
     * @param eventType
     *            the event type that became inactive
     * @param swekSource
     *            the source that became inactive
     */
    private void fireNewEventTypeAndSourceInActive(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier) {
        for (EventTypePanelModelListener l : panelModelListeners) {
            l.newEventTypeAndSourceInActive(eventType, swekSource, supplier);
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
            fireNewEventTypeAndSourceActive(swekEventType, supplier.getSource(), supplier);
        }
    }

    /**
     * Informs the listeners about an event type that became inactive.
     *
     * @param swekEventType
     *            the event type that became inactive
     */
    private void fireNewEventTypeInActive(SWEKEventType swekEventType) {
        for (SWEKSupplier supplier : swekEventType.getSuppliers()) {
            fireNewEventTypeAndSourceInActive(swekEventType, supplier.getSource(), supplier);
        }
    }

}
