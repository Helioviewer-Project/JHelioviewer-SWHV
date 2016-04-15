package org.helioviewer.jhv.data.guielements;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.data.guielements.listeners.DataCollapsiblePanelModelListener;
import org.helioviewer.jhv.data.guielements.model.DataCollapsiblePanelModel;
import org.helioviewer.jhv.gui.ImageViewerGui;

/**
 * Popup displaying informations about a HEK event.
 *
 * This panel is a JDialog, so that it can be displayed on top of an GLCanvas,
 * which is not possible for other swing components.
 */
@SuppressWarnings("serial")
public class SWEKEventInformationDialog extends JDialog implements WindowListener, DataCollapsiblePanelModelListener {

    private JPanel allTablePanel;

    private DataCollapsiblePanel standardParameters;
    private DataCollapsiblePanel allParameters;
    private DataCollapsiblePanel precedingEventsPanel;
    private DataCollapsiblePanel followingEventsPanel;
    private DataCollapsiblePanel otherRelatedEventsPanel;

    private int expandedPanels;

    private EventDescriptionPanel eventDescriptionPanel;

    private final JHVEvent event;
    private final JHVRelatedEvents rEvent;
    private Integer nrOfWindowsOpened;

    private final DataCollapsiblePanelModel model;

    private ArrayList<JHVEvent> otherRelatedEvents;
    private final boolean otherRelatedEventsLoaded;

    public SWEKEventInformationDialog(JHVRelatedEvents revent, JHVEvent event) {
        super(ImageViewerGui.getMainFrame(), revent.getJHVEventType().getEventType().getEventName());
        this.event = event;
        rEvent = revent;
        model = new DataCollapsiblePanelModel();
        model.addListener(this);
        otherRelatedEventsLoaded = false;
        initDialog(revent);
        startOtherRelatedEventsSwingWorker();
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        decrementNrOfWindows();
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    private void initDialog(JHVRelatedEvents revent) {
        nrOfWindowsOpened = 0;
        eventDescriptionPanel = new EventDescriptionPanel(revent, event);

        initAllTablePanel();
        initParameterCollapsiblePanels();
        setCollapsiblePanels();

        setLayout(new GridBagLayout());

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

        add(allTablePanel, allTablePanelConstraint);
    }

    /**
     * initialize the allTablePanel
     *
     */
    private void initAllTablePanel() {
        allTablePanel = new JPanel();
        allTablePanel.setLayout(new GridBagLayout());
    }

    /**
     * initialize collapsible panels
     */
    private void initParameterCollapsiblePanels() {
        ParameterTablePanel standardParameterPanel = new ParameterTablePanel(event.getVisibleEventParameters().values());
        expandedPanels = 1;
        standardParameters = new DataCollapsiblePanel("Standard Parameters", standardParameterPanel, true, model);

        ParameterTablePanel allEventsPanel = new ParameterTablePanel(event.getAllEventParameters().values());
        allParameters = new DataCollapsiblePanel("All Parameters", allEventsPanel, false, model);

        ArrayList<JHVEvent> precedingEvents = rEvent.getPreviousEvents(event);
        if (!precedingEvents.isEmpty()) {
            precedingEventsPanel = createRelatedEventsCollapsiblePane("Preceding Events", rEvent, precedingEvents);
        }

        ArrayList<JHVEvent> nextEvents = rEvent.getNextEvents(event);
        if (!nextEvents.isEmpty()) {
            followingEventsPanel = createRelatedEventsCollapsiblePane("Following Events", rEvent, nextEvents);
        }
    }

    private void setCollapsiblePanels() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.NORTH;
        if (standardParameters.isExpanded()) {
            gc.weighty = 1;
        } else {
            gc.weighty = 0;
        }
        allTablePanel.add(standardParameters, gc);

        gc.gridy = 1;
        if (allParameters.isExpanded()) {
            gc.weighty = 1;
        } else {
            gc.weighty = 0;
        }
        allTablePanel.add(allParameters, gc);

        int gridYPosition = 2;

        if (precedingEventsPanel != null) {
            gc.gridy = gridYPosition;
            if (precedingEventsPanel.isExpanded()) {
                gc.weighty = 1;
            } else {
                gc.weighty = 0;
            }
            allTablePanel.add(precedingEventsPanel, gc);
            gridYPosition++;
        }

        if (followingEventsPanel != null) {
            gc.gridy = gridYPosition;
            if (followingEventsPanel.isExpanded()) {
                gc.weighty = 1;
            } else {
                gc.weighty = 0;
            }
            allTablePanel.add(followingEventsPanel, gc);
            gridYPosition++;
        }

        if (otherRelatedEventsPanel != null) {
            gc.gridy = gridYPosition;
            if (otherRelatedEventsPanel.isExpanded()) {
                gc.weighty = 1;
            } else {
                gc.weighty = 0;
            }
            allTablePanel.add(otherRelatedEventsPanel, gc);
            gridYPosition++;
        }
    }

