package org.helioviewer.gl3d.plugin.hekplugin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector3dDouble;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.gl3d.view.GL3DLayeredView;
import org.helioviewer.gl3d.wcs.conversion.SphericalToSolarSphereConversion;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SphericalCoordinateSystem;
import org.helioviewer.jhv.data.datatype.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.JHVEvent;
import org.helioviewer.jhv.data.datatype.JHVPoint;
import org.helioviewer.jhv.data.datatype.JHVPositionInformation;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent.GenericTriangle;
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
        List<JHVPoint> points = evt.getPositioningInformation().get(i).getBoundBox();
        String type = evt.getName();
        Color eventColor = evt.getColor();

        Vector<HEKEvent.GenericTriangle<Vector3dDouble>> triangles = null;//evt.getTriangulation3D(now);

        if (triangles != null) {
            g.setColor(eventColor);
            for (GenericTriangle<Vector3dDouble> triangle : triangles) {
                Vector3dDouble tri[] = { triangle.A, triangle.B, triangle.C };
                g.fillPolygon(tri);
            }
        }

        // draw bounds
        g.setColor(new Color(255, 255, 255, 255));
        Vector3dDouble oldBoundaryPoint3d = null;

        for (JHVPoint point : points) {
            double theta = point.getCoordinate2() / 180. * Math.PI;// - Astronomy.getB0InRadians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
            double phi = point.getCoordinate1() / 180. * Math.PI - Astronomy.getL0Radians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));

            //System.out.println(el.centralPoint().getCoordinate1() + " " + el.centralPoint().getCoordinate2());
            double x = Math.cos(theta) * Math.sin(phi);
            double z = Math.cos(theta) * Math.cos(phi);
            double y = -Math.sin(theta);
            Vector3dDouble boundaryPoint3d = new Vector3dDouble(x, y, z);
            int divpoints = 10;
            double xold, yold, zold;
            xold = -2;
            yold = 0;
            zold = 0;
            if (oldBoundaryPoint3d != null) {
                for (int j = 0; j <= divpoints; j++) {
                    double alpha = 1. - 1. * j / divpoints;
                    double xnew = alpha * oldBoundaryPoint3d.getX() + (1 - alpha) * boundaryPoint3d.getX();
                    double ynew = alpha * oldBoundaryPoint3d.getY() + (1 - alpha) * boundaryPoint3d.getY();
                    double znew = alpha * oldBoundaryPoint3d.getZ() + (1 - alpha) * boundaryPoint3d.getZ();
                    double r = Math.sqrt(x * x + y * y + z * z);
                    xnew = 1.01 * xnew / r;
                    ynew = 1.01 * ynew / r;
                    znew = 1.01 * znew / r;
                    if (xold != -2) {
                        g.drawLine3d(xold, yold, zold, xnew, ynew, znew);
                    }
                    xold = xnew;
                    yold = ynew;
                    zold = znew;
                }

            }
            //if (oldBoundaryPoint3d != null)
            //    g.drawLine3d(oldBoundaryPoint3d.getX(), oldBoundaryPoint3d.getY(), oldBoundaryPoint3d.getZ(), boundaryPoint3d.getX(), boundaryPoint3d.getY(), boundaryPoint3d.getZ());

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
        //SphericalCoord stony = evt.getStony(now);
        int i = 0;
        while (i < evt.getPositioningInformation().size() && evt.getPositioningInformation().get(i).getCoordinateSystem() != JHVCoordinateSystem.HGS) {
            i++;
        }
        if (i < evt.getPositioningInformation().size()) {
            JHVPositionInformation el = evt.getPositioningInformation().get(i);
            double theta = el.centralPoint().getCoordinate2() / 180. * Math.PI;// - Astronomy.getB0InRadians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
            double phi = el.centralPoint().getCoordinate1() / 180. * Math.PI - Astronomy.getL0Radians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));

            //System.out.println(el.centralPoint().getCoordinate1() + " " + el.centralPoint().getCoordinate2());
            double x = Math.cos(theta) * Math.sin(phi);
            double z = Math.cos(theta) * Math.cos(phi);
            double y = -Math.sin(theta);
            g.drawImage3d(bi, x, y, z, 0.5f);
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
                Log.info(toDraw);
                for (JHVEvent evt : toDraw) {
                    drawPolygon(g, evt, currentDate);
                }

                for (JHVEvent evt : toDraw) {
                    drawIcon(g, evt, currentDate);
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
