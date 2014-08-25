package org.helioviewer.swhv.gui.layerpanel.test;

import static org.junit.Assert.assertTrue;

import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import javax.swing.SwingUtilities;

import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerController;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerModel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerPanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SWHVDateRangeTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        Frame f = new Frame();
        final SWHVDateRangeLayerModel model = new SWHVDateRangeLayerModel();
        final SWHVDateRangeLayerPanel panel = new SWHVDateRangeLayerPanel();
        model.addListener(panel);
        Date endDate = new Date(System.currentTimeMillis());
        Date beginDate = new Date(endDate.getTime() - 1000 * 60 * 60 * 4);
        SWHVDateRangeLayerController controller = new SWHVDateRangeLayerController(model, panel);
        f.add(panel);
        f.pack();
        f.setVisible(true);
        model.setBeginDate(beginDate);
        model.setEndDate(endDate);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    System.out.println(panel.getDateLabelText());
                    System.out.println(model.getBeginDate());
                }
            });
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        System.out.println(panel.getDateLabelText());

        assertTrue(panel.getDateLabelText().equals("2014-03-03T12:00:00 - 2014-03-04T15:00:00 "));
    }

}
