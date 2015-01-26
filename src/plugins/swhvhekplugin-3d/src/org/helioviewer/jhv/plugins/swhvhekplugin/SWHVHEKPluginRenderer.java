package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.media.opengl.GL2;
import javax.swing.ImageIcon;

import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVPoint;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.plugins.swhvhekplugin.cache.SWHVHEKData;
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
public class SWHVHEKPluginRenderer implements PhysicalRenderer {

    /**
     * Default constructor.
     */
    public SWHVHEKPluginRenderer() {
    }

    public void drawPolygon(PhysicalRenderGraphics g, JHVEvent evt, Date now) {
        HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

        if (!pi.containsKey(JHVCoordinateSystem.JHV2D)) {
            return;
        }
        JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV2D);
        List<JHVPoint> points = el.getBoundCC();
        if (points == null || points.size() == 0) {
            points = el.getBoundBox();

        }
        if (points == null || points.size() == 0) {
            return;
        }

        GL2 gl = g.getGL();

        // draw bounds
        JHVPoint oldBoundaryPoint3d = null;

        for (JHVPoint point : points) {
            double theta = point.getCoordinate2() / 180. * Math.PI;// - Astronomy.getB0InRadians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
            double phi = point.getCoordinate1() / 180. * Math.PI - Astronomy.getL0Radians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));

            int divpoints = 10;
            Color evtColor = evt.getEventRelationShip().getRelationshipColor();

            gl.glColor3d(evtColor.getRed() / 255., evtColor.getGreen() / 255., evtColor.getBlue() / 255.);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glEnable(GL2.GL_LINE_SMOOTH);
            gl.glLineWidth(0.5f);
            gl.glBegin(GL2.GL_LINE_STRIP);
            if (oldBoundaryPoint3d != null) {
                for (int j = 0; j <= divpoints; j++) {
                    double alpha = 1. - 1. * j / divpoints;
                    double xnew = alpha * oldBoundaryPoint3d.getCoordinate1() + (1 - alpha) * point.getCoordinate1();
                    double ynew = alpha * oldBoundaryPoint3d.getCoordinate2() + (1 - alpha) * point.getCoordinate2();
                    double znew = alpha * oldBoundaryPoint3d.getCoordinate3() + (1 - alpha) * point.getCoordinate3();
                    double r = Math.sqrt(xnew * xnew + ynew * ynew + znew * znew);
                    xnew = xnew / r;
                    ynew = ynew / r;
                    znew = znew / r;
                    gl.glVertex3d(xnew, -ynew, 0.);
                }

            }
            gl.glEnd();
            gl.glDisable(GL2.GL_LINE_SMOOTH);

            oldBoundaryPoint3d = point;
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
    private static HashMap<String, BufferedImage> iconCache = new HashMap<String, BufferedImage>();

    public void drawIcon(PhysicalRenderGraphics g, JHVEvent evt, Date now) {
        BufferedImage bi;
        String type = evt.getJHVEventType().getEventType();
        GL2 gl = g.getGL();
        gl.glDisable(GL2.GL_TEXTURE_1D);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        if (iconCache.containsKey(type)) {
            bi = iconCache.get(type);
        } else {
            ImageIcon icon = evt.getIcon();
            bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics graph = bi.createGraphics();
            icon.paintIcon(null, graph, 0, 0);
            graph.dispose();
            iconCache.put(type, bi);
        }
        HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

        if (pi.containsKey(JHVCoordinateSystem.JHV2D)) {
            JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV2D);
            if (el.centralPoint() != null) {
                JHVPoint pt = el.centralPoint();
                g.drawImage(bi, pt.getCoordinate1(), pt.getCoordinate2());
            }
        }

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

            if (currentDate != null) {
                ArrayList<JHVEvent> toDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(currentDate);

                for (JHVEvent evt : toDraw) {
                    drawPolygon(g, evt, currentDate);
                }

                for (JHVEvent evt : toDraw) {
                    drawIcon(g, evt, currentDate);
                }
            }
        }
    }

}
