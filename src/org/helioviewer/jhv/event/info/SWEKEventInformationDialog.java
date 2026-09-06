package org.helioviewer.jhv.event.info;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.EventCache;
import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.thread.Task;

// Popup displaying information about a HEK event.
// This panel is a JDialog so it can appear above the heavyweight render surface.
@SuppressWarnings("serial")
public final class SWEKEventInformationDialog extends JDialog {

    private JPanel allTablePanel;

    private DataCollapsiblePanel standardParameters;
    private DataCollapsiblePanel allParameters;
    private DataCollapsiblePanel relatedEventsPanel;
    private DataCollapsiblePanel otherRelatedEventsPanel;

    private SolarEvent event;
    private final RelatedEvents related;

    public SWEKEventInformationDialog(RelatedEvents _related, SolarEvent _event) {
        super(MainFrame.get(), _event.getSupplier().group().getName());
        setType(Window.Type.UTILITY); // avoids tab on macOS when Prefer tabs is always
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        event = _event;
        related = _related;

        initAllTablePanel();
        initParameterCollapsiblePanels();
        setCollapsiblePanels();

        setLayout(new GridBagLayout());

        GridBagConstraints eventDescriptionConstraint = new GridBagConstraints();
        eventDescriptionConstraint.gridx = 0;
        eventDescriptionConstraint.gridy = 0;
        eventDescriptionConstraint.weightx = 1;
        eventDescriptionConstraint.weighty = 0;
        eventDescriptionConstraint.anchor = GridBagConstraints.LINE_START;
        eventDescriptionConstraint.fill = GridBagConstraints.BOTH;

        add(new EventDescriptionPanel(_related, event), eventDescriptionConstraint);

        GridBagConstraints allTablePanelConstraint = new GridBagConstraints();
        allTablePanelConstraint.gridx = 0;
        allTablePanelConstraint.gridy = 1;
        allTablePanelConstraint.gridwidth = 1;
        allTablePanelConstraint.weightx = 1;
        allTablePanelConstraint.weighty = 1;
        allTablePanelConstraint.fill = GridBagConstraints.BOTH;

        add(allTablePanel, allTablePanelConstraint);

        Task.submit("event-info", new DatabaseCallable(event), this::onSuccessDatabase, SWEKEventInformationDialog::onFailureDatabase);
    }

    private record DatabaseCallable(SolarEvent event) implements Callable<EventDatabase.EventDetails> {
        @Override
        public EventDatabase.EventDetails call() throws Exception {
            return EventDatabase.getEventDetails(event.getUniqueID());
        }
    }

    private void onSuccessDatabase(@Nonnull EventDatabase.EventDetails details) {
        event = details.event();
        List<SolarEvent> relatedEvents = details.relatedEvents();
        if (!relatedEvents.isEmpty())
            otherRelatedEventsPanel = createOtherRelatedEventsCollapsiblePane(relatedEvents);

        initParameterCollapsiblePanels();
        repack();
        repaint();
    }

    private static void onFailureDatabase(String ignoredLogContext, Throwable t) {
        Log.error(t);
    }

    private void initAllTablePanel() {
        allTablePanel = new JPanel(new GridBagLayout());
    }

    private void initParameterCollapsiblePanels() {
        ParameterTablePanel standardParameterPanel = new ParameterTablePanel(event.getVisibleEventParameters());
        standardParameters = new DataCollapsiblePanel("Standard Parameters", standardParameterPanel, true, this::repack);

        ParameterTablePanel allEventsPanel = new ParameterTablePanel(event.getAllEventParameters());
        allParameters = new DataCollapsiblePanel("All Parameters", allEventsPanel, false, this::repack);

        List<SolarEvent> relatedEvents = related.getAssociatedEvents(event);
        if (!relatedEvents.isEmpty())
            relatedEventsPanel = createRelatedEventsCollapsiblePane(related, relatedEvents);
    }

    private void setCollapsiblePanels() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.PAGE_START;
        gc.weighty = standardParameters.isExpanded() ? 1 : 0;
        allTablePanel.add(standardParameters, gc);

        gc.gridy = 1;
        gc.weighty = allParameters.isExpanded() ? 1 : 0;
        allTablePanel.add(allParameters, gc);

        int gridYPosition = 2;

        if (relatedEventsPanel != null) {
            gc.gridy = gridYPosition;
            gc.weighty = relatedEventsPanel.isExpanded() ? 1 : 0;
            allTablePanel.add(relatedEventsPanel, gc);
            gridYPosition++;
        }

        if (otherRelatedEventsPanel != null) {
            gc.gridy = gridYPosition;
            gc.weighty = otherRelatedEventsPanel.isExpanded() ? 1 : 0;
            allTablePanel.add(otherRelatedEventsPanel, gc);
            //gridYPosition++;
        }
    }

    private DataCollapsiblePanel createRelatedEventsCollapsiblePane(RelatedEvents related, List<SolarEvent> relations) {
        JPanel eventPanels = new JPanel();
        eventPanels.setLayout(new BoxLayout(eventPanels, BoxLayout.PAGE_AXIS));
        relations.forEach(ev -> eventPanels.add(createEventPanel(related, ev)));
        return new DataCollapsiblePanel("Related Events", new JScrollPane(eventPanels), false, this::repack);
    }

    private DataCollapsiblePanel createOtherRelatedEventsCollapsiblePane(List<SolarEvent> events) {
        JPanel eventPanels = new JPanel();
        eventPanels.setLayout(new BoxLayout(eventPanels, BoxLayout.PAGE_AXIS));
        Colors.Data colors = new Colors.Data();
        for (SolarEvent relatedEvent : events) {
            RelatedEvents relatedEvents = EventCache.getRelatedEvents(relatedEvent.getUniqueID());
            if (relatedEvents == null)
                relatedEvents = new RelatedEvents(relatedEvent, colors.getNextColor());
            eventPanels.add(createEventPanel(relatedEvents, relatedEvent));
        }
        return new DataCollapsiblePanel("Other Related Events", new JScrollPane(eventPanels), false, this::repack);
    }

    private static JPanel createEventPanel(RelatedEvents related, SolarEvent event) {
        JButton detailsButton = new JButton("Details");
        detailsButton.addActionListener(e -> {
            SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(related, event);
            dialog.pack();
            dialog.setVisible(true);
        });

        JPanel eventAndButtonPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        eventAndButtonPanel.add(new EventDescriptionPanel(related, event), c);

        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.LINE_END;
        eventAndButtonPanel.add(detailsButton, c);

        return eventAndButtonPanel;
    }

    private void repack() {
        allTablePanel.removeAll();
        setCollapsiblePanels();
        pack();
    }

}
