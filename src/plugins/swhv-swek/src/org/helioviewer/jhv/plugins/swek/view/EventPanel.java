package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTree;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKDownloadManager;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelEventType;
import org.helioviewer.jhv.plugins.swek.view.filter.AbstractFilterPanel;
import org.helioviewer.jhv.plugins.swek.view.filter.FilterPanelFactory;

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
        List<AbstractFilterPanel> filterPanels = FilterPanelFactory.createFilterPanel(eventType);
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridLayout(filterPanels.size(), 1));
        filterPanel.setOpaque(false);
        filterPanel.setBackground(Color.white);
        for (AbstractFilterPanel afp : filterPanels) {
            filterPanel.add(afp);
        }
        add(filterPanel, BorderLayout.PAGE_END);
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
