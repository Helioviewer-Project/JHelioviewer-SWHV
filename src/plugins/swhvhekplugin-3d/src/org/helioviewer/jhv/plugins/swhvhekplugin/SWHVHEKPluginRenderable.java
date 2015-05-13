package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;

import org.helioviewer.base.math.GL3DMat4d;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVPoint;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.opengl.GLTexture;

import com.jogamp.opengl.GL2;

public class SWHVHEKPluginRenderable implements Renderable {

    private static HashMap<String, GLTexture> iconCacheId = new HashMap<String, GLTexture>();

    private boolean isVisible = true;

    private final RenderableType type;

    public SWHVHEKPluginRenderable() {
        this.type = new RenderableType("HEK plugin");
        ImageViewerGui.getRenderableContainer().addRenderable(this);
    }

    private void bindTexture(GL2 gl, String key, ImageIcon icon) {
        GLTexture tex = iconCacheId.get(key);
        if (tex == null) {
            BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics graph = bi.createGraphics();
            icon.paintIcon(null, graph, 0, 0);
            graph.dispose();

            tex = new GLTexture(gl);
            tex.bind(gl, GL2.GL_TEXTURE_2D);
            tex.copyBufferedImage2D(gl, bi);
            iconCacheId.put(key, tex);
        }

        tex.bind(gl, GL2.GL_TEXTURE_2D);
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
        double thetaDelta = Astronomy.getB0Radians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
        double thetaStart = principleAngle - angularWidth / 2.;
        double thetaEnd = principleAngle + angularWidth / 2.;
        double phi = -Math.PI / 2. - Astronomy.getL0Radians(new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
        double lineResolution = 10;
        Color eventColor = evt.getEventRelationShip().getRelationshipColor();
        gl.glColor3f(eventColor.getRed() / 255f, eventColor.getGreen() / 255f, eventColor.getBlue() / 255f);

        if (evt.isHighlighted()) {
            gl.glLineWidth(1.6f);
        } else {
            gl.glLineWidth(0.8f);
        }

        gl.glDisable(GL2.GL_TEXTURE_2D);
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

            gl.glVertex3f((float) xrot, (float) yrot, (float) zrot);
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

            gl.glVertex3f((float) xrot, (float) yrot, (float) zrot);
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

            gl.glVertex3f((float) xrot, (float) yrot, (float) zrot);
        }
        gl.glEnd();
        gl.glEnable(GL2.GL_TEXTURE_2D);
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
            gl.glColor3f(evtColor.getRed() / 255f, evtColor.getGreen() / 255f, evtColor.getBlue() / 255f);
        } else {
            gl.glColor3f(evt.getColor().getRed() / 255f, evt.getColor().getGreen() / 255f, evt.getColor().getBlue() / 255f);
        }

        if (evt.isHighlighted()) {
            gl.glLineWidth(1.6f);
        } else {
            gl.glLineWidth(0.7f);
        }

        gl.glDisable(GL2.GL_TEXTURE_2D);
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
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    public void drawIcon(GL2 gl, JHVEvent evt, Date now) {
        String type = evt.getJHVEventType().getEventType();
        HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

        if (pi.containsKey(JHVCoordinateSystem.JHV)) {
            JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV);
            if (el.centralPoint() != null) {
                JHVPoint pt = el.centralPoint();
                bindTexture(gl, type, evt.getIcon());
                if (evt.isHighlighted()) {
                    this.drawImage3d(gl, pt.getCoordinate1(), pt.getCoordinate2(), pt.getCoordinate3(), 0.16, 0.16);
                } else {
                    this.drawImage3d(gl, pt.getCoordinate1(), pt.getCoordinate2(), pt.getCoordinate3(), 0.1, 0.1);
                }
            }
        }
    }

    public void drawImage3d(GL2 gl, double x, double y, double z, double width, double height) {
        y = -y;

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

        gl.glEnable(GL2.GL_CULL_FACE);
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
        gl.glDisable(GL2.GL_CULL_FACE);
    }

    @Override
    public void render(GL2 gl) {
        AbstractView view;
        if (isVisible && (view = Displayer.getLayersModel().getActiveView()) != null) {
            Date currentDate = view.getMetaData().getDateTime().getTime();
            ArrayList<JHVEvent> toDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(currentDate);
            for (JHVEvent evt : toDraw) {
                if (evt.getName().equals("Coronal Mass Ejection")) {
                    drawCactusArc(gl, evt, currentDate);
                } else {
                    drawPolygon(gl, evt, currentDate);
                    drawIcon(gl, evt, currentDate);
                }
            }
            SWHVHEKSettings.resetCactusColor();
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public RenderableType getType() {
        return type;
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return "HEK events";
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

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public boolean isActiveImageLayer() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void dispose(GL2 gl) {
        for (GLTexture el : iconCacheId.values()) {
            el.delete(gl);
        }
        iconCacheId.clear();
    }

}
