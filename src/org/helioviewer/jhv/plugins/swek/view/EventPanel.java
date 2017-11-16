package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
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

import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelListener;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class EventPanel extends JPanel implements SWEKTreeModelListener, ActionListener {

    private final SWEKGroup group;

    private final JLabel loadingLabel = new JLabel("    ");
    private final JLayer<JComponent> layer = new JLayer<>(null, UITimer.busyIndicator);

    // The timer handling the loading animation
    private final Timer loadingTimer = new Timer(500, this);
    private final EventTypePanelModel eventPanelModel;

    public EventPanel(SWEKGroup _group) {
        group = _group;
        setLayout(new BorderLayout());
        SWEKTreeModel.addSWEKTreeModelListener(this);

        eventPanelModel = new EventTypePanelModel(group);

        JTree eventTypeTree = new JTree(eventPanelModel);
        eventTypeTree.setEditable(true);
        eventTypeTree.setShowsRootHandles(true);
        eventTypeTree.setSelectionModel(null);
        eventTypeTree.setCellRenderer(new SWEKEventTreeRenderer(eventTypeTree, eventPanelModel));
        eventTypeTree.setCellEditor(new MyTreeCellEditor(eventTypeTree, (DefaultTreeCellRenderer) eventTypeTree.getCellRenderer()));

        // workaround for Win HiDpi
        if (System.getProperty("jhv.os").equals("windows")) {
            eventTypeTree.setRowHeight(new JCheckBox("J").getPreferredSize().height);
        }

        add(eventTypeTree, BorderLayout.CENTER);

        JPanel filterPanel = new JPanel(new FlowLayout());
        filterPanel.setBackground(eventTypeTree.getBackground());
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        filterPanel.add(layer);
        add(filterPanel, BorderLayout.LINE_END);
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

    public void serialize(JSONObject jo) {
        eventPanelModel.serialize(jo);
    }

    public void deserialize(JSONObject jo) {
        eventPanelModel.deserialize(jo);
    }

    private class MyTreeCellEditor extends DefaultTreeCellEditor  {

        public MyTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
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
