package org.helioviewer.swhv.gui.layerpanel.layertype;

import java.util.Date;

import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractLayerModel;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerController;
import org.helioviewer.swhv.gui.layerpanel.SWHVOptionPanel;

public class SWHVTypeLayerModel extends SWHVAbstractLayerModel {

    private SWHVTypeLayerController controller;

    @Override
    public void fireLevelChanged() {

    }

    @Override
    public void fireFoldedChanged() {

    }

    @Override
    public SWHVOptionPanel getOptionPanel() {
        return new SWHVTypeLayerOptionPanel();
    }

    public void setController(SWHVTypeLayerController controller) {
        this.controller = controller;
    }

    @Override
    public SWHVLayerController getController() {
        return controller;
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
