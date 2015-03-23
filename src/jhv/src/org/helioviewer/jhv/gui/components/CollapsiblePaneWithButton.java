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
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.observation.ImageDataPanel;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class CollapsiblePaneWithButton extends CollapsiblePane {

    private static final long serialVersionUID = -9107038141279736802L;
    private ImageDataPanel observationImagePane;

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
        initActions();
    }

    @Override
    public void setButtons() {
        topButtonsPanel = new JPanel();
        topButtonsPanel.setLayout(new BorderLayout());
        topButtonsPanel.add(toggleButton, BorderLayout.WEST);
        add(topButtonsPanel, BorderLayout.PAGE_START);
    }

    private void initActions() {
        addLayerAction = new AbstractAction("Add layer", IconBank.getIcon(JHVIcon.ADD)) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;
            {
                putValue(SHORT_DESCRIPTION, "Add a new layer");
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
                final JHVJP2View activeView = LayersModel.getSingletonInstance().getActiveView();
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
                                    observationImagePane.setStartDate(startDate, false);
                                    observationImagePane.setEndDate(endDate, false);
                                }
                            } catch (ParseException e) {
                                // Should not happen
                                Log.error("Cannot update observation dialog", e);
                            }
                        }
                    }
                }
                // Show dialog
                ImageViewerGui.getSingletonInstance().getObservationDialog().showDialog();
            }
        };
    }

}
