package org.helioviewer.swhv.gui.layerpanel.layertimeline;

import java.util.Date;

import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractLayerModel;
import org.helioviewer.swhv.gui.layerpanel.SWHVOptionPanel;
import org.helioviewer.swhv.gui.layerpanel.SWHVRegistrableLayerModel;

public class SWHVTimeLineLayerModel extends SWHVAbstractLayerModel implements SWHVRegistrableLayerModel {
    private SWHVTimeLineLayerController controller;

    @Override
    public void fireLevelChanged() {
    }

    @Override
    public void fireFoldedChanged() {
    }

    @Override
    public SWHVOptionPanel getOptionPanel() {
        return new SWHVTimeLineLayerOptionPanel(this);
    }

    @Override
    public SWHVTimeLineLayerController getController() {
        return this.controller;
    }

    public void setController(SWHVTimeLineLayerController controller) {
        this.controller = controller;
    }

    @Override
    public void fireRemoved() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setBeginDate(Date beginDate) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setEndDate(Date endDate) {
        // TODO Auto-generated method stub

    }
}
