/**
 *
 */
package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponentManager;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;

/**
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class SWEKPluginContainer extends OverlayContainer{

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void installOverlayImpl(OverlayView overlayView, OverlayControlComponentManager controlList) {
        // TODO Auto-generated method stub

    }

    @Override
    public Class<? extends PhysicalRenderer> getOverlayClass() {
        // TODO Auto-generated method stub
        return null;
    }



}
