package org.helioviewer.swhv.gui.layerpanel.daterangelayer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractLayerController;

public class SWHVDateRangeLayerController extends SWHVAbstractLayerController {
    private SWHVDateRangeLayerModel model;
    private SWHVDateRangeLayerPanel panel;
    private SWHVDateRangeLayerSetDateActionListener dateRangeLayerSetDateActionListener;

    public SWHVDateRangeLayerController(SWHVDateRangeLayerModel model, SWHVDateRangeLayerPanel panel) {
        this.setModel(model);
        this.setView(panel);
        panel.setController(this);
        model.setController(this);
        dateRangeLayerSetDateActionListener = new SWHVDateRangeLayerSetDateActionListener();
    }

    @Override
    public SWHVDateRangeLayerModel getModel() {
        return model;
    }

    public void setModel(SWHVDateRangeLayerModel model) {
        this.model = model;
    }

    @Override
    public SWHVDateRangeLayerPanel getPanel() {
        return panel;
    }

    public void setView(SWHVDateRangeLayerPanel panel) {
        this.panel = panel;
    }

    public void remove() {
        this.model.remove();
    }

    @Override
    public void setActive() {
        this.model.setActive(true, true);

    }

    @Override
    public void toggleFold() {
        this.model.toggleFold();
    }

    public class SWHVDateRangeLayerSetDateActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            SWHVDateRangeLayerOptionPanel optionPanel = (SWHVDateRangeLayerOptionPanel) (model.getOptionPanel());
            if (optionPanel.getBeginDate() != model.getBeginDate()) {
                model.setBeginDate(optionPanel.getBeginDate());
                model.fireBeginDateChanged();
            }
            if (optionPanel.getEndDate() != model.getEndDate()) {
                model.setEndDate(optionPanel.getEndDate());
                model.fireEndDateChanged();
            }
        }
    }

    public SWHVDateRangeLayerSetDateActionListener getDateRangeLayerSetDateActionListener() {
        return dateRangeLayerSetDateActionListener;
    }

    public void setDateRangeLayerSetDateActionListener(SWHVDateRangeLayerSetDateActionListener dateRangeLayerSetDateActionListener) {
        this.dateRangeLayerSetDateActionListener = dateRangeLayerSetDateActionListener;
    }
}
