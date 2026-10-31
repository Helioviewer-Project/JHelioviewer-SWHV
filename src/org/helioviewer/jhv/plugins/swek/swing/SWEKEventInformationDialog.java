package org.helioviewer.jhv.plugins.swek.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.CollapsiblePane;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.Task;

// Popup displaying information about a HEK event.
// This panel is a JDialog so it can appear above the heavyweight render surface.
@SuppressWarnings("serial")
final class SWEKEventInformationDialog extends JDialog {

    private final SWEKSession session;
    private final SWEKSwingUI ui;
    private final JPanel allTablePanel = new JPanel(new GridBagLayout());

    private CollapsiblePane standardParameters;
    private CollapsiblePane allParameters;
    private CollapsiblePane relatedEventsPanel;
    private CollapsiblePane otherRelatedEventsPanel;

    SWEKEventInformationDialog(SWEKSwingUI _ui, RelatedEvents related, SolarEvent event) {
        super(MainFrame.get(), event.getSupplier().group().getName());
        setType(Window.Type.UTILITY); // avoids tab on macOS when Prefer tabs is always
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        ui = _ui;
        session = ui.session;

        initParameterCollapsiblePanels(event, true, false);
        List<SolarEvent> relatedEvents = related.getAssociatedEvents(event);
        if (!relatedEvents.isEmpty())
            relatedEventsPanel = createRelatedEventsCollapsiblePane(related, relatedEvents);
        setCollapsiblePanels();

        setLayout(new GridBagLayout());

        GridBagConstraints eventDescriptionConstraint = new GridBagConstraints();
        eventDescriptionConstraint.gridx = 0;
        eventDescriptionConstraint.gridy = 0;
        eventDescriptionConstraint.weightx = 1;
        eventDescriptionConstraint.weighty = 0;
        eventDescriptionConstraint.anchor = GridBagConstraints.LINE_START;
        eventDescriptionConstraint.fill = GridBagConstraints.BOTH;

        add(new EventDescriptionPanel(session, related, event), eventDescriptionConstraint);

        GridBagConstraints allTablePanelConstraint = new GridBagConstraints();
        allTablePanelConstraint.gridx = 0;
        allTablePanelConstraint.gridy = 1;
        allTablePanelConstraint.gridwidth = 1;
        allTablePanelConstraint.weightx = 1;
        allTablePanelConstraint.weighty = 1;
        allTablePanelConstraint.fill = GridBagConstraints.BOTH;

        add(allTablePanel, allTablePanelConstraint);

        int eventId = event.getUniqueID();
        Task.submitBackground("event-info", () -> session.getEventDetails(eventId), this::onSuccessDatabase, this::onFailureDatabase);
    }

    private void onSuccessDatabase(@Nonnull SWEKSession.EventDetails details) {
        if (!isDisplayable()) // closed (disposed) before the result arrived, pack() would recreate the native peer
            return;

        List<SolarEvent> relatedEvents = details.relatedEvents();
        if (!relatedEvents.isEmpty())
            otherRelatedEventsPanel = createOtherRelatedEventsCollapsiblePane(relatedEvents);

        // the stored event carries the full parameter set; keep what the user expanded meanwhile
        initParameterCollapsiblePanels(details.event(), standardParameters.isExpanded(), allParameters.isExpanded());
        repack();
        repaint();
    }

    private void onFailureDatabase(String ignoredLogContext, Throwable t) {
        if (isDisplayable() && !AppThread.isInterrupted(t)) Log.error(t);
    }

    private void initParameterCollapsiblePanels(SolarEvent event, boolean standardExpanded, boolean allExpanded) {
        ParameterTablePanel standardParameterPanel = new ParameterTablePanel(event.getVisibleEventParameters());
        standardParameters = createCollapsiblePane("Standard Parameters", standardParameterPanel, standardExpanded);

        ParameterTablePanel allEventsPanel = new ParameterTablePanel(event.getAllEventParameters());
        allParameters = createCollapsiblePane("All Parameters", allEventsPanel, allExpanded);
    }

    private CollapsiblePane createCollapsiblePane(String title, JComponent managed, boolean startExpanded) {
        return new CollapsiblePane(title, managed, startExpanded) {
            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                repack();
            }
        };
    }

    private void setCollapsiblePanels() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.PAGE_START;

        for (CollapsiblePane pane : new CollapsiblePane[]{standardParameters, allParameters, relatedEventsPanel, otherRelatedEventsPanel}) {
            if (pane == null)
                continue;
            gc.weighty = pane.isExpanded() ? 1 : 0;
            allTablePanel.add(pane, gc);
            gc.gridy++;
        }
    }

    private CollapsiblePane createRelatedEventsCollapsiblePane(RelatedEvents related, List<SolarEvent> relations) {
        JPanel eventPanels = new JPanel();
        eventPanels.setLayout(new BoxLayout(eventPanels, BoxLayout.PAGE_AXIS));
        relations.forEach(ev -> eventPanels.add(createEventPanel(related, ev)));
        return createCollapsiblePane("Related Events", new JScrollPane(eventPanels), false);
    }

    private CollapsiblePane createOtherRelatedEventsCollapsiblePane(List<SolarEvent> events) {
        JPanel eventPanels = new JPanel();
        eventPanels.setLayout(new BoxLayout(eventPanels, BoxLayout.PAGE_AXIS));
        Colors.Data colors = new Colors.Data();
        for (SolarEvent relatedEvent : events) {
            RelatedEvents relatedEvents = session.getRelatedEvents(relatedEvent.getUniqueID());
            if (relatedEvents == null)
                relatedEvents = new RelatedEvents(relatedEvent, colors.getNextColor());
            eventPanels.add(createEventPanel(relatedEvents, relatedEvent));
        }
        return createCollapsiblePane("Other Related Events", new JScrollPane(eventPanels), false);
    }

    private JPanel createEventPanel(RelatedEvents related, SolarEvent event) {
        JButton detailsButton = new JButton("Details");
        detailsButton.addActionListener(e -> ui.open(related, event));

        JPanel eventAndButtonPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        eventAndButtonPanel.add(new EventDescriptionPanel(session, related, event), c);

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
