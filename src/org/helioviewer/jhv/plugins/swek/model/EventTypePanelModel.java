package org.helioviewer.jhv.plugins.swek.model;

import java.util.HashSet;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.json.JSONObject;

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
            boolean selected = group.isSelected();

            for (SWEKSupplier supplier : suppliers) {
                supplier.setSelected(selected);
                fireActivateGroupAndSupplier(group, supplier, selected);
            }
        } else if (row > 0 && row <= suppliers.size()) {
            SWEKSupplier supplier = suppliers.get(row - 1);
            selectSupplier(supplier, !supplier.isSelected());
        }
    }

    private void selectSupplier(SWEKSupplier supplier, boolean selected) {
        supplier.setSelected(selected);
        if (selected) {
            group.setSelected(true);
        } else {
            boolean groupSelected = false;
            for (SWEKSupplier stms : group.getSuppliers()) {
                groupSelected |= stms.isSelected();
            }
            // SWEKTreeModel.setStopLoading(group);
            group.setSelected(groupSelected);
        }
        fireActivateGroupAndSupplier(group, supplier, selected);
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

    private void fireActivateGroupAndSupplier(SWEKGroup swekGroup, SWEKSupplier swekSupplier, boolean active) {
        for (EventTypePanelModelListener l : panelModelListeners) {
            l.activateGroupAndSupplier(swekGroup, swekSupplier, active);
        }
    }

    public void serialize(JSONObject jo) {
        JSONObject go = new JSONObject();
        for (SWEKSupplier supplier : group.getSuppliers()) {
            go.put(supplier.getName(), supplier.isSelected());
        }
        jo.put(group.getName(), go);
    }

    public void deserialize(JSONObject jo) {
        if (jo.has(group.getName())) {
            JSONObject go = jo.getJSONObject(group.getName());
            for (SWEKSupplier supplier : group.getSuppliers()) {
                selectSupplier(supplier, go.optBoolean(supplier.getName(), false));
            }
        }
    }

}
