package org.helioviewer.swhv.gui.layerpanel.test;

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

public class SWHVLayerInteractionTest {
    private SWHVDateRangeLayerModel layer0;
    private SWHVImageLayerModel layer1;
    private SWHVImageLayerModel layer2;
    private SWHVImageLayerModel layer3;
    private SWHVDateRangeLayerModel layer4;
    private SWHVImageLayerModel layer5;
    private SWHVImageLayerModel layer6;
    private SWHVImageLayerModel layer7;
    private SWHVDateRangeLayerModel tlayer0;
    private SWHVDateRangeLayerModel tlayer1;
    private SWHVImageLayerModel tlayer2;
    private SWHVImageLayerModel tlayer3;

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
        GlobalStateContainer.getSingletonInstance().getLayerContainerModel().setLayers(new SWHVLayerModel[0]);
        layer4 = new SWHVDateRangeLayerModel();
        SWHVDateRangeLayerPanel mdateRangepanel4 = new SWHVDateRangeLayerPanel();
        layer4.addListener(mdateRangepanel4);
        SWHVDateRangeLayerController mcontroller4 = new SWHVDateRangeLayerController(layer4, mdateRangepanel4);
        layer4.setRoot();
        layer5 = new SWHVImageLayerModel();
        layer6 = new SWHVImageLayerModel();
        layer7 = new SWHVImageLayerModel();
        layer4.addChild(layer5);
        layer4.addChild(layer6);
        layer4.addChild(layer7);

        tlayer0 = new SWHVDateRangeLayerModel();
        SWHVDateRangeLayerPanel tmdateRangepanel0 = new SWHVDateRangeLayerPanel();
        tlayer0.addListener(tmdateRangepanel0);
        SWHVDateRangeLayerController tmcontroller0 = new SWHVDateRangeLayerController(tlayer0, tmdateRangepanel0);
        tlayer0.setRoot();
        tlayer1 = new SWHVDateRangeLayerModel();
        SWHVDateRangeLayerPanel tmdateRangepanel1 = new SWHVDateRangeLayerPanel();
        tlayer1.addListener(tmdateRangepanel1);
        SWHVDateRangeLayerController tmcontroller1 = new SWHVDateRangeLayerController(tlayer1, tmdateRangepanel1);
        tlayer0.addChild(tlayer1);
        tlayer2 = new SWHVImageLayerModel();
        tlayer1.addChild(tlayer2);
        tlayer3 = new SWHVImageLayerModel();
        tlayer2.addChild(tlayer3);
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
    public void testVisibleStartup() {
        assertTrue(layer0.isVisible());
        assertTrue(layer1.isVisible());
        assertTrue(layer2.isVisible());
        assertTrue(layer3.isVisible());
        assertTrue(layer4.isVisible());
        assertTrue(layer5.isVisible());
        assertTrue(layer6.isVisible());
        assertTrue(layer7.isVisible());
    }

    @Test
    public void testFoldStartup() {
        assertTrue(!layer0.isFolded());
        assertTrue(!layer1.isFolded());
        assertTrue(!layer2.isFolded());
        assertTrue(!layer3.isFolded());
        assertTrue(!layer4.isFolded());
        assertTrue(!layer5.isFolded());
        assertTrue(!layer6.isFolded());
        assertTrue(!layer7.isFolded());
    }

    @Test
    public void testFold() {
        layer0.fold();
        assertTrue(layer0.isVisible());
        assertTrue(!layer1.isVisible());
        assertTrue(!layer2.isVisible());
        assertTrue(!layer3.isVisible());
        assertTrue(layer4.isVisible());
        assertTrue(layer5.isVisible());
        assertTrue(layer6.isVisible());
        assertTrue(layer7.isVisible());

        assertTrue(layer0.isFolded());
        assertTrue(!layer1.isFolded());
        assertTrue(!layer2.isFolded());
        assertTrue(!layer3.isFolded());
        assertTrue(!layer4.isFolded());
        assertTrue(!layer5.isFolded());
        assertTrue(!layer6.isFolded());
        assertTrue(!layer7.isFolded());
    }

