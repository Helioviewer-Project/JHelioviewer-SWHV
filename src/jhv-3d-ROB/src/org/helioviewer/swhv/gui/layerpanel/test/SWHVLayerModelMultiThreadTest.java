package org.helioviewer.swhv.gui.layerpanel.test;

import static org.junit.Assert.assertTrue;

import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerController;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerModel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerPanel;
import org.helioviewer.swhv.gui.layerpanel.image.SWHVImageLayerModel;
import org.junit.Test;

public class SWHVLayerModelMultiThreadTest {

    @Test
    public void testCaseNaive() throws InterruptedException {
        final SWHVDateRangeLayerModel mmodel = new SWHVDateRangeLayerModel();
        SWHVDateRangeLayerPanel mdateRangepanel = new SWHVDateRangeLayerPanel();
        mmodel.addListener(mdateRangepanel);
        SWHVDateRangeLayerController mcontroller = new SWHVDateRangeLayerController(mmodel, mdateRangepanel);
        mmodel.setRoot();

        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    mmodel.addChild(new SWHVImageLayerModel());
                }
            }
        };
        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    mmodel.addChild(new SWHVImageLayerModel());
                }
            }
        };
        Thread t1 = new Thread(runnable1);
        t1.start();
        Thread t2 = new Thread(runnable2);
        t2.start();

        t1.join();
        t2.join();
        assertTrue(mmodel.getChildren().length == 2000);
    }

    @Test
    public void testCaseNaive2() throws InterruptedException {
        final SWHVDateRangeLayerModel mmodel = new SWHVDateRangeLayerModel();
        SWHVDateRangeLayerPanel mdateRangepanel = new SWHVDateRangeLayerPanel();
        mmodel.addListener(mdateRangepanel);
        SWHVDateRangeLayerController mcontroller = new SWHVDateRangeLayerController(mmodel, mdateRangepanel);
        mmodel.setRoot();

        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10000; i++) {
                    mmodel.addChild(new SWHVImageLayerModel());
                }
            }
        };
        final int[] removed = new int[1];
        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100000; i++) {
                    SWHVLayerModel[] children = mmodel.getChildren();
                    if (children.length > 0) {
                        mmodel.removeChild(0);
                        removed[0] = removed[0] + 1;
                    }
                }
            }
        };
        Thread t1 = new Thread(runnable1);
        t1.start();
        Thread t2 = new Thread(runnable2);
        t2.start();

        t1.join();
        t2.join();
        assertTrue(mmodel.getChildren().length == (10000 - removed[0]));
    }
}
