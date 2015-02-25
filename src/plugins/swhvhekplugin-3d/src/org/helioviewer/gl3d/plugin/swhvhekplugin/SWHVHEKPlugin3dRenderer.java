package org.helioviewer.gl3d.plugin.swhvhekplugin;

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
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVPoint;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.plugins.swhvhekplugin.cache.SWHVHEKData;
import org.helioviewer.jhv.plugins.swhvhekplugin.settings.SWHVHEKSettings;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderGraphics;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;

/**
 * The solar event renderer provides a possibility to draw solar events with
 * there associated icons.
 *
 * @author Malte Nuhn
 */
public class SWHVHEKPlugin3dRenderer extends PhysicalRenderer3d {

    /**
     * Default constructor.
     */
    public SWHVHEKPlugin3dRenderer() {
    }

    public void drawCactusArc(PhysicalRenderGraphics g, JHVEvent evt, Date now) {
        GL2 gl = g.getGL();
        List<JHVEventParameter> params = evt.getAllEventParameters();
        double principleAngle = 0;
        double angularWidth = 0;
        double distSun = 1.;
        for (JHVEventParameter param : params) {
            if (param.getParameterName().equals("cme_angularwidth")) {
                angularWidth = Double.parseDouble(param.getParameterValue()) * Math.PI / 180.;
            }
            if (param.getParameterName().equals("event_coord1")) {
                principleAngle = -(Double.parseDouble(param.getParameterValue()) - 90.) * Math.PI / 180.;
            }
            if (param.getParameterName().equals("event_coord2")) {
                distSun = Double.parseDouble(param.getParameterValue());
            }
        }
        double arcResolution = 100;
        double thetaDelta = Astronomy.getB0InRadians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
        double thetaStart = principleAngle - angularWidth / 2.;
        double thetaEnd = principleAngle + angularWidth / 2.;
        double phi = -Math.PI / 2. - Astronomy.getL0Radians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
        double lineResolution = 10;
        Color eventColor = evt.getEventRelationShip().getRelationshipColor();
        gl.glColor3d(eventColor.getRed() / 255., eventColor.getGreen() / 255., eventColor.getBlue() / 255.);
        gl.glDisable(GL2.GL_LIGHTING);

        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        if (evt.isHighlighted()) {
            gl.glLineWidth(1.6f);
        } else {
            gl.glLineWidth(0.8f);
        }
        gl.glBegin(GL2.GL_LINE_STRIP);
        for (int i = 0; i <= lineResolution; i++) {
            double alpha = 1. - 1. * i / arcResolution;
            double r = alpha * distSun + (1 - alpha) * (distSun + 2);
            double theta = thetaStart;

            double x = r * Math.cos(theta) * Math.sin(phi);
            double z = r * Math.cos(theta) * Math.cos(phi);
            double y = r * Math.sin(theta);
            double yrot = y * Math.cos(thetaDelta) + z * Math.sin(thetaDelta);
            double zrot = -y * Math.sin(thetaDelta) + z * Math.cos(thetaDelta);
            double xrot = x;

            gl.glVertex3d(xrot, yrot, zrot);
        }
        for (int i = 0; i <= arcResolution; i++) {
            double alpha = 1. - 1. * i / arcResolution;
            double theta = alpha * thetaStart + (1 - alpha) * thetaEnd;

            double x = distSun * Math.cos(theta) * Math.sin(phi);
            double z = distSun * Math.cos(theta) * Math.cos(phi);
            double y = distSun * Math.sin(theta);
            double yrot = y * Math.cos(thetaDelta) + z * Math.sin(thetaDelta);
            double zrot = -y * Math.sin(thetaDelta) + z * Math.cos(thetaDelta);
            double xrot = x;

            gl.glVertex3d(xrot, yrot, zrot);
        }
        for (int i = 0; i <= lineResolution; i++) {
            double alpha = 1. - 1. * i / arcResolution;
            double r = alpha * distSun + (1 - alpha) * (distSun + 2);
            double theta = thetaEnd;

            double x = r * Math.cos(theta) * Math.sin(phi);
            double z = r * Math.cos(theta) * Math.cos(phi);
            double y = r * Math.sin(theta);
            double yrot = y * Math.cos(thetaDelta) + z * Math.sin(thetaDelta);
            double zrot = -y * Math.sin(thetaDelta) + z * Math.cos(thetaDelta);
            double xrot = x;

            gl.glVertex3d(xrot, yrot, zrot);
        }
        gl.glEnd();
        gl.glDisable(GL2.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_LIGHTING);

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
        HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

        if (!pi.containsKey(JHVCoordinateSystem.JHV)) {
            return;
        }
        JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV);

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
        if (evt.getEventRelationShip().getRelationshipColor() != null) {
            Color evtColor = evt.getEventRelationShip().getRelationshipColor();
            gl.glColor3d(evtColor.getRed() / 255., evtColor.getGreen() / 255., evtColor.getBlue() / 255.);
        } else {
            gl.glColor3d(evt.getColor().getRed() / 255., evt.getColor().getGreen() / 255., evt.getColor().getBlue() / 255.);
        }
        gl.glEnable(GL2.GL_BLEND);
        gl.glDisable(GL2.GL_LIGHTING);

        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        if (evt.isHighlighted()) {
            gl.glLineWidth(1.6f);
        } else {
            gl.glLineWidth(0.7f);
        }
        for (JHVPoint point : points) {
            int divpoints = 10;
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
                    gl.glVertex3d(xnew, -ynew, znew);
                }

            }
            gl.glEnd();

            oldBoundaryPoint3d = point;
        }
        gl.glDisable(GL2.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_LIGHTING);

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

        if (pi.containsKey(JHVCoordinateSystem.JHV)) {
            JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV);
            if (el.centralPoint() != null) {
                JHVPoint pt = el.centralPoint();
                if (evt.isHighlighted()) {
                    g.drawImage3d(bi, pt.getCoordinate1(), pt.getCoordinate2(), pt.getCoordinate3(), 0.16f);
                } else {
                    g.drawImage3d(bi, pt.getCoordinate1(), pt.getCoordinate2(), pt.getCoordinate3(), 0.1f);
                }
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
                    if (evt.getName().equals("Coronal Mass Ejection")) {
                        drawCactusArc(g, evt, currentDate);
                    } else {
                        drawPolygon(g, evt, currentDate);
                        drawIcon(g, evt, currentDate);
                    }
                }
            }
        }
        SWHVHEKSettings.resetCactusColor();
    }

}
