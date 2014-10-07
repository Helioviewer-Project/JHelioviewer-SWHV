package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTree;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKDownloadManager;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelEventType;

/**
 * Panel display one event type
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class EventPanel extends JPanel implements MouseListener {
    /** seriolVersionUID */
    private static final long serialVersionUID = 1057300852220893978L;

    /** The event type for which the event panel is created */
    private final SWEKEventType eventType;

    /** Tree containing the event type and it's sources. */
    private JTree eventTypeTree;

    /** The model for this panel */
    private final EventTypePanelModel eventPanelModel;

    /** Instance of the download manager */
    private final SWEKDownloadManager downloadManager;

    /**
     * Creates a event panel for a certain
     */
    public EventPanel(SWEKEventType eventType) {
        downloadManager = SWEKDownloadManager.getSingletonInstance();
        this.eventType = eventType;
        eventPanelModel = new EventTypePanelModel(new SWEKTreeModelEventType(this.eventType));
        eventPanelModel.addEventPanelModelListener(downloadManager);
        initVisisualComponents();
    }

    /**
     * Initializes the visual components
     */
    private void initVisisualComponents() {
        setLayout(new BorderLayout());
        eventTypeTree = new JTree(eventPanelModel);
        eventTypeTree.setShowsRootHandles(true);
        eventTypeTree.setSelectionModel(null);
        eventTypeTree.addMouseListener(this);
        eventTypeTree.addTreeExpansionListener(eventPanelModel);
        eventTypeTree.setCellRenderer(new SWEKEventTreeRenderer());
        add(eventTypeTree, BorderLayout.CENTER);
        final FilterDialog filterDialog = new FilterDialog(eventType);

        if (eventType.containsFilter()) {
            JButton filterButton = new JButton("Filter");
            filterButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    filterDialog.setVisible(true);
                }
            });
            filterButton.addMouseListener(new MouseListener() {

                @Override
                public void mouseReleased(MouseEvent arg0) {
                }

                @Override
                public void mousePressed(MouseEvent arg0) {
                    Point pressedLocation = arg0.getLocationOnScreen();
                    Point windowLocation = new Point(pressedLocation.x, pressedLocation.y - filterDialog.getSize().height);
                    filterDialog.setLocation(windowLocation);
                }

                @Override
                public void mouseExited(MouseEvent arg0) {
                }

                @Override
                public void mouseEntered(MouseEvent arg0) {
                }

                @Override
                public void mouseClicked(MouseEvent arg0) {
                }
            });

            add(filterButton, BorderLayout.LINE_END);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int clickedOnRow = eventTypeTree.getRowForLocation(e.getX(), e.getY());
        eventPanelModel.rowClicked(clickedOnRow);
        eventTypeTree.revalidate();
        eventTypeTree.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

}
