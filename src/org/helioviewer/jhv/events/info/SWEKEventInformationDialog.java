package org.helioviewer.jhv.events.info;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.JHVEventCache;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import com.google.common.util.concurrent.FutureCallback;

// Popup displaying informations about a HEK event.
// This panel is a JDialog, so that it can be displayed on top of an GLCanvas,
// which is not possible for other swing components.
@SuppressWarnings("serial")
public class SWEKEventInformationDialog extends JDialog implements DataCollapsiblePanelModelListener {

    private JPanel allTablePanel;

    private DataCollapsiblePanel standardParameters;
    private DataCollapsiblePanel allParameters;
    private DataCollapsiblePanel precedingEventsPanel;
    private DataCollapsiblePanel followingEventsPanel;
    private DataCollapsiblePanel otherRelatedEventsPanel;

    private final JHVEvent event;
    private final JHVRelatedEvents rEvent;

    private final DataCollapsiblePanelModel model;

    public SWEKEventInformationDialog(JHVRelatedEvents revent, JHVEvent _event) {
        super(JHVFrame.getFrame(), revent.getSupplier().getGroup().getName());
        setType(Window.Type.UTILITY); // avoids tab on macOS when Prefer tabs is always

        event = _event;
        rEvent = revent;

        model = new DataCollapsiblePanelModel();
        model.addListener(this);

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

        add(new EventDescriptionPanel(revent, event), eventDescriptionConstraint);

        GridBagConstraints allTablePanelConstraint = new GridBagConstraints();
        allTablePanelConstraint.gridx = 0;
        allTablePanelConstraint.gridy = 1;
        allTablePanelConstraint.gridwidth = 1;
        allTablePanelConstraint.weightx = 1;
        allTablePanelConstraint.weighty = 1;
        allTablePanelConstraint.fill = GridBagConstraints.BOTH;

        add(allTablePanel, allTablePanelConstraint);

        EventQueueCallbackExecutor.pool.submit(new DatabaseCallable(event), new DatabaseCallback());
    }

    private void initAllTablePanel() {
        allTablePanel = new JPanel(new GridBagLayout());
    }

    private void initParameterCollapsiblePanels() {
        ParameterTablePanel standardParameterPanel = new ParameterTablePanel(event.getVisibleEventParameters());
        standardParameters = new DataCollapsiblePanel("Standard Parameters", standardParameterPanel, true, model);

        ParameterTablePanel allEventsPanel = new ParameterTablePanel(event.getAllEventParameters());
        allParameters = new DataCollapsiblePanel("All Parameters", allEventsPanel, false, model);

        List<JHVEvent> precedingEvents = rEvent.getPreviousEvents(event);
        if (!precedingEvents.isEmpty()) {
            precedingEventsPanel = createRelatedEventsCollapsiblePane("Preceding Events", rEvent, precedingEvents);
        }

        List<JHVEvent> nextEvents = rEvent.getNextEvents(event);
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
        gc.anchor = GridBagConstraints.PAGE_START;
        gc.weighty = standardParameters.isExpanded() ? 1 : 0;
        allTablePanel.add(standardParameters, gc);

        gc.gridy = 1;
        gc.weighty = allParameters.isExpanded() ? 1 : 0;
        allTablePanel.add(allParameters, gc);

        int gridYPosition = 2;

        if (precedingEventsPanel != null) {
            gc.gridy = gridYPosition;
            gc.weighty = precedingEventsPanel.isExpanded() ? 1 : 0;
            allTablePanel.add(precedingEventsPanel, gc);
            gridYPosition++;
        }

        if (followingEventsPanel != null) {
            gc.gridy = gridYPosition;
            gc.weighty = followingEventsPanel.isExpanded() ? 1 : 0;
            allTablePanel.add(followingEventsPanel, gc);
            gridYPosition++;
        }

        if (otherRelatedEventsPanel != null) {
            gc.gridy = gridYPosition;
            gc.weighty = otherRelatedEventsPanel.isExpanded() ? 1 : 0;
            allTablePanel.add(otherRelatedEventsPanel, gc);
            //gridYPosition++;
        }
    }

    private DataCollapsiblePanel createRelatedEventsCollapsiblePane(String relation, JHVRelatedEvents rEvents, List<JHVEvent> relations) {
        JPanel allPrecedingEvents = new JPanel();
        allPrecedingEvents.setLayout(new BoxLayout(allPrecedingEvents, BoxLayout.PAGE_AXIS));
        relations.forEach(ev -> allPrecedingEvents.add(createEventPanel(rEvents, ev)));
        return new DataCollapsiblePanel(relation, new JScrollPane(allPrecedingEvents), false, model);
    }

    private DataCollapsiblePanel createOtherRelatedEventsCollapsiblePane(String relation, List<JHVRelatedEvents> rEvents) {
        JPanel allPrecedingEvents = new JPanel();
        allPrecedingEvents.setLayout(new BoxLayout(allPrecedingEvents, BoxLayout.PAGE_AXIS));
        for (JHVRelatedEvents rev : rEvents) {
            List<JHVEvent> evs = rev.getEvents();
            if (!evs.isEmpty()) {
                allPrecedingEvents.add(createEventPanel(rev, evs.get(0)));
            }
        }
        return new DataCollapsiblePanel(relation, new JScrollPane(allPrecedingEvents), false, model);
    }

    private static JPanel createEventPanel(JHVRelatedEvents rEvents, JHVEvent event) {
        JButton detailsButton = new JButton("Details");
        detailsButton.addActionListener(e -> {
            SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(rEvents, event);
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
        eventAndButtonPanel.add(new EventDescriptionPanel(rEvents, event), c);

        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.LINE_END;
        eventAndButtonPanel.add(detailsButton, c);

        return eventAndButtonPanel;
    }

    @Override
    public void repack() {
        allTablePanel.removeAll();
        setCollapsiblePanels();
        pack();
    }

    private static class DatabaseCallable implements Callable<List<JHVEvent>> {

        private final JHVEvent e;

        DatabaseCallable(JHVEvent _e) {
            e = _e;
        }

        @Override
        public List<JHVEvent> call() throws Exception {
            return EventDatabase.getOtherRelations(e.getUniqueID(), e.getSupplier(), false, true);
        }

    }

    private class DatabaseCallback implements FutureCallback<List<JHVEvent>> {

        @Override
        public void onSuccess(List<JHVEvent> result) {
            result.forEach(JHVEventCache::addEvent);

            ArrayList<JHVRelatedEvents> rEvents = new ArrayList<>();
            int id = event.getUniqueID();
            for (JHVEvent jhvEvent : result) {
                int jid = jhvEvent.getUniqueID();
                //if (jid == id)
                //    event = jhvEvent;
                //else
                if (jid != id)
                    rEvents.add(JHVEventCache.getRelatedEvents(jid));
            }

            if (!rEvents.isEmpty())
                otherRelatedEventsPanel = createOtherRelatedEventsCollapsiblePane("Other Related Events", rEvents);

            allTablePanel.removeAll();
            initParameterCollapsiblePanels();
            setCollapsiblePanels();

            repack();
            repaint();
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("SWEKEventInformationDialog", t);
        }

    }

}
