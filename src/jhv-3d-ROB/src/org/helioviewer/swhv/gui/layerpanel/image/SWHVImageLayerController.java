package org.helioviewer.swhv.gui.layerpanel.image;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractRegistrableLayerPanel;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;
import org.helioviewer.swhv.gui.layerpanel.SWHVRegistrableLayerController;

public class SWHVImageLayerController implements SWHVRegistrableLayerController {

    private SWHVImageLayerModel model;
    private final SWHVImageLayerPanel panel;

    public SWHVImageLayerController(SWHVImageLayerModel model, SWHVImageLayerPanel panel) {
        this.model = model;
        this.panel = panel;
        this.model.setController(this);
        this.panel.setController(this);
    }

    public void setModel(SWHVImageLayerModel model) {
        this.model = model;
    }

    @Override
    public SWHVImageLayerModel getModel() {
        return model;
    }

    public static class SWHVRegisterImageLayerDateActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            SWHVLayerModel activeLayer = GlobalStateContainer.getSingletonInstance().getLayerContainerModel().getActiveLayer();

            if (activeLayer != null) {
                SWHVImageLayerModel imageLayerModel = new SWHVImageLayerModel();
                SWHVImageLayerPanel imageLayerPanel = new SWHVImageLayerPanel();
                imageLayerModel.addListener(imageLayerPanel);
                SWHVImageLayerController imageLayerController = new SWHVImageLayerController(imageLayerModel, imageLayerPanel);
                activeLayer.addChild(imageLayerModel);
            }

        }
    }

    public static ActionListener getRegisterActionListener() {
        return new SWHVRegisterImageLayerDateActionListener();
    }

    @Override
    public SWHVAbstractRegistrableLayerPanel getPanel() {
        return this.panel;
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

}
