package org.helioviewer.jhv.events;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.Timer;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.base.JHVTreeCell;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class EventPanel extends JPanel implements SWEKDownloadListener {

    private final SWEKGroup group;

    private final JLabel loadingLabel = new JLabel("    ");
    private final JLayer<JComponent> over = new JLayer<>(null, UITimer.busyIndicator);

    // The timer handling the loading animation
    private final Timer loadingTimer = new Timer(500, e -> over.repaint());

    public EventPanel(SWEKGroup _group) {
        group = _group;
        setLayout(new BorderLayout());
        SWEKDownloadManager.addListener(this);

        JTree tree = new JTree(new EventPanelModel(group));
        tree.setEditable(true);
        tree.setShowsRootHandles(true);
        tree.setSelectionModel(null);
        tree.setCellRenderer(new JHVTreeCell.Renderer());
        tree.setCellEditor(new JHVTreeCell.Editor());
        tree.setRowHeight(0); // force calculation of nodes heights
        add(tree, BorderLayout.CENTER);

        JPanel busyPanel = new JPanel();
        busyPanel.setBackground(tree.getBackground());
        busyPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        busyPanel.add(over);
        add(busyPanel, BorderLayout.LINE_END);
    }

    @Override
    public void startedDownload(SWEKGroup _group) {
        if (group.equals(_group) && !loadingTimer.isRunning()) {
            over.setView(loadingLabel);
            loadingTimer.start();
        }
    }

    @Override
    public void stoppedDownload(SWEKGroup _group) {
        if (group.equals(_group) && loadingTimer.isRunning()) {
            loadingTimer.stop();
            over.setView(null);
        }
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
