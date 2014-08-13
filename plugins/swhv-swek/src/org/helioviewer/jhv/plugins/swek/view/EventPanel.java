package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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
        this.downloadManager = SWEKDownloadManager.getSingletonInstance();
        this.eventType = eventType;
        this.eventPanelModel = new EventTypePanelModel(new SWEKTreeModelEventType(this.eventType));
        this.eventPanelModel.addEventPanelModelListener(this.downloadManager);
        initVisisualComponents();
    }

    /**
     * Initializes the visual components
     */
    private void initVisisualComponents() {
        setLayout(new BorderLayout());
        this.eventTypeTree = new JTree(this.eventPanelModel);
        this.eventTypeTree.setShowsRootHandles(true);
        this.eventTypeTree.setSelectionModel(null);
        this.eventTypeTree.addMouseListener(this);
        this.eventTypeTree.addTreeExpansionListener(this.eventPanelModel);
        this.eventTypeTree.setCellRenderer(new SWEKEventTreeRenderer());
        add(this.eventTypeTree, BorderLayout.CENTER);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int clickedOnRow = this.eventTypeTree.getRowForLocation(e.getX(), e.getY());
        this.eventPanelModel.rowClicked(clickedOnRow);
        this.eventTypeTree.revalidate();
        this.eventTypeTree.repaint();
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
