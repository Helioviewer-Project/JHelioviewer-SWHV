package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import org.helioviewer.base.astronomy.Position;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.math.GL3DMat4d;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVPoint;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLTexture;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class SWHVHEKPluginRenderable extends AbstractRenderable {

    private static final SWHVHEKImagePanelEventPopupController controller = new SWHVHEKImagePanelEventPopupController();

    private static final double LINEWIDTH = 0.5;
    private static final double LINEWIDTH_CACTUS = 1.01;

    private static final double LINEWIDTH_HI = 1;

    private static HashMap<String, GLTexture> iconCacheId = new HashMap<String, GLTexture>();
    private static double ICON_SIZE = 0.1;
    private static double ICON_SIZE_HIGHLIGHTED = 0.16;

    private void bindTexture(GL2 gl, String key, ImageIcon icon) {
        GLTexture tex = iconCacheId.get(key);
        if (tex == null) {
            BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics graph = bi.createGraphics();
            icon.paintIcon(null, graph, 0, 0);
            graph.dispose();

            tex = new GLTexture(gl);
            tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
            tex.copyBufferedImage2D(gl, bi);
            iconCacheId.put(key, tex);
        }

        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
    }

    private void interPolatedDraw(GL2 gl, double mres, double r_start, double r_end, double t_start, double t_end, double phi, double thetaDelta) {
        gl.glBegin(GL2.GL_LINE_STRIP);
        for (int i = 0; i <= mres; i++) {
            double alpha = 1. - i / mres;
            double r = alpha * r_start + (1 - alpha) * (r_end);
            double theta = alpha * t_start + (1 - alpha) * (t_end);

            double x = r * Math.cos(theta) * Math.sin(phi);
            double z = r * Math.cos(theta) * Math.cos(phi);
            double y = r * Math.sin(theta);
            double yrot = y * Math.cos(thetaDelta) + z * Math.sin(thetaDelta);
            double zrot = -y * Math.sin(thetaDelta) + z * Math.cos(thetaDelta);
            double xrot = x;

            gl.glVertex3f((float) xrot, (float) yrot, (float) zrot);
        }
        gl.glEnd();
    }

    private final GL3DVec2d[] texcoords = { new GL3DVec2d(0, 0), new GL3DVec2d(0, 1), new GL3DVec2d(1, 1), new GL3DVec2d(1, 0) };
    private final int texCoordHelpers[][] = { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 1, 0 } };;

    private void drawCactusArc(GL2 gl, JHVEvent evt, Date now) {
        Collection<JHVEventParameter> params = evt.getAllEventParameters().values();
        double principalAngle = 0;
        double principalAngleDegree = 0;
        double angularWidth = 0;
        double angularWidthDegree = 0;

        double distSun = 2.4;
        double speed = 500;

        for (JHVEventParameter param : params) {
            String name = param.getParameterName();
            String value = param.getParameterValue();

            if (name.equals("cme_angularwidth")) {
                angularWidthDegree = Double.parseDouble(value);
                angularWidth = Math.toRadians(angularWidthDegree);
            }
            if (name.equals("event_coord1")) {
                principalAngleDegree = Double.parseDouble(value);
                principalAngle = Math.PI / 2. - Math.toRadians(principalAngleDegree);
            }
            if (name.equals("cme_radiallinvel")) {
                speed = Double.parseDouble(value);
            }
        }
        double factor = (Sun.RadiusMeter / 1000) * (1000);
        double distSunBegin = distSun;
        distSun += speed * (controller.currentTime.getTime() - evt.getStartDate().getTime()) / factor;
        int lineResolution = 2;

        Date date = new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2);
        Position.Latitudinal p = Sun.getEarth(date);

        double thetaDelta = -p.lat;
        double thetaStart = principalAngle - angularWidth / 2.;
        double thetaEnd = principalAngle + angularWidth / 2.;

        double phi = -Math.PI / 2. - p.lon;

        Color color = evt.getEventRelationShip().getRelationshipColor();
        if (color == null) {
            color = evt.getColor();
        }

        gl.glColor3f(0f, 0f, 0f);
        GLHelper.lineWidth(gl, LINEWIDTH_CACTUS * 1.2);

        interPolatedDraw(gl, angularWidthDegree / 4, distSun, distSun, thetaStart, principalAngle, phi, thetaDelta);
        interPolatedDraw(gl, angularWidthDegree / 4, distSun, distSun, principalAngle, thetaEnd, phi, thetaDelta);

        gl.glColor3f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
        GLHelper.lineWidth(gl, LINEWIDTH_CACTUS);

        interPolatedDraw(gl, angularWidthDegree / 4, distSun, distSun, thetaStart, principalAngle, phi, thetaDelta);
        interPolatedDraw(gl, angularWidthDegree / 4, distSun, distSun, principalAngle, thetaEnd, phi, thetaDelta);

        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, thetaStart, thetaStart, phi, thetaDelta);
        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, principalAngle, principalAngle, phi, thetaDelta);
        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, thetaEnd, thetaEnd, phi, thetaDelta);

        double r, theta;
        double x, y, z;
        double xrot, yrot, zrot;
        String type = evt.getJHVEventType().getEventType();
        bindTexture(gl, type, evt.getIcon());
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

        double sz = ICON_SIZE;
        if (evt.isHighlighted()) {
            sz = ICON_SIZE_HIGHLIGHTED;
        }
        gl.glColor3f(1, 1, 1);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_QUADS);
        {
            for (int i = 0; i < texCoordHelpers.length; i++) {
                int[] el = texCoordHelpers[i];
                double deltatheta = sz / distSun * (el[1] * 2 - 1);
                double deltar = sz * (el[0] * 2 - 1);
                r = distSun + deltar;
                theta = principalAngle + deltatheta;
                x = r * Math.cos(theta) * Math.sin(phi);
                z = r * Math.cos(theta) * Math.cos(phi);
                y = r * Math.sin(theta);
                yrot = y * Math.cos(thetaDelta) + z * Math.sin(thetaDelta);
                zrot = -y * Math.sin(thetaDelta) + z * Math.cos(thetaDelta);
                xrot = x;
                gl.glTexCoord2f(el[0], el[1]);
                gl.glVertex3f((float) xrot, (float) yrot, (float) zrot);
            }
        }
        gl.glEnd();
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_CULL_FACE);
    }

    private void drawPolygon(GL2 gl, JHVEvent evt, Date now) {
        HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

        if (!pi.containsKey(JHVCoordinateSystem.JHV)) {
            return;
        }

        JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV);
        List<JHVPoint> points = el.getBoundCC();
        if (points == null || points.size() == 0) {
            points = el.getBoundBox();
            if (points == null || points.size() == 0) {
                return;
            }
        }

        Color color = evt.getEventRelationShip().getRelationshipColor();
        if (color == null) {
            color = evt.getColor();
        }
        gl.glColor3f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);

        GLHelper.lineWidth(gl, evt.isHighlighted() ? LINEWIDTH_HI : LINEWIDTH);

        // draw bounds
        JHVPoint oldBoundaryPoint3d = null;

        for (JHVPoint point : points) {
            int divpoints = 10;

            gl.glBegin(GL2.GL_LINE_STRIP);
            if (oldBoundaryPoint3d != null) {
                for (int j = 0; j <= divpoints; j++) {
                    double alpha = 1. - j / (double) divpoints;
                    double xnew = alpha * oldBoundaryPoint3d.getCoordinate1() + (1 - alpha) * point.getCoordinate1();
                    double ynew = alpha * oldBoundaryPoint3d.getCoordinate2() + (1 - alpha) * point.getCoordinate2();
                    double znew = alpha * oldBoundaryPoint3d.getCoordinate3() + (1 - alpha) * point.getCoordinate3();
                    double r = Math.sqrt(xnew * xnew + ynew * ynew + znew * znew);
                    gl.glVertex3f((float) (xnew / r), (float) -(ynew / r), (float) (znew / r));
                }
            }
            gl.glEnd();

            oldBoundaryPoint3d = point;
        }
    }

    private void drawIcon(GL2 gl, JHVEvent evt, Date now) {
        String type = evt.getJHVEventType().getEventType();
        HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

        if (pi.containsKey(JHVCoordinateSystem.JHV)) {
            JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV);
            if (el.centralPoint() != null) {
                JHVPoint pt = el.centralPoint();
                bindTexture(gl, type, evt.getIcon());
                if (evt.isHighlighted()) {
                    this.drawImage3d(gl, pt.getCoordinate1(), pt.getCoordinate2(), pt.getCoordinate3(), ICON_SIZE_HIGHLIGHTED, ICON_SIZE_HIGHLIGHTED);
                } else {
                    this.drawImage3d(gl, pt.getCoordinate1(), pt.getCoordinate2(), pt.getCoordinate3(), ICON_SIZE, ICON_SIZE);
                }
            }
        }
    }

    private void drawImage3d(GL2 gl, double x, double y, double z, double width, double height) {
        y = -y;

        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

        gl.glColor3f(1, 1, 1);
        double width2 = width / 2.;
        double height2 = height / 2.;

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
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glTexCoord2f(1, 1);
            gl.glVertex3f((float) p3.x, (float) p3.y, (float) p3.z);
            gl.glTexCoord2f(1, 0);
            gl.glVertex3f((float) p2.x, (float) p2.y, (float) p2.z);
            gl.glTexCoord2f(0, 0);
            gl.glVertex3f((float) p1.x, (float) p1.y, (float) p1.z);
            gl.glTexCoord2f(0, 1);
            gl.glVertex3f((float) p0.x, (float) p0.y, (float) p0.z);
        }
        gl.glEnd();
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_CULL_FACE);
    }

    private static final double vpScale = 0.019;
    private TextRenderer textRenderer;
    private Font font;
    private float oldFontSize = -1;

    public void drawText(GL2 gl, JHVEvent evt) {
        int height = Displayer.getGLHeight();
        int width = Displayer.getGLWidth();

        float fontSize = (int) (height * vpScale);
        if (textRenderer == null || fontSize != oldFontSize) {
            oldFontSize = fontSize;
            font = UIGlobals.UIFontRoboto.deriveFont(fontSize);
            if (textRenderer != null) {
                textRenderer.dispose();
            }
            textRenderer = new TextRenderer(font, true, true);
            textRenderer.setUseVertexArrays(true);
            textRenderer.setSmoothing(false);
            textRenderer.setColor(Color.WHITE);
        }

        final int deltaX = 45;
        int deltaY = 45;

        Point pt = SWHVHEKImagePanelEventPopupController.highlightedMousePosition;
        textRenderer.beginRendering(width, height, true);
        Map<String, JHVEventParameter> params = evt.getVisibleEventParameters();
        GL3DVec2d bd = new GL3DVec2d(0, 0);
        int ct = 0;
        for (Entry<String, JHVEventParameter> entry : params.entrySet()) {
            String txt = entry.getValue().getParameterDisplayName() + " : " + entry.getValue().getParameterValue();
            Rectangle2D bound = textRenderer.getBounds(txt);
            if (bd.x < bound.getWidth())
                bd.x = bound.getWidth();
            ct++;
        }
        bd.y = fontSize * 1.1 * (ct);
        // applyCamera
        gl.glColor4f(0.5f, 0.5f, 0.5f, 0.7f);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        int leftMargin = 10;
        int rightMargin = 10;
        int topMargin = 5;
        int bottomMargin = 5;
        float left = pt.x + deltaX - leftMargin;
        float bottom = pt.y + deltaY - fontSize - topMargin;
        float w = (float) bd.x + leftMargin + rightMargin;
        float h = (float) (bd.y + bottomMargin + topMargin);
        {
            gl.glBegin(GL2.GL_QUADS);

            gl.glVertex2f(left, height - bottom);
            gl.glVertex2f(left, height - bottom - h);
            gl.glVertex2f(left + w, height - bottom - h);
            gl.glVertex2f(left + w, height - bottom);
            gl.glEnd();

        }
        gl.glPopMatrix();
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glColor4f(1, 1, 1, 1);

        for (Entry<String, JHVEventParameter> entry : params.entrySet()) {
            String txt = entry.getValue().getParameterDisplayName() + " : " + entry.getValue().getParameterValue();

            textRenderer.draw(txt, pt.x + deltaX, height - pt.y - deltaY);
            deltaY += fontSize * 1.1;
        }
        textRenderer.endRendering();
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
        if (isVisible[vp.getIndex()]) {
            JHVEvent highLightedEvent = null;
            ArrayList<JHVEvent> eventsToDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(controller.currentTime);
            for (JHVEvent evt : eventsToDraw) {
                if (evt.getName().equals("Coronal Mass Ejection")) {
                    drawCactusArc(gl, evt, controller.currentTime);
                } else {
                    drawPolygon(gl, evt, controller.currentTime);

                    gl.glDisable(GL2.GL_DEPTH_TEST);
                    drawIcon(gl, evt, controller.currentTime);
                    gl.glEnable(GL2.GL_DEPTH_TEST);
                }
                if (evt.isHighlighted()) {
                    highLightedEvent = evt;
                }
            }
            if (highLightedEvent != null) {
                drawText(gl, highLightedEvent);
            }
            SWHVHEKSettings.resetCactusColor();
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
        ImageViewerGui.getInputController().removePlugin(controller);
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return "SWEK events";
    }

    @Override
    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);

        if (isVisible) {
            controller.timeChanged(Layers.addTimeListener(controller));
            ImageViewerGui.getInputController().addPlugin(controller);
        } else {
            ImageViewerGui.getInputController().removePlugin(controller);
            Layers.removeTimeListener(controller);
        }
    }

    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
        setVisible(true);
    }

    @Override
    public void dispose(GL2 gl) {
        for (GLTexture el : iconCacheId.values()) {
            el.delete(gl);
        }
        iconCacheId.clear();
    }

    @Override
    public void renderMiniview(GL2 gl, GL3DViewport vp) {
    }

}
