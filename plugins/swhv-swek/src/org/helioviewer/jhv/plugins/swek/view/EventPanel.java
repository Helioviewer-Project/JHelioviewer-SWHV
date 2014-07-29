package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JTree;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.model.EventPanelModel;
import org.helioviewer.jhv.plugins.swek.model.EventPanelModelListener;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelEventType;

/**
 * Panel display one event type
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class EventPanel extends JPanel implements EventPanelModelListener, MouseListener {
    /** seriolVersionUID */
    private static final long serialVersionUID = 1057300852220893978L;

    /** The event type for which the event panel is created */
    private final SWEKEventType eventType;

    /** Tree containing the event type and it's sources. */
    private JTree eventTypeTree;

    /** The model for this panel */
    private final EventPanelModel eventPanelModel;

    /**
     * Creates a event panel for a certain
     */
    public EventPanel(SWEKEventType eventType) {
        this.eventType = eventType;
        this.eventPanelModel = new EventPanelModel(new SWEKTreeModelEventType(this.eventType));
        this.eventPanelModel.addEventPanelModelListener(this);
        initVisisualComponents();
    }

    /**
     * Initializes the visual components
     */
    private void initVisisualComponents() {
        setLayout(new BorderLayout());
        this.eventTypeTree = new JTree(this.eventPanelModel);
        this.eventTypeTree.setSelectionModel(null);
        this.eventTypeTree.addMouseListener(this);
        this.eventTypeTree.setCellRenderer(new SWEKEventTreeRenderer());
        add(this.eventTypeTree, BorderLayout.CENTER);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int clickedOnRow = this.eventTypeTree.getRowForLocation(e.getX(), e.getY());
        this.eventPanelModel.rowClicked(clickedOnRow);
        Log.debug("Clicked on " + clickedOnRow);
        this.eventTypeTree.revalidate();
        this.eventTypeTree.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

}
