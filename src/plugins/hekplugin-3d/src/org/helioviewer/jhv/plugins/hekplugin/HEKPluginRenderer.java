package org.helioviewer.jhv.plugins.hekplugin;

import java.util.Date;

import org.helioviewer.jhv.data.datatype.JHVEvent;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderGraphics;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;

/**
 * The solar event renderer provides a possibility to draw solar events with
 * there associated icons.
 *
 * @author Malte Nuhn
 */
public class HEKPluginRenderer implements PhysicalRenderer {

    /**
     * Default constructor.
     */
    public HEKPluginRenderer() {
    }

    /**
     * The actual rendering routine
     *
     * @param g
     *            - PhysicalRenderGraphics to render to
     * @param evt
     *            - Event to draw
     * @param now
     *            - Current point in time
     */
    public void drawPolygon(PhysicalRenderGraphics g, JHVEvent evt, Date now) {

    }

    /**
     * The actual rendering routine
     *
     * @param g
     *            - PhysicalRenderGraphics to render to
     * @param evt
     *            - Event to draw
     * @param now
     *            - Current point in time
     */
    public void drawIcon(PhysicalRenderGraphics g, JHVEvent evt, Date now) {

    }

    /**
     * {@inheritDoc}
     *
     * Draws all available and visible solar events with there associated icon.
     */
    @Override
    public void render(PhysicalRenderGraphics g) {
        TimedMovieView masterView = LinkedMovieManager.getActiveInstance().getMasterMovie();
        if (masterView != null && masterView.getCurrentFrameDateTime() != null) {
            Date currentDate = masterView.getCurrentFrameDateTime().getTime();

        }
    }

}
