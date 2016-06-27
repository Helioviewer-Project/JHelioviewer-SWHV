package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModelListener;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialogPanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public abstract class SimpleObservationDialogUIPanel extends ObservationDialogPanel implements JHVCalendarListener, LayersListener, ObservationDialogDateModelListener {

    private final JHVCalendarDatePicker calendarStartDate;

    public SimpleObservationDialogUIPanel() {
        ObservationDialogDateModel.getInstance().addListener(this);

        JLabel labelStartDate = new JLabel("Start date");
        calendarStartDate = new JHVCalendarDatePicker();

        JPanel timePane = new JPanel();
        JPanel plotPane = new JPanel();

        // set up time settings
        calendarStartDate.addJHVCalendarListener(this);
        calendarStartDate.setToolTipText("UTC date for observation start");

        JPanel startDatePane = new JPanel(new BorderLayout());
        startDatePane.add(labelStartDate, BorderLayout.PAGE_START);
        startDatePane.add(calendarStartDate, BorderLayout.CENTER);

        timePane.setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        timePane.add(startDatePane);

        // set basic layout
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(timePane);
        add(plotPane);

        Layers.addLayersListener(this);
    }

    public void setTime(long start) {
        calendarStartDate.setTime(start);
    }

    public long getTime() {
        return calendarStartDate.getTime();
    }

    // JHV Calendar Listener

    @Override
    public void actionPerformed(JHVCalendarEvent e) {
        if (e.getSource() == calendarStartDate) {
            ObservationDialogDateModel.getInstance().setStartTime(calendarStartDate.getTime(), true);
        }
    }

    @Override
    public void layerAdded(View view) {
        long start = view.getFirstTime().milli;
        calendarStartDate.setTime(start);
        ObservationDialogDateModel.getInstance().setStartTime(start, false);
    }

    @Override
    public void activeLayerChanged(View view) {
    }

    @Override
    public void startTimeChanged(long startTime) {
        calendarStartDate.setTime(startTime);
    }

    @Override
    public void endTimeChanged(long endTime) {
    }

}
