package org.helioviewer.jhv.plugins.hekplugin;

import java.util.Date;

import org.helioviewer.base.math.Interval;
import org.helioviewer.gl3d.plugin.hekplugin.HEKPlugin3dRenderer;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.opengl.OverlayPluginContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponent;
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
public class HEKPluginContainer extends OverlayContainer {

    // TODO: Malte Nuhn - Does storing the panel connected with this plugin fit
    // the architecture?

    private HEKPluginPanel hekPanel;
    private boolean builtin_mode = false;

    public HEKPluginContainer(boolean builtin_mode) {
        this.builtin_mode = builtin_mode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends PhysicalRenderer> getOverlayClass() {
        return HEKPluginRenderer.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installOverlayImpl(OverlayView overlayView, OverlayControlComponentManager controlList) {

        hekPanel = new HEKPluginPanel();
        OverlayPluginContainer overlayPluginContainer = new OverlayPluginContainer();
        overlayPluginContainer.setRenderer(new HEKPluginRenderer());
        overlayPluginContainer.setRenderer3d(new HEKPlugin3dRenderer());
        overlayView.addOverlay(overlayPluginContainer);
        controlList.add(new OverlayControlComponent(hekPanel, getName()));

        /*
         * hekPanel = new HEKPluginPanel(HEKCache.getSingletonInstance());
         * overlayView.setRenderer(new HEKPluginRenderer()); controlList.add(new
         * OverlayControlComponent(hekPanel, getName()));
         */
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

    /**
     * @see HEKPanel#setCurInterval()
     */
    public void setCurInterval(Interval<Date> newInterval) {
        hekPanel.setCurInterval(newInterval);
    }

    /**
     * @see HEKPanel#getStructure()
     */
    public void getStructure() {
        hekPanel.getStructure();
    }

    public void setEnabled(boolean b) {
        hekPanel.setEnabled(b);
    }

}
