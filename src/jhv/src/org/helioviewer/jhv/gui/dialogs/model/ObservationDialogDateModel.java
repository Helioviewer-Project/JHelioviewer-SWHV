package org.helioviewer.jhv.gui.dialogs.model;

import java.util.HashSet;

public class ObservationDialogDateModel {

    private static final HashSet<ObservationDialogDateModelListener> listeners = new HashSet<>();

    private static long startTime;
    private static long endTime;

    private static boolean startByUser;
    private static boolean endByUser;

    public static void addListener(ObservationDialogDateModelListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(ObservationDialogDateModelListener listener) {
        listeners.remove(listener);
    }

    private static void fireStartTimeChanged() {
        for (ObservationDialogDateModelListener l : listeners) {
            l.startTimeChanged(startTime);
        }
    }

    private static void fireEndTimeChanged() {
        for (ObservationDialogDateModelListener l : listeners) {
            l.endTimeChanged(endTime);
        }
    }

    public static void setStartTime(long _startTime, boolean byUser) {
        if (!startByUser) {
            startByUser = byUser;
        }
        if (!startByUser || byUser) {
            startTime = _startTime;
            fireStartTimeChanged();
        }
    }

    public static void setEndTime(long _endTime, boolean byUser) {
        if (!endByUser) {
            endByUser = byUser;
        }
        if (!endByUser || byUser) {
            endTime = _endTime;
            fireEndTimeChanged();
        }
    }

    public static boolean isEndTimeSetByUser() {
        return endByUser;
    }

    public static boolean isStartTimeSetByUser() {
        return startByUser;
    }

}
