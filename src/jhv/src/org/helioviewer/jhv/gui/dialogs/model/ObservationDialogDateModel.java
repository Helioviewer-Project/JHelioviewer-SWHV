package org.helioviewer.jhv.gui.dialogs.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ObservationDialogDateModel {
    private final List<ObservationDialogDateModelListener> listeners;

    private static ObservationDialogDateModel instance;

    private Date startDate;
    private Date endDate;

    private boolean endDateSetByUser;

    private boolean startDateSetByUser;

    private ObservationDialogDateModel() {
        listeners = new ArrayList<ObservationDialogDateModelListener>();
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

    public void setEndDate(Date endDate, boolean byUser) {
        if (!endDateSetByUser) {
            endDateSetByUser = byUser;
        }
        if (!endDateSetByUser || byUser) {
            this.endDate = endDate;
            fireEndDateChanged();
        }
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate, boolean byUser) {
        if (!startDateSetByUser) {
            startDateSetByUser = byUser;
        }
        if (!startDateSetByUser || byUser) {
            this.startDate = startDate;
            fireStartDateChanged();
        }
    }

    public Date getEndDate() {
        return endDate;
    }

    public boolean isEndDateSetByUser() {
        return endDateSetByUser;
    }

    public boolean isStartDateSetByUser() {
        return startDateSetByUser;
    }

    private void fireStartDateChanged() {
        for (ObservationDialogDateModelListener l : listeners) {
            l.startDateChanged(startDate);
        }
    }

    private void fireEndDateChanged() {
        for (ObservationDialogDateModelListener l : listeners) {
            l.endDateChanged(endDate);
        }
    }
}
