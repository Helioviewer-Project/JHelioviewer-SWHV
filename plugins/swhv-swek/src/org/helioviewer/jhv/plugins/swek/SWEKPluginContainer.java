/**
 *
 */
package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.jhv.plugins.swek.rendering.SWEKRenderer;
import org.helioviewer.jhv.plugins.swek.view.SWEKPluginPanel;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponent;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponentManager;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.opengl.OverlayPluginContainer;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;

/**
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class SWEKPluginContainer extends OverlayContainer{

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
        SWEKPluginPanel swekPanel = new SWEKPluginPanel();
        OverlayPluginContainer overlayPluginContainer = new OverlayPluginContainer();
        overlayPluginContainer.setRenderer(new SWEKRenderer());
        overlayView.addOverlay(overlayPluginContainer);
        controlList.add(new OverlayControlComponent(swekPanel, getName()));


    }

    @Override
    public Class<? extends PhysicalRenderer> getOverlayClass() {
        // TODO Auto-generated method stub
        return null;
    }



}
