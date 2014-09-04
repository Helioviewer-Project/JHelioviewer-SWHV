package org.helioviewer.jhv.plugins.swek.request;

import java.util.ArrayList;
import java.util.Date;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;

/**
 * This class intercepts changes of the layers and request data from the
 * JHVEventContainer.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class OutgoingRequestManager implements LayersListener {

    @Override
    public void layerAdded(int idx) {
        try {
            View activeView = LayersModel.getSingletonInstance().getActiveView();
            new ArrayList<Date>();
            if (activeView instanceof JHVJPXView) {
                JHVJPXView activeJPXView = (JHVJPXView) activeView;
                for (int frame = 1; frame <= activeJPXView.getMaximumFrameNumber(); frame++) {
                    activeJPXView.getJP2Image().getValueFromXML("DATE-OBS", "fits", frame);
                }
            }
        } catch (JHV_KduException ex) {
            Log.error("Received an kakadu exception. " + ex);
        }
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void layerChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void activeLayerChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void viewportGeometryChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void timestampChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void subImageDataChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void layerDownloaded(int idx) {
        // TODO Auto-generated method stub

    }

}
