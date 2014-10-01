package org.helioviewer.gl3d.plugin.hekplugin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.media.opengl.GL2;
import javax.swing.ImageIcon;

import org.helioviewer.base.math.Vector3dDouble;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.gl3d.view.GL3DLayeredView;
import org.helioviewer.gl3d.wcs.conversion.SphericalToSolarSphereConversion;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SphericalCoordinateSystem;
import org.helioviewer.jhv.data.datatype.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.JHVEvent;
import org.helioviewer.jhv.data.datatype.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.JHVPoint;
import org.helioviewer.jhv.data.datatype.JHVPositionInformation;
import org.helioviewer.jhv.plugins.hekplugin.cache.SWHVHEKData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderGraphics;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;

/**
 * The solar event renderer provides a possibility to draw solar events with
 * there associated icons.
 *
 * @author Malte Nuhn
 */
public class HEKPlugin3dRenderer extends PhysicalRenderer3d {
    private final SphericalCoordinateSystem sphericalCS = new SphericalCoordinateSystem();
    private final SolarSphereCoordinateSystem solarSphereCS = new SolarSphereCoordinateSystem();
    private float scale = 1;
    private double height = -1000;
    SphericalToSolarSphereConversion conversion = (SphericalToSolarSphereConversion) sphericalCS.getConversion(solarSphereCS);

    /**
     * Default constructor.
     */
    public HEKPlugin3dRenderer() {
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
                principleAngle = Double.parseDouble(param.getParameterValue()) * Math.PI / 180.;
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

        gl.glColor3d(evt.getColor().getRed(), evt.getColor().getGreen(), evt.getColor().getBlue());
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glLineWidth(0.5f);
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
        int i = 0;
        while (i < evt.getPositioningInformation().size() && evt.getPositioningInformation().get(i).getCoordinateSystem() != JHVCoordinateSystem.HGS) {
            i++;
        }
        GL2 gl = g.getGL();

        List<JHVPoint> points = evt.getPositioningInformation().get(i).getBoundCC();
        if (points == null || points.size() == 0) {
            points = evt.getPositioningInformation().get(i).getBoundBox();

        }
        if (points == null || points.size() == 0) {
            return;
        }
        String type = evt.getName();
        Color eventColor = evt.getColor();

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
                    gl.glVertex3d(xnew, -ynew, znew);
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
    public void drawIcon(PhysicalRenderGraphics g, JHVEvent evt, Date now) {
        ImageIcon icon = evt.getIcon();
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics graph = bi.createGraphics();
        icon.paintIcon(null, graph, 0, 0);
        graph.dispose();
        int i = 0;
        while (i < evt.getPositioningInformation().size() && evt.getPositioningInformation().get(i).getCoordinateSystem() != JHVCoordinateSystem.HGS) {
            i++;
        }
        if (i < evt.getPositioningInformation().size()) {
            JHVPositionInformation el = evt.getPositioningInformation().get(i);
            if (el.centralPoint() != null) {
                double theta = el.centralPoint().getCoordinate2() / 180. * Math.PI;// - Astronomy.getB0InRadians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
                double phi = el.centralPoint().getCoordinate1() / 180. * Math.PI - Astronomy.getL0Radians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
                double x = Math.cos(theta) * Math.sin(phi);
                double z = Math.cos(theta) * Math.cos(phi);
                double y = -Math.sin(theta);
                g.drawImage3d(bi, x, y, z, 0.5f);
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
    }

    @Override
    public void viewChanged(View view) {
        GL3DLayeredView layeredView = ViewHelper.getViewAdapter(view, GL3DLayeredView.class);
        if (layeredView != null) {
            GL3DImageTextureView imageTextureView = (GL3DImageTextureView) layeredView.getLayer(0);
            Region region = imageTextureView.getAdapter(RegionView.class).getRegion();
            if (region != null) {
                if (height < 0)
                    height = region.getHeight();
                else
                    scale = (float) (region.getHeight() / height);
            }
        }
    }

}
