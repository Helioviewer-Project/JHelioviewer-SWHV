package org.helioviewer.jhv.data.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.helioviewer.jhv.data.event.SWEKDownloadManager;
import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.helioviewer.jhv.gui.UITimer;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class EventPanel extends JPanel implements SWEKTreeModelListener, ActionListener {

    private final SWEKGroup group;

    private final JLabel loadingLabel = new JLabel("    ");
    private final JLayer<JComponent> layer = new JLayer<>(null, UITimer.busyIndicator);

    // The timer handling the loading animation
    private final Timer loadingTimer = new Timer(500, this);

    public EventPanel(SWEKGroup _group) {
        group = _group;
        setLayout(new BorderLayout());
        SWEKTreeModel.addSWEKTreeModelListener(this);

        JTree eventTypeTree = new JTree(new EventPanelModel(group));
        eventTypeTree.setEditable(true);
        eventTypeTree.setShowsRootHandles(true);
        eventTypeTree.setSelectionModel(null);
        eventTypeTree.setCellRenderer(new SWEKEventTreeRenderer(eventTypeTree, this));
        eventTypeTree.setCellEditor(new MyTreeCellEditor(eventTypeTree, (DefaultTreeCellRenderer) eventTypeTree.getCellRenderer()));

        // workaround for Win HiDpi
        if (System.getProperty("jhv.os").equals("windows")) {
            eventTypeTree.setRowHeight(new JCheckBox("J").getPreferredSize().height);
        }

        add(eventTypeTree, BorderLayout.CENTER);

        JPanel busyPanel = new JPanel();
        busyPanel.setBackground(eventTypeTree.getBackground());
        busyPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        busyPanel.add(layer);
        add(busyPanel, BorderLayout.LINE_END);
    }

    @Override
    public void startedDownloadingGroup(SWEKGroup _group) {
        if (group.equals(_group) && !loadingTimer.isRunning()) {
            layer.setView(loadingLabel);
            loadingTimer.start();
        }
    }

    @Override
    public void stoppedDownloadingGroup(SWEKGroup _group) {
        if (group.equals(_group) && loadingTimer.isRunning()) {
            loadingTimer.stop();
            layer.setView(null);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        layer.repaint();
    }

    public void selectGroup(SWEKGroup group, boolean selected) {
        group.setSelected(selected);
        for (SWEKSupplier supplier : group.getSuppliers()) {
            supplier.setSelected(selected);
            SWEKDownloadManager.activateSupplier(supplier, selected);
        }
    }

    public void selectSupplier(SWEKSupplier supplier, boolean selected) {
        supplier.setSelected(selected);
        if (selected) {
            group.setSelected(true);
        } else {
            boolean groupSelected = false;
            for (SWEKSupplier stms : group.getSuppliers()) {
                groupSelected |= stms.isSelected();
            }
            group.setSelected(groupSelected);
        }
        SWEKDownloadManager.activateSupplier(supplier, selected);
    }

    public void serialize(JSONObject jo) {
        JSONObject go = new JSONObject();
        for (SWEKSupplier supplier : group.getSuppliers()) {
            go.put(supplier.getName(), supplier.isSelected());
        }
        jo.put(group.getName(), go);
    }

    public void deserialize(JSONObject jo) {
        JSONObject go = jo.optJSONObject(group.getName());
        if (go != null)
            for (SWEKSupplier supplier : group.getSuppliers()) {
                selectSupplier(supplier, go.optBoolean(supplier.getName(), false));
            }
    }

    private class MyTreeCellEditor extends DefaultTreeCellEditor  {

        MyTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
            super(tree, renderer);
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
            return renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

    }

}