    private DataCollapsiblePanel createRelatedEventsCollapsiblePane(String relation, JHVRelatedEvents rEvents, ArrayList<JHVEvent> relations) {
        JPanel allPrecedingEvents = new JPanel();
        allPrecedingEvents.setLayout(new BoxLayout(allPrecedingEvents, BoxLayout.Y_AXIS));
        for (final JHVEvent event : relations) {
            allPrecedingEvents.add(createEventPanel(rEvents, event));
        }
        return new DataCollapsiblePanel(relation, new JScrollPane(allPrecedingEvents), false, model);
    }

    private DataCollapsiblePanel createOtherRelatedEventsCollapsiblePane(String relation, ArrayList<JHVRelatedEvents> rEvents) {
        JPanel allPrecedingEvents = new JPanel();
        allPrecedingEvents.setLayout(new BoxLayout(allPrecedingEvents, BoxLayout.Y_AXIS));
        for (final JHVRelatedEvents rEvent : rEvents) {
            if (rEvent.getEvents().size() > 0) {
                allPrecedingEvents.add(createEventPanel(rEvent, rEvent.getEvents().get(0)));
            }
        }
        return new DataCollapsiblePanel(relation, new JScrollPane(allPrecedingEvents), false, model);
    }

    private JPanel createEventPanel(final JHVRelatedEvents rEvents, final JHVEvent event) {
        JPanel eventAndButtonPanel = new JPanel();

        JButton detailsButton = new JButton("Details");
        detailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                incrementNrOfWindows();
                SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(rEvents, event);
                dialog.addWindowListener(SWEKEventInformationDialog.this);
                dialog.validate();
                dialog.pack();
                dialog.setVisible(true);

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
        eventAndButtonPanel.add(new EventDescriptionPanel(rEvents, event), c);

        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.EAST;

        eventAndButtonPanel.add(detailsButton, c);
        return eventAndButtonPanel;

    }

    private void incrementNrOfWindows() {
        nrOfWindowsOpened++;
    }

    private void decrementNrOfWindows() {
        nrOfWindowsOpened--;
    }

    @Override
    public void repack() {
        int newExpandedPanels = nrOfExpandedPanels();
        allTablePanel.removeAll();
        setCollapsiblePanels();

        pack();
        expandedPanels = newExpandedPanels;
    }

    private int nrOfExpandedPanels() {
        int newExpandedPanels = 0;
        if (allParameters.isExpanded()) {
            newExpandedPanels++;
        }
        if (standardParameters.isExpanded()) {
            newExpandedPanels++;
        }

        if ((followingEventsPanel != null && followingEventsPanel.isExpanded())) {
            newExpandedPanels++;
        }
        if ((precedingEventsPanel != null && precedingEventsPanel.isExpanded())) {
            newExpandedPanels++;
        }
        if ((otherRelatedEventsPanel != null && otherRelatedEventsPanel.isExpanded())) {
            newExpandedPanels++;
        }
        return newExpandedPanels;
    }

    private void startOtherRelatedEventsSwingWorker() {
        SwingWorker<ArrayList<JHVEvent>, Void> worker = new SwingWorker<ArrayList<JHVEvent>, Void>() {
            @Override
            public ArrayList<JHVEvent> doInBackground() {
                ArrayList<JHVEvent> jhvEvents = JHVEventContainer.getSingletonInstance().getOtherRelations(event);

                return jhvEvents;
            }

            @Override
            public void done() {
                try {
                    ArrayList<JHVEvent> events = get();
                    JHVEventCache cache = JHVEventCache.getSingletonInstance();
                    for (JHVEvent ev : events) {
                        cache.add(ev);
                    }
                    Set<JHVRelatedEvents> rEventsSet = new HashSet<JHVRelatedEvents>();
                    for (JHVEvent jhvEvent : events) {
                        rEventsSet.add(cache.getRelatedEvents(jhvEvent.getUniqueID()));
                    }
                    ArrayList<JHVRelatedEvents> rEvents = new ArrayList<JHVRelatedEvents>();
                    rEvents.addAll(rEventsSet);

                    if (!rEvents.isEmpty()) {
                        otherRelatedEventsPanel = createOtherRelatedEventsCollapsiblePane("Other Related Events", rEvents);
                        SWEKEventInformationDialog.this.repack();
                        SWEKEventInformationDialog.this.repaint();
                    }
                } catch (InterruptedException ignore) {
                } catch (java.util.concurrent.ExecutionException e) {
                }

            }
        };
        worker.execute();
    }

}
