package org.helioviewer.jhv.plugins.swhvhekplugin;

import org.helioviewer.gl3d.plugin.swhvhekplugin.SWHVHEKPlugin3dRenderer;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.opengl.OverlayPluginContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponentManager;

/**
 * Overlay plug-in to display different kinds of solar events retrieved from the
 * HEK API.
 * <p>
 * This plug-in provides a Treeview, categorizing the different Events available
 * on different tree levels:
 * <li>Catalogue (Default=HEK)</li>
 * <li>Event Type (Coronal Holes, ...)</li>
 * <li>Feature Recognition Method</li>
 * <li>Event</li>
 *
 * It is necessary to initially request the Tree structure for each Interval
 * currently looked at. Once the structure is loaded, the user can select
 * Different categories and download all the events "inside" these categories.
 *
 * The basic architecture is heavily inspired by the event catalogue developed
 * by Stephan Pagel.
 *
 * @author Malte Nuhn
 */
public class SWHVHEKPluginContainer extends OverlayContainer {

    // TODO: Malte Nuhn - Does storing the panel connected with this plugin fit
    // the architecture?

    private boolean builtin_mode = false;

    public SWHVHEKPluginContainer(boolean builtin_mode) {
        this.builtin_mode = builtin_mode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends PhysicalRenderer> getOverlayClass() {
        return SWHVHEKPluginRenderer.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installOverlayImpl(OverlayView overlayView, OverlayControlComponentManager controlList) {
        OverlayPluginContainer overlayPluginContainer = new OverlayPluginContainer();
        overlayPluginContainer.setRenderer(new SWHVHEKPluginRenderer());
        overlayPluginContainer.setRenderer3d(new SWHVHEKPlugin3dRenderer());
        overlayView.addOverlay(overlayPluginContainer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "HEK Events " + (builtin_mode ? "Built-In Version" : "");
    }

}
