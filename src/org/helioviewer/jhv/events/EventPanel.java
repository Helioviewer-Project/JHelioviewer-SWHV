package org.helioviewer.jhv.events;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTree;

import org.helioviewer.jhv.gui.components.base.JHVTreeCell;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class EventPanel extends JPanel {

    private final SWEKGroup group;

    public EventPanel(SWEKGroup _group) {
        group = _group;

        JTree tree = new JTree(group);
        tree.setEditable(true);
        tree.setShowsRootHandles(true);
        tree.setSelectionModel(null);
        tree.setCellRenderer(new JHVTreeCell.Renderer());
        tree.setCellEditor(new JHVTreeCell.Editor());
        tree.setRowHeight(0); // force calculation of nodes heights
        group.setTree(tree);

        setLayout(new BorderLayout());
        add(tree);
    }

    public void serialize(JSONObject jo) {
        JSONObject go = new JSONObject();
        group.getSuppliers().forEach(supplier -> go.put(supplier.getName(), supplier.isSelected()));
        jo.put(group.getName(), go);
    }

    public void deserialize(JSONObject jo) {
        JSONObject go = jo.optJSONObject(group.getName());
        if (go != null)
            group.getSuppliers().forEach(supplier -> supplier.activate(go.optBoolean(supplier.getName(), false)));
    }

}
