package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.Timer;

import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelListener;
import org.json.JSONObject;

import com.jidesoft.swing.JideButton;

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
        eventTypeTree.setShowsRootHandles(true);
        eventTypeTree.setSelectionModel(null);
        eventTypeTree.setCellRenderer(new SWEKEventTreeRenderer());
        eventTypeTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickedOnRow = eventTypeTree.getRowForLocation(e.getX(), e.getY());
                eventPanelModel.rowClicked(clickedOnRow);
                // eventTypeTree.revalidate();
                eventTypeTree.repaint();
            }
        });

        // workaround for Win HiDpi
        if (System.getProperty("jhv.os").equals("windows")) {
            eventTypeTree.setRowHeight(new JCheckBox("J").getPreferredSize().height);
        }

        add(eventTypeTree, BorderLayout.CENTER);

        JPanel filterPanel = new JPanel(new FlowLayout());
        filterPanel.setBackground(eventTypeTree.getBackground());
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));

        if (group.containsFilter()) {
            FilterDialog filterDialog = new FilterDialog(group);
            JideButton filterButton = new JideButton("Filter");
            filterButton.addActionListener(e -> filterDialog.setVisible(true));
            filterButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point pressedLocation = e.getLocationOnScreen();
                    Point windowLocation = new Point(pressedLocation.x, pressedLocation.y - filterDialog.getSize().height);
                    filterDialog.setLocation(windowLocation);
                }
            });
            filterPanel.add(filterButton);
        }
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

}
