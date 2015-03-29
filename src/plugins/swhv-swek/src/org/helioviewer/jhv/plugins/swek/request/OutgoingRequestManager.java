package org.helioviewer.jhv.plugins.swek.request;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.swek.receive.SWEKEventHandler;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

/**
 * This class intercepts changes of the layers and request data from the
 * JHVEventContainer.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class OutgoingRequestManager implements LayersListener {

    /** The singleton instance of the outgoing request manager */
    private static OutgoingRequestManager instance;

    /** instance of the swek event handler */
    private final SWEKEventHandler swekEventHandler;

    /**
     * private constructor
     */
    private OutgoingRequestManager() {
        swekEventHandler = SWEKEventHandler.getSingletonInstance();
    }

    /**
     * Gets the singleton instance of the outgoing request manager
     * 
     * @return the singleton instance
     */
    public static OutgoingRequestManager getSingletonInstance() {
        if (instance == null) {
            instance = new OutgoingRequestManager();
        }
        return instance;
    }

    @Override
    public void layerAdded(int idx) {
    }

    @Override
    public void layerRemoved(int oldIdx) {
    }

    @Override
    public void activeLayerChanged(View view) {
        if (view instanceof JHVJPXView) {
            JHVJPXView jpxView = (JHVJPXView) view;
            List<Date> requestDates = new ArrayList<Date>();
            for (int frame = 0; frame <= jpxView.getMaximumFrameNumber(); frame++) {
                requestDates.add(jpxView.getFrameDateTime(frame).getTime());
            }
            JHVEventContainer.getSingletonInstance().requestForDateList(requestDates, swekEventHandler);
        }
    }

}
