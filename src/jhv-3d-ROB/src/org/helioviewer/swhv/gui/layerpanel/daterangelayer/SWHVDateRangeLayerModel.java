package org.helioviewer.swhv.gui.layerpanel.daterangelayer;

import java.util.Date;

import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractLayerModel;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerController;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;
import org.helioviewer.swhv.gui.layerpanel.SWHVOptionPanel;

public class SWHVDateRangeLayerModel extends SWHVAbstractLayerModel {
    private Date beginDate;
    private Date endDate;
    private SWHVDateRangeLayerController controller;

    public SWHVDateRangeLayerModel() {

    }

    public SWHVDateRangeLayerModel(SWHVDateRangeLayerController controller) {
        this.setController(controller);
    }

    @Override
    public SWHVLayerController getController() {
        return this.controller;
    }

    public SWHVDateRangeLayerController getDateRangeLayerController() {
        return this.controller;
    }

    public void setController(SWHVDateRangeLayerController controller) {
        this.controller = controller;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    @Override
    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
        fireBeginDateChanged();
    }

    @Override
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        fireEndDateChanged();
    }

    public void fireBeginDateChanged() {
        for (int i = 0; i < this.listenerPanel.length; i++) {
            SWHVDateRangeLayerModelListener listener = (SWHVDateRangeLayerModelListener) this.listenerPanel[i];
            listener.beginDateChanged(this);
        }
        for (SWHVLayerModel child : getChildren()) {
            child.setBeginDate(this.getBeginDate());
        }
    }

    public void fireEndDateChanged() {
        for (int i = 0; i < this.listenerPanel.length; i++) {
            SWHVDateRangeLayerModelListener listener = (SWHVDateRangeLayerModelListener) this.listenerPanel[i];
            listener.endDateChanged(this);
        }
        for (SWHVLayerModel child : getChildren()) {
            child.setEndDate(this.getEndDate());
        }
    }

    @Override
    public void fireLevelChanged() {
        for (int i = 0; i < this.listenerPanel.length; i++) {
            SWHVDateRangeLayerModelListener listener = (SWHVDateRangeLayerModelListener) this.listenerPanel[i];
            listener.updateLevel(this);
        }
    }

    @Override
    public void fireFoldedChanged() {
        for (int i = 0; i < this.listenerPanel.length; i++) {
            SWHVDateRangeLayerModelListener listener = (SWHVDateRangeLayerModelListener) this.listenerPanel[i];
            listener.updateFolded(this);
        }
    }

    @Override
    public SWHVOptionPanel getOptionPanel() {
        if (optionPanel == null) {
            this.optionPanel = new SWHVDateRangeLayerOptionPanel(this);
        } else {
            ((SWHVDateRangeLayerOptionPanel) optionPanel).setDateRangeLayerModel(this);
        }
        return optionPanel;
    }

    @Override
    public void fireRemoved() {

    }

}
