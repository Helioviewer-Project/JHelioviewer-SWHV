package org.helioviewer.jhv.data.guielements;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelation;
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
public class SWEKEventInformationDialog extends JDialog implements WindowFocusListener, FocusListener {

    private static final long serialVersionUID = 1L;

    private JPanel allTablePanel;

    private CollapsiblePane standardParameters;

    private CollapsiblePane advancedParameters;

    private CollapsiblePane allParameters;

    private CollapsiblePane followedby;

    private CollapsiblePane precededBy;

    private CollapsiblePane otherRelatedEvents;

    private final EventDescriptionPanel eventDescriptionPanel;

    private final JHVEvent event;

    /**
     * 
     * 
     * @param event
     */
    public SWEKEventInformationDialog(JHVEvent event) {
        super();

        this.event = event;

        eventDescriptionPanel = new EventDescriptionPanel(event);

        initAllTablePanel();
        initParameterCollapsiblePanels();

        this.setLayout(new GridBagLayout());
        // this.setUndecorated(true);
        // this.setMinimumSize(new Dimension(250, 50));

        this.addWindowFocusListener(this);
        this.addFocusListener(this);

        this.setAlwaysOnTop(true);

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

    @Override
    public void windowGainedFocus(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        this.setVisible(false);
        this.dispose();

    }

    @Override
    public void focusGained(FocusEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void focusLost(FocusEvent arg0) {
        this.setVisible(false);
        this.dispose();

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
            JPanel allPrecedingEvents = new JPanel();
            allPrecedingEvents.setLayout(new BoxLayout(allPrecedingEvents, BoxLayout.Y_AXIS));
            for (JHVEventRelation er : event.getEventRelationShip().getPrecedingEvents().values()) {
                allPrecedingEvents.add(new EventDescriptionPanel(er.getTheEvent()));
            }
            precededBy = new CollapsiblePane("Preceding Events", allPrecedingEvents, false);
            allTablePanel.add(precededBy);
        }

        if (!event.getEventRelationShip().getNextEvents().isEmpty()) {
            JPanel allNextEvents = new JPanel();
            allNextEvents.setLayout(new BoxLayout(allNextEvents, BoxLayout.Y_AXIS));
            for (JHVEventRelation er : event.getEventRelationShip().getNextEvents().values()) {
                allNextEvents.add(new EventDescriptionPanel(er.getTheEvent()));
            }
            followedby = new CollapsiblePane("Succeeding Events", allNextEvents, false);
            allTablePanel.add(followedby);
        }

        if (!event.getEventRelationShip().getRelatedEventsByRule().isEmpty()) {
            JPanel allOtherRelatedEvents = new JPanel();
            allOtherRelatedEvents.setLayout(new BoxLayout(allOtherRelatedEvents, BoxLayout.Y_AXIS));
            for (JHVEventRelation er : event.getEventRelationShip().getRelatedEventsByRule().values()) {
                allOtherRelatedEvents.add(new EventDescriptionPanel(er.getTheEvent()));
            }
            otherRelatedEvents = new CollapsiblePane("Other Related Events", allOtherRelatedEvents, false);
            allTablePanel.add(otherRelatedEvents);
        }
    }
}
