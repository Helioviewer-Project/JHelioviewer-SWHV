package org.helioviewer.globalstate;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.swhv.gui.layerpanel.image.SWHVImageLayerController;
import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerModel;
import org.helioviewer.swhv.gui.layerpanel.layertimeline.SWHVTimeLineLayerController;
import org.helioviewer.swhv.gui.layerpanel.type.SWHVChooseTypeContainerController;
import org.helioviewer.swhv.gui.layerpanel.type.SWHVChooseTypeContainerModel;
import org.helioviewer.swhv.gui.layerpanel.type.SWHVChooseTypeContainerPanel;
import org.helioviewer.swhv.gui.layerpanel.type.SWHVChooseTypeController;
import org.helioviewer.swhv.gui.layerpanel.type.SWHVChooseTypeModel;
import org.helioviewer.swhv.gui.layerpanel.type.SWHVChooseTypePanel;

public class GlobalStateContainer {

    private final SWHVLayerContainerModel layerContainerModel;
    private final SWHVChooseTypeContainerModel chooseTypeContainerModel;
    private final SWHVChooseTypeContainerController chooseTypeContainerController;
    private final SWHVChooseTypeContainerPanel chooseTypeContainerPanel;

    private static GlobalStateContainer singletonInstance = new GlobalStateContainer();

    public static GlobalStateContainer getSingletonInstance() {
        return singletonInstance;
    }

    public GlobalStateContainer() {
        this.layerContainerModel = SWHVLayerContainerModel.getSingletonInstance();
        this.chooseTypeContainerModel = SWHVChooseTypeContainerModel.getSingletonInstance();
        this.chooseTypeContainerPanel = SWHVChooseTypeContainerPanel.getSingletonInstance();
        this.chooseTypeContainerController = new SWHVChooseTypeContainerController(chooseTypeContainerModel, chooseTypeContainerPanel);
        this.chooseTypeContainerModel.addListener(this.chooseTypeContainerPanel);
        registerStandardTypes();
    }

    private void registerStandardTypes() {
        ImageIcon typeIconImage = IconBank.getIcon(JHVIcon.FORWARD);
        SWHVChooseTypeModel chooseTypeModelImage = new SWHVChooseTypeModel("Image", SWHVImageLayerController.getRegisterActionListener(), typeIconImage);
        SWHVChooseTypePanel chooseTypePanelImage = new SWHVChooseTypePanel(chooseTypeModelImage);
        SWHVChooseTypeController chooseTypeControllerImage = new SWHVChooseTypeController(chooseTypeModelImage, chooseTypePanelImage);

        this.chooseTypeContainerModel.registerType(chooseTypeModelImage);

        ImageIcon typeIconTimeline = IconBank.getIcon(JHVIcon.FORWARD);
        SWHVChooseTypeModel chooseTypeModelTimeline = new SWHVChooseTypeModel("TimeLine", SWHVTimeLineLayerController.getRegisterActionListener(), typeIconTimeline);
        SWHVChooseTypePanel chooseTypePanelTimeline = new SWHVChooseTypePanel(chooseTypeModelTimeline);
        SWHVChooseTypeController chooseTypeControllerTimeline = new SWHVChooseTypeController(chooseTypeModelTimeline, chooseTypePanelTimeline);

        this.chooseTypeContainerModel.registerType(chooseTypeModelTimeline);
    }

    public SWHVLayerContainerModel getLayerContainerModel() {
        return layerContainerModel;
    }

    public SWHVChooseTypeContainerModel getChooseTypeContainerModel() {
        return chooseTypeContainerModel;
    }

    public SWHVChooseTypeContainerPanel getChooseTypeContainerPanel() {
        return chooseTypeContainerPanel;
    }

}
