package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.Timer;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelEventType;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelListener;

// Panel to display one event type
@SuppressWarnings("serial")
public class EventPanel extends JPanel implements SWEKTreeModelListener, ActionListener {

    /** The event type for which the event panel is created */
    private final SWEKEventType eventType;

    /** The label holding the loading text */
    private final JLabel loadingLabel;

    /** The timer handling the loading animation */
    private final Timer loadingTimer;

    private int loadingStep;

    public EventPanel(SWEKEventType _eventType) {
        eventType = _eventType;
        setLayout(new BorderLayout());
        SWEKTreeModel.addSWEKTreeModelListener(this);

        loadingTimer = new Timer(500, this);
        loadingStep = 0;

        EventTypePanelModel eventPanelModel = new EventTypePanelModel(new SWEKTreeModelEventType(eventType));
        eventPanelModel.addEventPanelModelListener(SWEKPlugin.downloadManager);

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

        FilterDialog filterDialog = new FilterDialog(eventType);
        SmallPanel filterPanel = new SmallPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setOpaque(true);
        filterPanel.setBackground(Color.WHITE);

        if (eventType.containsFilter()) {
            JButton filterButton = new JButton("Filter");
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

        loadingLabel = new JLabel();
        filterPanel.add(loadingLabel);
        filterPanel.setSmall();
        add(filterPanel, BorderLayout.LINE_END);
    }

    @Override
    public void startedDownloadingEventType(SWEKEventType eventType) {
        if (eventType.equals(this.eventType) && !loadingTimer.isRunning()) {
            loadingLabel.setText("Loading   ");
            loadingStep = 0;
            loadingTimer.start();
        }
    }

    @Override
    public void stoppedDownloadingEventType(SWEKEventType eventType) {
        if (eventType.equals(this.eventType) && loadingTimer.isRunning()) {
            loadingTimer.stop();
            loadingLabel.setText("          ");
            loadingStep = 0;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (loadingStep) {
        case 0:
            loadingLabel.setText("Loading   ");
            break;
        case 1:
            loadingLabel.setText("Loading.  ");
            break;
        case 2:
            loadingLabel.setText("Loading.. ");
            break;
        case 3:
            loadingLabel.setText("Loading...");
            loadingStep = -1;
            break;
        default:
            break;
        }
        loadingStep++;
    }

}
