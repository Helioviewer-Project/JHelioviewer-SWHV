package org.helioviewer.swhv.gui.layerpanel.layertimeline;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerController;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerPanel;
import org.helioviewer.swhv.mvc.SWHVModel;

public class SWHVTimeLineLayerController implements SWHVLayerController {
    private final SWHVTimeLineLayerModel model;
    private final SWHVTimeLineLayerPanel timelineLayerPanel;

    public SWHVTimeLineLayerController(SWHVTimeLineLayerModel timelineLayerModel, SWHVTimeLineLayerPanel timelineLayerPanel) {
        this.model = timelineLayerModel;
        this.model.setController(this);
        this.timelineLayerPanel = timelineLayerPanel;
        this.timelineLayerPanel.setController(this);
    }

    @Override
    public SWHVLayerPanel getPanel() {
        return this.timelineLayerPanel;
    }

    @Override
    public SWHVModel getModel() {
        return this.model;
    }

    @Override
    public void setActive() {
        this.model.setActive(true, true);
    }

    @Override
    public void toggleFold() {
    }

    public static class SWHVRegisterTimeLineLayerDateActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            SWHVLayerModel activeLayer = GlobalStateContainer.getSingletonInstance().getLayerContainerModel().getActiveLayer();

            if (activeLayer != null) {
                SWHVTimeLineLayerModel timelineLayerModel = new SWHVTimeLineLayerModel();
                SWHVTimeLineLayerPanel timelineLayerPanel = new SWHVTimeLineLayerPanel();
                SWHVTimeLineLayerController timelineLayerController = new SWHVTimeLineLayerController(timelineLayerModel, timelineLayerPanel);

                activeLayer.addChild(timelineLayerModel);
            }

        }
    }

    public static ActionListener getRegisterActionListener() {
        return new SWHVRegisterTimeLineLayerDateActionListener();
    }

    public void remove() {
        this.model.remove();
    }
}
