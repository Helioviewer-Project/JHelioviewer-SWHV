/**
 *
 */
package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.jhv.plugins.swek.view.SWEKPluginPanel;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.opengl.OverlayPluginContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponent;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponentManager;

/**
 * @author Bram.Bourgoignie@oma.be
 * 
 */
public class SWEKPluginContainer extends OverlayContainer {

    @Override
    public String getName() {
        return "Space Weather Event Knowledgebase";
    }

    @Override
    public String getDescription() {
        return "A description";
    }

    @Override
    protected void installOverlayImpl(OverlayView overlayView, OverlayControlComponentManager controlList) {
        SWEKPluginPanel swekPanel = SWEKPluginPanel.getSWEKPluginPanelInstance();
        OverlayPluginContainer overlayPluginContainer = new OverlayPluginContainer();
        overlayView.addOverlay(overlayPluginContainer);
        controlList.add(new OverlayControlComponent(swekPanel, getName()));

    }

    @Override
    public Class<? extends PhysicalRenderer> getOverlayClass() {
        // TODO Auto-generated method stub
        return null;
    }

}
