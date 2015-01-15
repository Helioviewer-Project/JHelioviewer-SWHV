package org.helioviewer.jhv.data.guielements;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelation;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.CollapsiblePane;

/**
 * Popup displaying informations about a HEK event.
 * 
 * <p>
 * This panel is a JDialog, so that it can be displayed on top of an GLCanvas,
 * which is not possible for other swing components.
 * 
 * <p>
 * For further informations about solar events, see
 * {@link org.helioviewer.jhv.solarevents}.
 * 
 * @author Markus Langenberg
 * @author Malte Nuhn
 * @author Bram.Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKEventInformationDialog extends JDialog implements WindowFocusListener, FocusListener, WindowListener {

    private static final long serialVersionUID = 1L;

    private JPanel allTablePanel;

    private CollapsiblePane standardParameters;

    private CollapsiblePane advancedParameters;

    private CollapsiblePane allParameters;

    private EventDescriptionPanel eventDescriptionPanel;

    private JHVEvent event;

    private Integer nrOfWindowsOpened;

    private final SWEKEventInformationDialog parent;

    /**
     * 
     * 
     * @param event
     */
    public SWEKEventInformationDialog(JHVEvent event) {
        super(ImageViewerGui.getMainFrame(), event.getJHVEventType().getEventType());
        initDialog(event);
        parent = null;
    }

    public SWEKEventInformationDialog(JHVEvent event, SWEKEventInformationDialog parent, boolean modal) {
        super(parent, event.getJHVEventType().getEventType(), modal);
        initDialog(event);
        this.parent = parent;
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        synchronized (nrOfWindowsOpened) {
            if (nrOfWindowsOpened == 0) {
                this.setVisible(false);
                this.dispose();
            }
        }
    }

    @Override
    public void focusGained(FocusEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void focusLost(FocusEvent arg0) {
        synchronized (nrOfWindowsOpened) {
            if (nrOfWindowsOpened == 0) {
                this.setVisible(false);
                this.dispose();
            }
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowClosing(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowClosed(WindowEvent e) {
        decrementNrOfWindows();
    }

    @Override
    public void windowIconified(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowActivated(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    private void initDialog(JHVEvent event) {
        this.event = event;
        nrOfWindowsOpened = 0;
        eventDescriptionPanel = new EventDescriptionPanel(event);

        initAllTablePanel();
        initParameterCollapsiblePanels();

        this.setLayout(new GridBagLayout());
        // this.setUndecorated(true);
        // this.setMinimumSize(new Dimension(250, 50));

        // this.addWindowFocusListener(this);
        // this.addFocusListener(this);

        this.setAlwaysOnTop(false);

        GridBagConstraints eventDescriptionConstraint = new GridBagConstraints();
        eventDescriptionConstraint.gridx = 0;
        eventDescriptionConstraint.gridy = 0;
        eventDescriptionConstraint.weightx = 1;
        eventDescriptionConstraint.weighty = 0;
        eventDescriptionConstraint.anchor = GridBagConstraints.WEST;
        eventDescriptionConstraint.fill = GridBagConstraints.BOTH;

        add(eventDescriptionPanel, eventDescriptionConstraint);

        GridBagConstraints allTablePanelConstraint = new GridBagConstraints();
        allTablePanelConstraint.gridx = 0;
        allTablePanelConstraint.gridy = 1;
        allTablePanelConstraint.gridwidth = 1;
        allTablePanelConstraint.weightx = 1;
        allTablePanelConstraint.weighty = 1;
        allTablePanelConstraint.fill = GridBagConstraints.BOTH;
        this.add(new JScrollPane(allTablePanel), allTablePanelConstraint);
    }

    /**
     * initialize the allTablePanel
     * 
     */
    private void initAllTablePanel() {
        allTablePanel = new JPanel();
        allTablePanel.setLayout(new BoxLayout(allTablePanel, BoxLayout.Y_AXIS));
    }

    /**
     * initialize collapsible panels
     */
    private void initParameterCollapsiblePanels() {
        ParameterTablePanel standardParameterPanel = new ParameterTablePanel(event.getVisibleEventParameters());
        standardParameters = new CollapsiblePane("Standard Parameters", standardParameterPanel, true);
        allTablePanel.add(standardParameters);

        ParameterTablePanel advancedParameterPanel = new ParameterTablePanel(event.getNonVisibleEventParameters());
        advancedParameters = new CollapsiblePane("Advanced Parameters", advancedParameterPanel, false);
        allTablePanel.add(advancedParameters);

        ParameterTablePanel allEventsPanel = new ParameterTablePanel(event.getAllEventParameters());
        allParameters = new CollapsiblePane("All Parameters", allEventsPanel, false);
        allTablePanel.add(allParameters);

        if (!event.getEventRelationShip().getPrecedingEvents().isEmpty()) {
            allTablePanel.add(createRelatedEventsCollapsiblePane("Preceding Events", event.getEventRelationShip().getPrecedingEvents()));
        }

        if (!event.getEventRelationShip().getNextEvents().isEmpty()) {
            allTablePanel.add(createRelatedEventsCollapsiblePane("Following Events", event.getEventRelationShip().getNextEvents()));
        }

        if (!event.getEventRelationShip().getRelatedEventsByRule().isEmpty()) {
            allTablePanel.add(createRelatedEventsCollapsiblePane("Other Related Events", event.getEventRelationShip().getRelatedEventsByRule()));
        }
    }

    private CollapsiblePane createRelatedEventsCollapsiblePane(String relation, Map<String, JHVEventRelation> relations) {
        JPanel allPrecedingEvents = new JPanel();
        allPrecedingEvents.setLayout(new BoxLayout(allPrecedingEvents, BoxLayout.Y_AXIS));
        for (final JHVEventRelation er : relations.values()) {
            if (er.getTheEvent() != null) {
                JPanel eventAndButtonPanel = new JPanel();

                JButton detailsButton = new JButton("Details");
                detailsButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (er.getTheEvent() != null) {
                            incrementNrOfWindows();
                            SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(er.getTheEvent(), SWEKEventInformationDialog.this, false);
                            // dialog.setLocation();
                            dialog.addWindowListener(SWEKEventInformationDialog.this);
                            dialog.validate();
                            dialog.pack();
                            dialog.setVisible(true);
                        }
                    }
                });

                eventAndButtonPanel.setLayout(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.fill = GridBagConstraints.BOTH;
                c.anchor = GridBagConstraints.CENTER;
                c.weightx = 1;
                c.weighty = 1;
                eventAndButtonPanel.add(new EventDescriptionPanel(er.getTheEvent()), c);

                c.gridy = 1;
                c.fill = GridBagConstraints.NONE;
                c.weightx = 0;
                c.weighty = 0;
                c.anchor = GridBagConstraints.EAST;

                eventAndButtonPanel.add(detailsButton, c);
                allPrecedingEvents.add(eventAndButtonPanel);
            }
        }
        return new CollapsiblePane(relation, allPrecedingEvents, false);
    }

    private void incrementNrOfWindows() {
        synchronized (nrOfWindowsOpened) {
            nrOfWindowsOpened++;
        }
    }

    private void decrementNrOfWindows() {
        synchronized (nrOfWindowsOpened) {
            nrOfWindowsOpened--;
        }
    }
}
