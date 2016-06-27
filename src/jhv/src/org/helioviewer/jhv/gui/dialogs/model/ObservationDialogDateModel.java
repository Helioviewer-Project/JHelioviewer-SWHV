package org.helioviewer.jhv.gui.dialogs.model;

import java.util.HashSet;

public class ObservationDialogDateModel {

    private final HashSet<ObservationDialogDateModelListener> listeners;

    private static ObservationDialogDateModel instance;

    private long startTime;
    private long endTime;

    private boolean startByUser;
    private boolean endByUser;

    private ObservationDialogDateModel() {
        listeners = new HashSet<ObservationDialogDateModelListener>();
    }

    public static ObservationDialogDateModel getInstance() {
        if (instance == null) {
            instance = new ObservationDialogDateModel();
        }
        return instance;
    }

    public void addListener(ObservationDialogDateModelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ObservationDialogDateModelListener listener) {
        listeners.remove(listener);
    }

    private void fireStartTimeChanged() {
        for (ObservationDialogDateModelListener l : listeners) {
            l.startTimeChanged(startTime);
        }
    }

    private void fireEndTimeChanged() {
        for (ObservationDialogDateModelListener l : listeners) {
            l.endTimeChanged(endTime);
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime, boolean byUser) {
        if (!startByUser) {
            startByUser = byUser;
        }
        if (!startByUser || byUser) {
            this.startTime = startTime;
            fireStartTimeChanged();
        }
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime, boolean byUser) {
        if (!endByUser) {
            endByUser = byUser;
        }
        if (!endByUser || byUser) {
            this.endTime = endTime;
            fireEndTimeChanged();
        }
    }

    public boolean isEndTimeSetByUser() {
        return endByUser;
    }

    public boolean isStartTimeSetByUser() {
        return startByUser;
    }

}
