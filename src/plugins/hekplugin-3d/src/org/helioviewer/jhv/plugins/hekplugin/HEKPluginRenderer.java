package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.swing.ImageIcon;

import org.helioviewer.base.math.Vector3dDouble;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.jhv.data.datatype.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.JHVEvent;
import org.helioviewer.jhv.data.datatype.JHVPoint;
import org.helioviewer.jhv.data.datatype.JHVPositionInformation;
import org.helioviewer.jhv.plugins.hekplugin.cache.SWHVHEKData;
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

    public void drawPolygon(PhysicalRenderGraphics g, JHVEvent evt, Date now) {
        HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

        if (!pi.containsKey(JHVCoordinateSystem.HGS)) {
            return;
        }
        JHVPositionInformation el = pi.get(JHVCoordinateSystem.HGS);
        List<JHVPoint> points = el.getBoundCC();
        if (points == null || points.size() == 0) {
            points = el.getBoundBox();

        }
        if (points == null || points.size() == 0) {
            return;
        }

        GL2 gl = g.getGL();

        // draw bounds
        Vector3dDouble oldBoundaryPoint3d = null;

        for (JHVPoint point : points) {
            double theta = point.getCoordinate2() / 180. * Math.PI;// - Astronomy.getB0InRadians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
            double phi = point.getCoordinate1() / 180. * Math.PI - Astronomy.getL0Radians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));

            double x = Math.cos(theta) * Math.sin(phi);
            double z = Math.cos(theta) * Math.cos(phi);
            double y = -Math.sin(theta);
            Vector3dDouble boundaryPoint3d = new Vector3dDouble(x, y, z);
            int divpoints = 10;
            gl.glColor3d(evt.getColor().getRed(), evt.getColor().getGreen(), evt.getColor().getBlue());
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glEnable(GL2.GL_LINE_SMOOTH);
            gl.glLineWidth(0.5f);
            gl.glBegin(GL2.GL_LINE_STRIP);
            if (oldBoundaryPoint3d != null) {
                for (int j = 0; j <= divpoints; j++) {
                    double alpha = 1. - 1. * j / divpoints;
                    double xnew = alpha * oldBoundaryPoint3d.getX() + (1 - alpha) * boundaryPoint3d.getX();
                    double ynew = alpha * oldBoundaryPoint3d.getY() + (1 - alpha) * boundaryPoint3d.getY();
                    double znew = alpha * oldBoundaryPoint3d.getZ() + (1 - alpha) * boundaryPoint3d.getZ();
                    double r = Math.sqrt(xnew * xnew + ynew * ynew + znew * znew);
                    xnew = xnew / r;
                    ynew = ynew / r;
                    znew = znew / r;
                    gl.glVertex3d(xnew, -ynew, 0.);
                }

            }
            gl.glEnd();
            gl.glDisable(GL2.GL_LINE_SMOOTH);

            oldBoundaryPoint3d = boundaryPoint3d;
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
        gl.glDisable(GL.GL_TEXTURE_1D);
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

        if (pi.containsKey(JHVCoordinateSystem.HGS)) {
            JHVPositionInformation el = pi.get(JHVCoordinateSystem.HGS);
            if (el.centralPoint() != null) {
                double theta = el.centralPoint().getCoordinate2() / 180. * Math.PI;// - Astronomy.getB0InRadians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
                double phi = el.centralPoint().getCoordinate1() / 180. * Math.PI - Astronomy.getL0Radians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
                double x = Math.cos(theta) * Math.sin(phi);
                double z = Math.cos(theta) * Math.cos(phi);
                double y = -Math.sin(theta);
                g.drawImage(bi, x, y);
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
