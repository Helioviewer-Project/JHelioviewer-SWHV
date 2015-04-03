package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.media.opengl.GL2;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.gl3d.math.GL3DMat4d;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVPoint;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;
import org.helioviewer.viewmodel.renderer.GLCommonRenderGraphics;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;

public class SWHVHEKPluginRenderable implements Renderable {

    private final GLCommonRenderGraphics commonRenderGraphics = GLCommonRenderGraphics.getSingletonInstance();
    private static HashMap<String, BufferedImage> iconCache = new HashMap<String, BufferedImage>();

    private boolean isVisible = true;

    public SWHVHEKPluginRenderable() {
        Displayer.getRenderablecontainer().addRenderable(this);
    }

    public void drawCactusArc(GL2 gl, JHVEvent evt, Date now) {
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
    public void drawPolygon(GL2 gl, JHVEvent evt, Date now) {
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

        // draw bounds
        JHVPoint oldBoundaryPoint3d = null;
        if (evt.getEventRelationShip().getRelationshipColor() != null) {
            Color evtColor = evt.getEventRelationShip().getRelationshipColor();
            gl.glColor3d(evtColor.getRed() / 255., evtColor.getGreen() / 255., evtColor.getBlue() / 255.);
        } else {
            gl.glColor3d(evt.getColor().getRed() / 255., evt.getColor().getGreen() / 255., evt.getColor().getBlue() / 255.);
        }
        gl.glEnable(GL2.GL_BLEND);

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
    }

    public void drawIcon(GL2 gl, JHVEvent evt, Date now) {
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
                    this.drawImage3d(gl, bi, pt.getCoordinate1(), pt.getCoordinate2(), pt.getCoordinate3(), 0.16, 0.16);
                } else {
                    this.drawImage3d(gl, bi, pt.getCoordinate1(), pt.getCoordinate2(), pt.getCoordinate3(), 0.1, 0.1);
                }
            }
        }
    }

    public void drawImage3d(GL2 gl, BufferedImage image, double x, double y, double z, double width, double height) {
        y = -y;

        commonRenderGraphics.bindImage(gl, image);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

        gl.glColor3f(1.0f, 1.0f, 1.0f);
        double width2 = width / 2.0;
        double height2 = height / 2.0;

        GL3DVec3d sourceDir = new GL3DVec3d(0, 0, 1);
        GL3DVec3d targetDir = new GL3DVec3d(x, y, z);

        GL3DVec3d axis = sourceDir.cross(targetDir);
        axis.normalize();
        GL3DMat4d r = GL3DMat4d.rotation(Math.atan2(x, z), GL3DVec3d.YAxis);
        r.rotate(-Math.asin(y / targetDir.length()), GL3DVec3d.XAxis);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_TEXTURE_2D);

        GL3DVec3d p0 = new GL3DVec3d(-width2, -height2, 0);
        GL3DVec3d p1 = new GL3DVec3d(-width2, height2, 0);
        GL3DVec3d p2 = new GL3DVec3d(width2, height2, 0);
        GL3DVec3d p3 = new GL3DVec3d(width2, -height2, 0);

        p0 = r.multiply(p0);
        p1 = r.multiply(p1);
        p2 = r.multiply(p2);
        p3 = r.multiply(p3);
        p0.add(targetDir);
        p1.add(targetDir);
        p2.add(targetDir);
        p3.add(targetDir);

        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glTexCoord2f(1.0f, 1.0f);
            gl.glVertex3d(p3.x, p3.y, p3.z);
            gl.glTexCoord2f(1.0f, 0.0f);
            gl.glVertex3d(p2.x, p2.y, p2.z);
            gl.glTexCoord2f(0.0f, 0.0f);
            gl.glVertex3d(p1.x, p1.y, p1.z);
            gl.glTexCoord2f(0.0f, 1.0f);
            gl.glVertex3d(p0.x, p0.y, p0.z);
        }
        gl.glEnd();

        gl.glDisable(GL2.GL_BLEND);
        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_BLEND);
    }

    @Override
    public void init(GL3DState state) {

    }

    @Override
    public void render(GL3DState state) {
        if (isVisible) {
            GL2 gl = state.gl;
            TimedMovieView masterView = LinkedMovieManager.getActiveInstance().getMasterMovie();
            if (masterView != null && masterView.getCurrentFrameDateTime() != null) {
                Date currentDate = masterView.getCurrentFrameDateTime().getTime();

                if (currentDate != null) {
                    ArrayList<JHVEvent> toDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(currentDate);
                    for (JHVEvent evt : toDraw) {
                        if (evt.getName().equals("Coronal Mass Ejection")) {
                            drawCactusArc(gl, evt, currentDate);
                        } else {
                            drawPolygon(gl, evt, currentDate);
                            drawIcon(gl, evt, currentDate);
                        }
                    }
                }
            }
            SWHVHEKSettings.resetCactusColor();
        }
    }

    @Override
    public void remove(GL3DState state) {
    }

    @Override
    public RenderableType getType() {
        return new RenderableType("HEK plugin");
    }

    @Override
    public Component getOptionsPanel() {
        return new JPanel();
    }

    @Override
    public String getName() {
        return "HEK Events";
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    @Override
    public String getTimeString() {
        return "";
    }

}
