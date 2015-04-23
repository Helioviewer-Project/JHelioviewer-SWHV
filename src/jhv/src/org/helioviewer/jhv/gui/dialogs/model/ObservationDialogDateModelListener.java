package org.helioviewer.jhv.gui.dialogs.model;

import java.util.Date;

public interface ObservationDialogDateModelListener {

    public abstract void startDateChanged(Date startDate);
    public abstract void endDateChanged(Date endDate);

}
