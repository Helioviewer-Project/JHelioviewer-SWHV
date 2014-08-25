package org.helioviewer.swhv.gui.layerpanel.test;

import static org.junit.Assert.assertTrue;

import javax.swing.ImageIcon;

import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;
import org.helioviewer.swhv.gui.layerpanel.image.SWHVImageLayerController;
import org.helioviewer.swhv.gui.layerpanel.type.SWHVChooseTypeContainerModel;
import org.helioviewer.swhv.gui.layerpanel.type.SWHVChooseTypeController;
import org.helioviewer.swhv.gui.layerpanel.type.SWHVChooseTypeModel;
import org.helioviewer.swhv.gui.layerpanel.type.SWHVChooseTypePanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChooseTypeTest {
    SWHVChooseTypeModel[] chooseTypeModel;
    ImageIcon typeIcon;
    SWHVChooseTypeContainerModel container = GlobalStateContainer.getSingletonInstance().getChooseTypeContainerModel();
    private int registered;

    @Before
    public void setUp() throws Exception {
        GlobalStateContainer.getSingletonInstance().getLayerContainerModel().setLayers(new SWHVLayerModel[0]);
        int registeredBefore = container.getRegisteredModels().length;
        int toRegister = 8;
        registered = registeredBefore + toRegister;
        chooseTypeModel = new SWHVChooseTypeModel[toRegister];
        typeIcon = IconBank.getIcon(JHVIcon.FORWARD);

        for (int i = 0; i < chooseTypeModel.length; i++) {
            chooseTypeModel[i] = new SWHVChooseTypeModel("Image", SWHVImageLayerController.getRegisterActionListener(), typeIcon);
            SWHVChooseTypePanel chooseTypePanelImage = new SWHVChooseTypePanel(chooseTypeModel[i]);
            SWHVChooseTypeController chooseTypeControllerImage = new SWHVChooseTypeController(chooseTypeModel[i], chooseTypePanelImage);

            container.registerType(chooseTypeModel[i]);
        }
    }

    @After
    public void tearDown() throws Exception {
        chooseTypeModel = null;
        GlobalStateContainer.getSingletonInstance().getLayerContainerModel().setLayers(new SWHVLayerModel[0]);
    }

    @Test
    public void test() {
        assertTrue(container.getRegisteredModels().length == registered);
    }

}
