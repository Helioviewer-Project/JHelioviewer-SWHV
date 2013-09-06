package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.Vector;

import org.helioviewer.base.math.SphericalCoord;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent.GenericTriangle;
import org.helioviewer.jhv.plugins.hekplugin.math.HEKCoordinateTransform;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
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
    public void drawPolygon(PhysicalRenderGraphics g, HEKEvent evt, Date now) {

        if (evt != null && evt.isVisible(now)) {

            String type = evt.getString("event_type");
            Color eventColor = HEKConstants.getSingletonInstance().acronymToColor(type, 128);

            Vector<HEKEvent.GenericTriangle<Vector2dDouble>> triangles = evt.getTriangulation(now);

            if (triangles != null) {
                g.setColor(eventColor);
                for (GenericTriangle<Vector2dDouble> triangle : triangles) {
                    Vector2dDouble tri[] = { triangle.A, triangle.B, triangle.C };
                    g.fillPolygon(tri);
                }
            }

            // draw bounds
            g.setColor(new Color(255, 255, 255, 68));
            int timeDifferenceInSeconds = (int) ((now.getTime() - evt.getStart().getTime()) / 1000);

            Vector<SphericalCoord> outerBound = evt.getStonyBound(now);
            Vector2dDouble oldBoundaryPoint2d = null;
            double oldPhi = 0;

            if (outerBound != null) {
                for (SphericalCoord boundaryPoint : outerBound) {
                    SphericalCoord rotatedBoundaryPoint = HEKCoordinateTransform.StonyhurstRotateStonyhurst(boundaryPoint, timeDifferenceInSeconds);
                    Vector2dDouble boundaryPoint2d = HEKEvent.convertToScreenCoordinates(rotatedBoundaryPoint, now);

                    if (oldBoundaryPoint2d != null) {
                        if (Math.abs(rotatedBoundaryPoint.phi) < 90 && Math.abs(oldPhi) < 90) {
                            g.drawLine(oldBoundaryPoint2d, boundaryPoint2d);
                        }
                    }

                    oldBoundaryPoint2d = boundaryPoint2d;
                    oldPhi = rotatedBoundaryPoint.phi;
                }
            }

        }

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
    public void drawIcon(PhysicalRenderGraphics g, HEKEvent evt, Date now) {
        if (evt != null && evt.isVisible(now)) {
            boolean large = evt.getShowEventInfo();
            BufferedImage icon = evt.getIcon(large);
            if (icon != null) {
                g.drawImage(icon, evt.getScreenCoordinates(now));
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Draws all available and visible solar events with there associated icon.
     */
    public void render(PhysicalRenderGraphics g) {
        TimedMovieView masterView = LinkedMovieManager.getActiveInstance().getMasterMovie();
        if (masterView != null && masterView.getCurrentFrameDateTime() != null) {
            Date currentDate = masterView.getCurrentFrameDateTime().getTime();

            if (currentDate != null) {
                Vector<HEKEvent> toDraw = HEKCache.getSingletonInstance().getModel().getActiveEvents(currentDate);

                for (HEKEvent evt : toDraw) {
                    drawPolygon(g, evt, currentDate);
                }

                for (HEKEvent evt : toDraw) {
                    drawIcon(g, evt, currentDate);
                }
            }
        }
    }
}
