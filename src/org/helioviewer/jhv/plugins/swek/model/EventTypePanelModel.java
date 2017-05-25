package org.helioviewer.jhv.plugins.swek.model;

import java.util.HashSet;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.json.JSONObject;

/**
 * The model of the event type panel. This model is a TreeModel and is used by
 * the tree on the event type panel.
 */
public class EventTypePanelModel implements TreeModel {

    private final SWEKGroup group;

    private final HashSet<EventTypePanelModelListener> panelModelListeners = new HashSet<>();

    public EventTypePanelModel(SWEKGroup _group) {
        group = _group;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        // listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        // listeners.remove(l);
    }

    public void addEventPanelModelListener(EventTypePanelModelListener listener) {
        panelModelListeners.add(listener);
    }

    public void removeEventPanelModelListener(EventTypePanelModelListener listener) {
        panelModelListeners.remove(listener);
    }

    public void rowClicked(int row) {
        List<SWEKSupplier> suppliers = group.getSuppliers();
        if (row == 0) {
            group.setSelected(!group.isSelected());
            for (SWEKSupplier supplier : suppliers) {
                supplier.setSelected(group.isSelected());
            }
            if (group.isSelected()) {
                fireGroupActive(group);
            } else {
                fireGroupInactive(group);
            }
        } else if (row > 0 && row <= suppliers.size()) {
            SWEKSupplier supplier = suppliers.get(row - 1);
            supplier.setSelected(!supplier.isSelected());
            if (supplier.isSelected()) {
                group.setSelected(true);
            } else {
                boolean groupSelected = false;
                for (SWEKSupplier stms : suppliers) {
                    groupSelected |= stms.isSelected();
                }
                SWEKTreeModel.setStopLoading(group);
                group.setSelected(groupSelected);
            }
            if (supplier.isSelected()) {
                fireGroupAndSupplierActive(group, supplier);
            } else {
                fireGroupAndSupplierInactive(group, supplier);
            }
        }
    }

    @Override
    public Object getChild(Object parent, int index) {
        return parent instanceof SWEKGroup ? ((SWEKGroup) parent).getSuppliers().get(index) : null;
    }

    @Override
    public int getChildCount(Object parent) {
        return parent instanceof SWEKGroup ? ((SWEKGroup) parent).getSuppliers().size() : 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if ((parent instanceof SWEKGroup) && (child instanceof SWEKSupplier)) {
            return ((SWEKGroup) parent).getSuppliers().indexOf(child);
        }
        return -1;
    }

    @Override
    public Object getRoot() {
        return group;
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof SWEKSupplier;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    private void fireGroupAndSupplierActive(SWEKGroup swekGroup, SWEKSupplier swekSupplier) {
        for (EventTypePanelModelListener l : panelModelListeners) {
            l.groupAndSupplierActive(swekGroup, swekSupplier);
        }
    }

    private void fireGroupAndSupplierInactive(SWEKGroup swekGroup, SWEKSupplier supplier) {
        for (EventTypePanelModelListener l : panelModelListeners) {
            l.groupAndSupplierInactive(swekGroup, supplier);
        }
    }

    private void fireGroupActive(SWEKGroup swekGroup) {
        for (SWEKSupplier supplier : swekGroup.getSuppliers()) {
            fireGroupAndSupplierActive(swekGroup, supplier);
        }
    }

    private void fireGroupInactive(SWEKGroup swekGroup) {
        for (SWEKSupplier supplier : swekGroup.getSuppliers()) {
            fireGroupAndSupplierInactive(swekGroup, supplier);
        }
    }

    public void serialize(JSONObject swekObject) {
        group.serialize(swekObject);
    }

    public void deserialize(JSONObject swekObject) {
        group.deserialize(swekObject);
    }

}
