package org.helioviewer.swhv.gui.layerpanel.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerController;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerModel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerPanel;
import org.helioviewer.swhv.gui.layerpanel.image.SWHVImageLayerModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SWHVLayerModelTest {
    private SWHVDateRangeLayerModel layer0;
    private SWHVImageLayerModel layer1;
    private SWHVImageLayerModel layer2;
    private SWHVImageLayerModel layer3;
    private SWHVDateRangeLayerModel layer4;
    private SWHVImageLayerModel layer5;
    private SWHVImageLayerModel layer6;
    private SWHVImageLayerModel layer7;

    @Before
    public void setup() {
        GlobalStateContainer.getSingletonInstance().getLayerContainerModel().setLayers(new SWHVLayerModel[0]);
        layer0 = new SWHVDateRangeLayerModel();
        SWHVDateRangeLayerPanel mdateRangepanel = new SWHVDateRangeLayerPanel();
        layer0.addListener(mdateRangepanel);
        SWHVDateRangeLayerController mcontroller = new SWHVDateRangeLayerController(layer0, mdateRangepanel);
        layer0.setRoot();
        layer1 = new SWHVImageLayerModel();
        layer2 = new SWHVImageLayerModel();
        layer3 = new SWHVImageLayerModel();
        layer0.addChild(layer1);
        layer0.addChild(layer2);
        layer0.addChild(layer3);

        layer4 = new SWHVDateRangeLayerModel();
        SWHVDateRangeLayerPanel mdateRangepanel4 = new SWHVDateRangeLayerPanel();
        layer4.addListener(mdateRangepanel);
        SWHVDateRangeLayerController mcontroller4 = new SWHVDateRangeLayerController(layer4, mdateRangepanel4);
        layer4.setRoot();
        layer5 = new SWHVImageLayerModel();
        layer6 = new SWHVImageLayerModel();
        layer7 = new SWHVImageLayerModel();
        layer4.addChild(layer5);
        layer4.addChild(layer6);
        layer4.addChild(layer7);
    }

    @After
    public void tearDown() throws Exception {
        layer0 = null;
        layer1 = null;
        layer2 = null;
        layer3 = null;
        layer4 = null;
        layer5 = null;
        layer6 = null;
        layer7 = null;
        GlobalStateContainer.getSingletonInstance().getLayerContainerModel().setLayers(new SWHVLayerModel[0]);
    }

    @Test
    public void testAddLayer() {
        SWHVLayerModel[] layers = GlobalStateContainer.getSingletonInstance().getLayerContainerModel().getLayers();
        assertTrue(layers.length == 8);
        assertTrue(layer0.getPosition() == 0);
        assertTrue(layer1.getPosition() == 1);
        assertTrue(layer2.getPosition() == 2);
        assertTrue(layer3.getPosition() == 3);
        assertTrue(layer1.getParent() == layer0);
        assertTrue(layer2.getParent() == layer0);
        assertTrue(layer3.getParent() == layer0);

        assertTrue(layer4.getPosition() == 4);
        assertTrue(layer7.getPosition() == 7);
        assertTrue(layer5.getParent() == layer4);
        assertTrue(layer6.getParent() == layer4);
        assertTrue(layer7.getParent() == layer4);
        assertTrue(layer4.getParent() == null);
        assertTrue(layers[0] == layer0);
        assertTrue(layers[1] == layer1);
        assertTrue(layers[2] == layer2);
        assertTrue(layers[3] == layer3);
        assertTrue(layers[4] == layer4);
        assertTrue(layers[5] == layer5);
        assertTrue(layers[6] == layer6);
        assertTrue(layers[7] == layer7);

    }

    @Test
    public void testRemoveLayer7() {
        layer4.removeChild(layer7);
        assertFalse(layer4.isChild(layer7));
        SWHVLayerModel[] layers = GlobalStateContainer.getSingletonInstance().getLayerContainerModel().getLayers();

        assertTrue(layers.length == 7);
        assertTrue(layer0.getPosition() == 0);
        assertTrue(layer1.getPosition() == 1);
        assertTrue(layer2.getPosition() == 2);
        assertTrue(layer3.getPosition() == 3);
        assertTrue(layer1.getParent() == layer0);
        assertTrue(layer2.getParent() == layer0);
        assertTrue(layer3.getParent() == layer0);

        assertTrue(layer4.getPosition() == 4);
        assertTrue(layer7.getPosition() == 7);
        assertTrue(layer5.getParent() == layer4);
        assertTrue(layer6.getParent() == layer4);
        assertTrue(layer4.getParent() == null);
        assertTrue(layers[0] == layer0);
        assertTrue(layers[1] == layer1);
        assertTrue(layers[2] == layer2);
        assertTrue(layers[3] == layer3);
        assertTrue(layers[4] == layer4);
        assertTrue(layers[5] == layer5);
        assertTrue(layers[6] == layer6);
        assertTrue(layer4.getChildren().length == 2);
    }

    @Test
    public void testRemoveLayer6() {
        layer4.removeChild(layer6);
        assertFalse(layer4.isChild(layer6));
        SWHVLayerModel[] layers = GlobalStateContainer.getSingletonInstance().getLayerContainerModel().getLayers();
        assertTrue(layers.length == 7);
        assertTrue(layer0.getPosition() == 0);
        assertTrue(layer1.getPosition() == 1);
        assertTrue(layer2.getPosition() == 2);
        assertTrue(layer3.getPosition() == 3);
        assertTrue(layer1.getParent() == layer0);
        assertTrue(layer2.getParent() == layer0);
        assertTrue(layer3.getParent() == layer0);
        assertTrue(layer4.getPosition() == 4);
        assertTrue(layer7.getPosition() == 6);
        assertTrue(layer5.getParent() == layer4);
        assertTrue(layer6.getParent() == layer4);
        assertTrue(layer4.getParent() == null);

        assertTrue(layers[0] == layer0);
        assertTrue(layers[1] == layer1);
        assertTrue(layers[2] == layer2);
        assertTrue(layers[3] == layer3);
        assertTrue(layers[4] == layer4);
        assertTrue(layers[5] == layer5);
        assertTrue(layers[6] == layer7);
        assertTrue(layer4.getChildren().length == 2);
    }

    @Test
    public void testRemoveAllChildren() {
        layer4.removeAllChildren();
        SWHVLayerModel[] layers = GlobalStateContainer.getSingletonInstance().getLayerContainerModel().getLayers();
        assertTrue(layers.length == 5);
        assertTrue(layer0.getPosition() == 0);
        assertTrue(layer1.getPosition() == 1);
        assertTrue(layer2.getPosition() == 2);
        assertTrue(layer3.getPosition() == 3);
        assertTrue(layer1.getParent() == layer0);
        assertTrue(layer2.getParent() == layer0);
        assertTrue(layer3.getParent() == layer0);
    }

    @Test
    public void testRemoveLayer4() {
        layer4.remove();
        SWHVLayerModel[] layers = GlobalStateContainer.getSingletonInstance().getLayerContainerModel().getLayers();
        assertTrue(layers.length == 4);
        assertTrue(layer0.getPosition() == 0);
        assertTrue(layer1.getPosition() == 1);
        assertTrue(layer2.getPosition() == 2);
        assertTrue(layer3.getPosition() == 3);
        assertTrue(layer1.getParent() == layer0);
        assertTrue(layer2.getParent() == layer0);
        assertTrue(layer3.getParent() == layer0);

        assertTrue(layers[0] == layer0);
        assertTrue(layers[1] == layer1);
        assertTrue(layers[2] == layer2);
        assertTrue(layers[3] == layer3);
    }

    @Test
    public void zla() {
        SWHVLayerModel[] zla = new SWHVLayerModel[0];
        assertTrue(zla.length == 0);
    }

}
