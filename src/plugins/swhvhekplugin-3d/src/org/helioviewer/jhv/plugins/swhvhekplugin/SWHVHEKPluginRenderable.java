package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Viewport;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.GLTexture;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class SWHVHEKPluginRenderable extends AbstractRenderable {

    private static final SWHVHEKPopupController controller = new SWHVHEKPopupController();

    private static final double LINEWIDTH = 0.5;
    private static final double LINEWIDTH_CACTUS = 1.01;

    private static final double LINEWIDTH_HI = 1;

    private static HashMap<String, GLTexture> iconCacheId = new HashMap<String, GLTexture>();
    private final static double ICON_SIZE = 0.1;
    private final static double ICON_SIZE_HIGHLIGHTED = 0.16;
    private final static int LEFT_MARGIN_TEXT = 10;
    private final static int RIGHT_MARGIN_TEXT = 10;
    private final static int TOP_MARGIN_TEXT = 5;
    private final static int BOTTOM_MARGIN_TEXT = 5;
    private final static int MOUSE_OFFSET_X = 25;
    private final static int MOUSE_OFFSET_Y = 25;

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

    private void interPolatedDraw(GL2 gl, int mres, double r_start, double r_end, double t_start, double t_end, Quat q) {
        gl.glBegin(GL2.GL_LINE_STRIP);
        {
            for (int i = 0; i <= mres; i++) {
                double alpha = 1. - i / (double) mres;
                double r = alpha * r_start + (1 - alpha) * (r_end);
                double theta = alpha * t_start + (1 - alpha) * (t_end);
                Vec3 res = q.rotateInverseVector(new Vec3(r * Math.cos(theta), r * Math.sin(theta), 0));
                gl.glVertex3f((float) res.x, (float) res.y, (float) res.z);
            }
        }
        gl.glEnd();
    }

    private static final int texCoordHelpers[][] = { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 } };

    private void drawCactusArc(GL2 gl, JHVEvent evt, Date timestamp) {
        Map<String, JHVEventParameter> params = evt.getAllEventParameters();
        double angularWidthDegree = SWHVHEKData.readCMEAngularWidthDegree(params);
        double angularWidth = Math.toRadians(angularWidthDegree);
        double principalAngleDegree = SWHVHEKData.readCMEPrincipalAngleDegree(params);
        double principalAngle = Math.toRadians(principalAngleDegree);
        double speed = SWHVHEKData.readCMESpeed(params);
        double factor = Sun.RadiusMeter;
        double distSunBegin = 2.4;
        double distSun = distSunBegin + speed * (timestamp.getTime() - evt.getStartDate().getTime()) / factor;
        int lineResolution = 2;

        Position.Latitudinal p = Sun.getEarth((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2);
        Quat q = new Quat(p.lat, p.lon);

        double thetaStart = principalAngle - angularWidth / 2.;
        double thetaEnd = principalAngle + angularWidth / 2.;

        Color color = evt.getEventRelationShip().getRelationshipColor();
        if (color == null) {
            color = evt.getColor();
        }

        gl.glColor3f(0f, 0f, 0f);
        GLHelper.lineWidth(gl, LINEWIDTH_CACTUS * 1.2);
        int angularResolution = (int) (angularWidthDegree / 4);
        interPolatedDraw(gl, angularResolution, distSun, distSun, thetaStart, principalAngle, q);
        interPolatedDraw(gl, angularResolution, distSun, distSun, principalAngle, thetaEnd, q);

        gl.glColor3f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
        GLHelper.lineWidth(gl, LINEWIDTH_CACTUS);

        interPolatedDraw(gl, angularResolution, distSun, distSun, thetaStart, principalAngle, q);
        interPolatedDraw(gl, angularResolution, distSun, distSun, principalAngle, thetaEnd, q);

        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, thetaStart, thetaStart, q);
        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, principalAngle, principalAngle, q);
        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, thetaEnd, thetaEnd, q);

        String type = evt.getJHVEventType().getEventType();
        bindTexture(gl, type, evt.getIcon());
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

        double sz = ICON_SIZE;
        if (evt.isHighlighted()) {
            sz = ICON_SIZE_HIGHLIGHTED;
        }

        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_QUADS);
        {
            for (int i = 0; i < texCoordHelpers.length; i++) {
                int[] el = texCoordHelpers[i];
                double deltatheta = sz / distSun * (el[1] * 2 - 1);
                double deltar = sz * (el[0] * 2 - 1);
                double r = distSun + deltar;
                double theta = principalAngle + deltatheta;
                Vec3 res = q.rotateInverseVector(new Vec3(r * Math.cos(theta), r * Math.sin(theta), 0));
                gl.glTexCoord2f(el[0], el[1]);
                gl.glVertex3f((float) res.x, (float) res.y, (float) res.z);
            }
        }
        gl.glEnd();
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    private void drawPolygon(GL2 gl, JHVEvent evt) {
        Map<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();
        if (!pi.containsKey(JHVCoordinateSystem.JHV)) {
            return;
        }

        JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV);
        List<Vec3> points = el.getBoundCC();
        if (points == null || points.isEmpty()) {
            points = el.getBoundBox();
            if (points == null || points.isEmpty()) {
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
        Vec3 oldBoundaryPoint3d = null;
        for (Vec3 point : points) {
            int divpoints = 10;

            gl.glBegin(GL2.GL_LINE_STRIP);
            if (oldBoundaryPoint3d != null) {
                for (int j = 0; j <= divpoints; j++) {
                    double alpha = 1. - j / (double) divpoints;
                    double xnew = alpha * oldBoundaryPoint3d.x + (1 - alpha) * point.x;
                    double ynew = alpha * oldBoundaryPoint3d.y + (1 - alpha) * point.y;
                    double znew = alpha * oldBoundaryPoint3d.z + (1 - alpha) * point.z;
                    double r = Math.sqrt(xnew * xnew + ynew * ynew + znew * znew);
                    gl.glVertex3f((float) (xnew / r), (float) -(ynew / r), (float) (znew / r));
                }
            }
            gl.glEnd();

            oldBoundaryPoint3d = point;
        }
    }

    private void drawIcon(GL2 gl, JHVEvent evt) {
        Map<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();
        if (pi.containsKey(JHVCoordinateSystem.JHV)) {
            JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV);
            if (el.centralPoint() != null) {
                Vec3 pt = el.centralPoint();
                String type = evt.getJHVEventType().getEventType();
                bindTexture(gl, type, evt.getIcon());
                if (evt.isHighlighted()) {
                    drawImage3d(gl, pt.x, pt.y, pt.z, ICON_SIZE_HIGHLIGHTED, ICON_SIZE_HIGHLIGHTED);
                } else {
                    drawImage3d(gl, pt.x, pt.y, pt.z, ICON_SIZE, ICON_SIZE);
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

        Vec3 targetDir = new Vec3(x, y, z);

        Mat4 r = Mat4.rotation(Math.atan2(x, z), Vec3.YAxis);
        r.rotate(-Math.asin(y / targetDir.length()), Vec3.XAxis);

        Vec3 p0 = new Vec3(-width2, -height2, 0);
        Vec3 p1 = new Vec3(-width2, height2, 0);
        Vec3 p2 = new Vec3(width2, height2, 0);
        Vec3 p3 = new Vec3(width2, -height2, 0);

        p0 = r.multiply(p0);
        p1 = r.multiply(p1);
        p2 = r.multiply(p2);
        p3 = r.multiply(p3);
        p0.add(targetDir);
        p1.add(targetDir);
        p2.add(targetDir);
        p3.add(targetDir);

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
    }

    private static final double vpScale = 0.019;

    private void drawText(GL2 gl, Viewport vp, JHVEvent evt, Point pt) {
        TextRenderer renderer = GLText.getRenderer((int) (vp.height * vpScale));
        float fontSize = renderer.getFont().getSize2D();

        Map<String, JHVEventParameter> params = evt.getVisibleEventParameters();

        double boundW = 0;
        int ct = 0;
        for (Map.Entry<String, JHVEventParameter> entry : params.entrySet()) {
            String txt = entry.getValue().getParameterDisplayName() + " : " + entry.getValue().getParameterValue();
            Rectangle2D bound = renderer.getBounds(txt);
            if (boundW < bound.getWidth())
                boundW = bound.getWidth();
            ct++;
        }

        Point textInit = new Point(pt.x, pt.y);
        float w = (float) (boundW + LEFT_MARGIN_TEXT + RIGHT_MARGIN_TEXT);
        float h = (float) (fontSize * 1.1 * ct + BOTTOM_MARGIN_TEXT + TOP_MARGIN_TEXT);

        // Correct if out of view
        if (w + pt.x + MOUSE_OFFSET_X - LEFT_MARGIN_TEXT > vp.width) {
            textInit.x -= (w + pt.x + MOUSE_OFFSET_X - LEFT_MARGIN_TEXT - vp.width);
        }
        if (h + pt.y + MOUSE_OFFSET_Y - fontSize - TOP_MARGIN_TEXT > vp.height) {
            textInit.y -= (h + pt.y + MOUSE_OFFSET_Y - fontSize - TOP_MARGIN_TEXT - vp.height);
        }
        float left = textInit.x + MOUSE_OFFSET_X - LEFT_MARGIN_TEXT;
        float bottom = textInit.y + MOUSE_OFFSET_Y - fontSize - TOP_MARGIN_TEXT;

        renderer.beginRendering(vp.width, vp.height, true);

        gl.glColor4f(0.33f, 0.33f, 0.33f, 0.9f);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        {
            gl.glBegin(GL2.GL_QUADS);
            gl.glVertex2f(left, vp.height - bottom);
            gl.glVertex2f(left, vp.height - bottom - h);
            gl.glVertex2f(left + w, vp.height - bottom - h);
            gl.glVertex2f(left + w, vp.height - bottom);
            gl.glEnd();

        }
        gl.glPopMatrix();
        gl.glEnable(GL2.GL_TEXTURE_2D);

        gl.glColor3f(1, 1, 1);
        int deltaY = MOUSE_OFFSET_Y;
        for (Map.Entry<String, JHVEventParameter> entry : params.entrySet()) {
            String txt = entry.getValue().getParameterDisplayName() + " : " + entry.getValue().getParameterValue();
            renderer.draw(txt, textInit.x + MOUSE_OFFSET_X, vp.height - textInit.y - deltaY);
            deltaY += fontSize * 1.1;
        }
        renderer.endRendering();
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (isVisible[vp.index]) {
            List<JHVEvent> eventsToDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(controller.currentTime);
            for (JHVEvent evt : eventsToDraw) {
                if (evt.getName().equals("Coronal Mass Ejection")) {
                    drawCactusArc(gl, evt, controller.currentTime);
                } else {
                    drawPolygon(gl, evt);

                    gl.glDisable(GL2.GL_DEPTH_TEST);
                    drawIcon(gl, evt);
                    gl.glEnable(GL2.GL_DEPTH_TEST);
                }
            }
            SWHVHEKSettings.resetCactusColor();
        }
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        if (isVisible[vp.index]) {
            if (SWHVHEKPopupController.mouseOverJHVEvent != null) {
                drawText(gl, vp, SWHVHEKPopupController.mouseOverJHVEvent, SWHVHEKPopupController.mouseOverPosition);
            }
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
        return "SWEK Events";
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

}
