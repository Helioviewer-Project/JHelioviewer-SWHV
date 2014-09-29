package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.dialogs.observation.ImageDataPanel;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class CollapsiblePaneWithButton extends CollapsiblePane {

    private static final long serialVersionUID = -9107038141279736802L;
    private ImageDataPanel observationImagePane;
    /**
     * Observation dialog to actually add new data
     */
    private final ObservationDialog observationDialog = ObservationDialog.getSingletonInstance();

    /**
     * Action to add a new layer. If there is a current active layer which much
     * different time, the dates will be updated.
     */
    private Action addLayerAction;

    /**
     * Button to add new layers
     */
    private JButton addLayerButton;

    public CollapsiblePaneWithButton(String title, Component component, boolean startExpanded) {
        super(title, component, startExpanded);
        // observationImagePane =
        // ImageViewerGui.getSingletonInstance().getImageSelectorPanel().getObservationImagePane();
        initActions();
    }

    @Override
    public void setButtons() {
        topButtonsPanel = new JPanel();
        topButtonsPanel.setLayout(new BorderLayout());
        topButtonsPanel.add(toggleButton, BorderLayout.WEST);
        add(topButtonsPanel, BorderLayout.PAGE_START);
        // addLayerButton=new JButton(addLayerAction);
        // addLayerButton.setHorizontalAlignment(SwingConstants.LEFT);
        // topButtonsPanel.add(addLayerButton, BorderLayout.EAST);
    }

    private void initActions() {
        addLayerAction = new AbstractAction("Add Layer", IconBank.getIcon(JHVIcon.ADD)) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;
            {
                putValue(SHORT_DESCRIPTION, "Add a new Layer");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (observationImagePane == null) {
                    observationImagePane = new ImageDataPanel();
                }
                // Check the dates if possible
                final View activeView = LayersModel.getSingletonInstance().getActiveView();

                /*
                 * TODO: Code Simplification - Cleanup Date selection when
                 * clicking on "add images", e.g. use
                 * LayersModel.getLatestDate(...), ...
                 *
                 * Here are some more comments by Helge:
                 *
                 * If it is a local file, the timestamps are read from the
                 * parsed JPX movie, i.e. a call will pause until the whole
                 * movie has finished loading.
                 *
                 * If it has been reading through the API the frame time stamps
                 * already have been returned and it is not bad. For the time
                 * being it will only update if its already loaded.
                 *
                 * I think there should be a better solution? Maybe a wait
                 * dialog? etc.?
                 */

                if (activeView != null) {
                    MovieView tmv = activeView.getAdapter(TimedMovieView.class);
                    if (tmv != null && tmv.getMaximumAccessibleFrameNumber() == tmv.getMaximumFrameNumber()) {
                        final ImmutableDateTime start = LayersModel.getSingletonInstance().getStartDate(activeView);
                        final ImmutableDateTime end = LayersModel.getSingletonInstance().getEndDate(activeView);
                        if (start != null && end != null) {
                            try {
                                Date startDate = start.getTime();
                                Date endDate = end.getTime();
                                Date obsStartDate = ImageDataPanel.apiDateFormat.parse(observationImagePane.getStartTime());
                                Date obsEndDate = ImageDataPanel.apiDateFormat.parse(observationImagePane.getEndTime());
                                // only updates if its really necessary with a
                                // tolerance of an hour
                                final int tolerance = 60 * 60 * 1000;
                                if (Math.abs(startDate.getTime() - obsStartDate.getTime()) > tolerance || Math.abs(endDate.getTime() - obsEndDate.getTime()) > tolerance) {
                                    observationImagePane.setStartDate(startDate);
                                    observationImagePane.setEndDate(endDate);
                                }
                            } catch (ParseException e) {
                                // Should not happen
                                Log.error("Cannot update observation dialog", e);
                            }
                        }
                    }
                }
                // Show dialog
                observationDialog.showDialog();
            }
        };
    }
}