    @Test
    public void testFold2() {
        layer4.fold();
        assertTrue(layer0.isVisible());
        assertTrue(layer1.isVisible());
        assertTrue(layer2.isVisible());
        assertTrue(layer3.isVisible());
        assertTrue(layer4.isVisible());
        assertTrue(!layer5.isVisible());
        assertTrue(!layer6.isVisible());
        assertTrue(!layer7.isVisible());

        assertTrue(!layer0.isFolded());
        assertTrue(!layer1.isFolded());
        assertTrue(!layer2.isFolded());
        assertTrue(!layer3.isFolded());
        assertTrue(layer4.isFolded());
        assertTrue(!layer5.isFolded());
        assertTrue(!layer6.isFolded());
        assertTrue(!layer7.isFolded());
    }

    @Test
    public void testFold3() {
        tlayer0.fold();
        assertTrue(tlayer0.isVisible());
        assertTrue(!tlayer1.isVisible());
        assertTrue(!tlayer2.isVisible());
        assertTrue(!tlayer3.isVisible());

        assertTrue(tlayer0.isFolded());
        assertTrue(!tlayer1.isFolded());
        assertTrue(!tlayer2.isFolded());
        assertTrue(!tlayer3.isFolded());
    }

    @Test
    public void testFold4() {
        tlayer0.fold();
        tlayer0.unFold();
        assertTrue(tlayer0.isVisible());
        assertTrue(tlayer1.isVisible());
        assertTrue(tlayer2.isVisible());
        assertTrue(tlayer3.isVisible());

        assertTrue(!tlayer0.isFolded());
        assertTrue(!tlayer1.isFolded());
        assertTrue(!tlayer2.isFolded());
        assertTrue(!tlayer3.isFolded());
    }

    @Test
    public void testFold5() {
        tlayer1.fold();
        assertTrue(tlayer0.isVisible());
        assertTrue(tlayer1.isVisible());
        assertTrue(!tlayer2.isVisible());
        assertTrue(!tlayer3.isVisible());

        assertTrue(!tlayer0.isFolded());
        assertTrue(tlayer1.isFolded());
        assertTrue(!tlayer2.isFolded());
        assertTrue(!tlayer3.isFolded());
    }

    @Test
    public void testFold6() {
        tlayer1.fold();
        tlayer1.unFold();
        assertTrue(tlayer0.isVisible());
        assertTrue(tlayer1.isVisible());
        assertTrue(tlayer2.isVisible());
        assertTrue(tlayer3.isVisible());

        assertTrue(!tlayer0.isFolded());
        assertTrue(!tlayer1.isFolded());
        assertTrue(!tlayer2.isFolded());
        assertTrue(!tlayer3.isFolded());
    }

    @Test
    public void testFold7() {
        tlayer2.fold();
        tlayer1.fold();
        assertTrue(tlayer0.isVisible());
        assertTrue(tlayer1.isVisible());
        assertTrue(!tlayer2.isVisible());
        assertTrue(!tlayer3.isVisible());

        assertTrue(!tlayer0.isFolded());
        assertTrue(tlayer1.isFolded());
        assertTrue(tlayer2.isFolded());
        assertTrue(!tlayer3.isFolded());
    }

    @Test
    public void testFold8() {
        tlayer2.fold();
        tlayer1.fold();
        tlayer1.unFold();
        assertTrue(tlayer0.isVisible());
        assertTrue(tlayer1.isVisible());
        assertTrue(tlayer2.isVisible());
        assertTrue(!tlayer3.isVisible());

        assertTrue(!tlayer0.isFolded());
        assertTrue(!tlayer1.isFolded());
        assertTrue(tlayer2.isFolded());
        assertTrue(!tlayer3.isFolded());
    }

    @Test
    public void testUnfold() {
        layer4.fold();
        layer4.unFold();
        assertTrue(layer0.isVisible());
        assertTrue(layer1.isVisible());
        assertTrue(layer2.isVisible());
        assertTrue(layer3.isVisible());
        assertTrue(layer4.isVisible());
        assertTrue(layer5.isVisible());
        assertTrue(layer6.isVisible());
        assertTrue(layer7.isVisible());

        assertTrue(!layer0.isFolded());
        assertTrue(!layer1.isFolded());
        assertTrue(!layer2.isFolded());
        assertTrue(!layer3.isFolded());
        assertTrue(!layer4.isFolded());
        assertTrue(!layer5.isFolded());
        assertTrue(!layer6.isFolded());
        assertTrue(!layer7.isFolded());
    }
}
