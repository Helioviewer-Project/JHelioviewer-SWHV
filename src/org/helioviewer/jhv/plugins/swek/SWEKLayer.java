package org.helioviewer.jhv.plugins.swek;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.data.cache.JHVRelatedEvents;
import org.helioviewer.jhv.data.event.JHVEvent;
import org.helioviewer.jhv.data.event.JHVEventParameter;
import org.helioviewer.jhv.data.event.JHVPositionInformation;
import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.Mat4;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.GLTexture;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class SWEKLayer extends AbstractRenderable {

    private static final SWEKPopupController controller = new SWEKPopupController(ImageViewerGui.getGLComponent());
    private final JPanel optionsPanel;

    private static final int DIVPOINTS = 10;
    private static final float LINEWIDTH = 1;
    private static final float LINEWIDTH_HIGHLIGHT = 2;
    private static final float LINEWIDTH_CACTUS = 2.02f;

    private static final HashMap<String, GLTexture> iconCacheId = new HashMap<>();
    private static final double ICON_SIZE = 0.1;
    private static final double ICON_SIZE_HIGHLIGHTED = 0.16;

    private boolean icons = true;

    public SWEKLayer(JSONObject jo) {
        if (jo != null)
            icons = jo.optBoolean("icons", icons);
        else
            setEnabled(true);

        optionsPanel = optionsPanel();
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("icons", icons);
    }

    private static void bindTexture(GL2 gl, SWEKGroup group) {
        String key = group.getName();
        GLTexture tex = iconCacheId.get(key);
        if (tex == null) {
            ImageIcon icon = group.getIcon();
            BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics graph = bi.createGraphics();
            icon.paintIcon(null, graph, 0, 0);
            graph.dispose();

            tex = new GLTexture(gl);
            tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);

            GLTexture.copyBufferedImage2D(gl, bi);
            iconCacheId.put(key, tex);
        }
        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
    }

    private static void interPolatedDraw(GL2 gl, int mres, double r_start, double r_end, double t_start, double t_end, Quat q) {
        gl.glBegin(GL2.GL_LINE_STRIP);
        {
            Vec3 v = new Vec3();
            for (int i = 0; i <= mres; i++) {
                double alpha = 1. - i / (double) mres;
                double r = alpha * r_start + (1 - alpha) * (r_end);
                double theta = alpha * t_start + (1 - alpha) * (t_end);

                v.x = r * Math.cos(theta);
                v.y = r * Math.sin(theta);
                Vec3 res = q.rotateInverseVector(v);

                gl.glVertex3f((float) res.x, (float) res.y, (float) res.z);
            }
        }
        gl.glEnd();
    }

    private static final int texCoordHelpers[][] = { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 } };

    private void drawCactusArc(GL2 gl, JHVRelatedEvents evtr, JHVEvent evt, long timestamp) {
        double angularWidthDegree = SWEKData.readCMEAngularWidthDegree(evt);
        double angularWidth = Math.toRadians(angularWidthDegree);
        double principalAngleDegree = SWEKData.readCMEPrincipalAngleDegree(evt);
        double principalAngle = Math.toRadians(principalAngleDegree);
        double speed = SWEKData.readCMESpeed(evt);
        double factor = Sun.RadiusMeter;
        double distSunBegin = 2.4;
        double distSun = distSunBegin + speed * (timestamp - evt.start) / factor;
        int lineResolution = 2;
        int angularResolution = (int) (angularWidthDegree / 4);

        Quat q = evt.getPositionInformation().getEarth().orientation;
        Color color = evtr.getColor();

        gl.glColor3f(0, 0, 0);
        gl.glLineWidth(LINEWIDTH_CACTUS * 1.2f);

        double thetaStart = principalAngle - angularWidth / 2.;
        interPolatedDraw(gl, angularResolution, distSun, distSun, thetaStart, principalAngle, q);
        double thetaEnd = principalAngle + angularWidth / 2.;
        interPolatedDraw(gl, angularResolution, distSun, distSun, principalAngle, thetaEnd, q);

        gl.glColor3f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
        gl.glLineWidth(LINEWIDTH_CACTUS);

        interPolatedDraw(gl, angularResolution, distSun, distSun, thetaStart, principalAngle, q);
        interPolatedDraw(gl, angularResolution, distSun, distSun, principalAngle, thetaEnd, q);

        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, thetaStart, thetaStart, q);
        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, principalAngle, principalAngle, q);
        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, thetaEnd, thetaEnd, q);

        if (icons) {
            bindTexture(gl, evtr.getSupplier().getGroup());

            double sz = ICON_SIZE;
            if (evtr.isHighlighted()) {
                sz = ICON_SIZE_HIGHLIGHTED;
            }

            gl.glEnable(GL2.GL_TEXTURE_2D);
            gl.glBegin(GL2.GL_QUADS);
            {
                Vec3 v = new Vec3();
                for (int[] el : texCoordHelpers) {
                    double deltatheta = sz / distSun * (el[1] * 2 - 1);
                    double deltar = sz * (el[0] * 2 - 1);
                    double r = distSun + deltar;
                    double theta = principalAngle + deltatheta;

                    v.x = r * Math.cos(theta);
                    v.y = r * Math.sin(theta);
                    Vec3 res = q.rotateInverseVector(v);

                    gl.glTexCoord2f(el[0], el[1]);
                    gl.glVertex3f((float) res.x, (float) res.y, (float) res.z);
                }
            }
            gl.glEnd();
            gl.glDisable(GL2.GL_TEXTURE_2D);
        }
    }

    private static void drawPolygon(Camera camera, Viewport vp, GL2 gl, JHVRelatedEvents evtr, JHVEvent evt) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        float[] points = pi.getBoundBox();
        if (points.length == 0) {
            return;
        }

        Color color = evtr.getColor();
        gl.glColor3f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);

        gl.glLineWidth(evtr.isHighlighted() ? LINEWIDTH_HIGHLIGHT : LINEWIDTH);

        // draw bounds
        Vec3 pt = new Vec3();
        float[] oldBoundaryPoint3d = new float[0];
        Vec2 previous = null;
        int plen = points.length / 3;
        for (int i = 0; i < plen; i++) {
            gl.glBegin(GL2.GL_LINE_STRIP);
            if (oldBoundaryPoint3d.length != 0) {
                for (int j = 0; j <= DIVPOINTS; j++) {
                    double alpha = 1. - j / (double) DIVPOINTS;
                    double xnew = alpha * oldBoundaryPoint3d[0] + (1 - alpha) * points[3 * i];
                    double ynew = alpha * oldBoundaryPoint3d[1] + (1 - alpha) * points[3 * i + 1];
                    double znew = alpha * oldBoundaryPoint3d[2] + (1 - alpha) * points[3 * i + 2];
                    double r = Math.sqrt(xnew * xnew + ynew * ynew + znew * znew);

                    if (Displayer.mode == Displayer.DisplayMode.Orthographic) {
                        gl.glVertex3f((float) (xnew / r), (float) -(ynew / r), (float) (znew / r));
                    } else {
                        pt.x = xnew / r;
                        pt.y = ynew / r;
                        pt.z = znew / r;
                        previous = GLHelper.drawVertex(camera, vp, gl, pt, previous);
                    }
                }
            }
            gl.glEnd();
            oldBoundaryPoint3d = new float[] { points[3 * i], points[3 * i + 1], points[3 * i + 2] };
        }
    }

    private static void drawIcon(GL2 gl, JHVRelatedEvents evtr, JHVEvent evt) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        Vec3 pt = pi.centralPoint();
        if (pt != null) {
            bindTexture(gl, evtr.getSupplier().getGroup());
            Color color = evtr.getColor();
            float alpha = 0.6f;
            gl.glColor4f(color.getRed() / 255f * alpha, color.getGreen() / 255f * alpha, color.getBlue() / 255f * alpha, alpha);
            if (evtr.isHighlighted()) {
                drawImage3d(gl, pt.x, pt.y, pt.z, ICON_SIZE_HIGHLIGHTED, ICON_SIZE_HIGHLIGHTED);
            } else {
                drawImage3d(gl, pt.x, pt.y, pt.z, ICON_SIZE, ICON_SIZE);
            }
        }
    }

    private static void drawImageScale(GL2 gl, double theta, double r, double width, double height) {
        double width2 = width / 4.;
        double height2 = height / 4.;

        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glTexCoord2f(1, 1);
            gl.glVertex2f((float) (theta + width2), (float) (r - height2));
            gl.glTexCoord2f(1, 0);
            gl.glVertex2f((float) (theta + width2), (float) (r + height2));
            gl.glTexCoord2f(0, 0);
            gl.glVertex2f((float) (theta - width2), (float) (r + height2));
            gl.glTexCoord2f(0, 1);
            gl.glVertex2f((float) (theta - width2), (float) (r - height2));
        }
        gl.glEnd();
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    private static void drawIconScale(GL2 gl, JHVRelatedEvents evtr, JHVEvent evt, GridScale scale, Camera camera, Viewport vp) {
        JHVPositionInformation pi = evt.getPositionInformation();
        if (pi == null)
            return;

        Vec3 pt = pi.centralPoint();
        if (pt != null) {
            pt = camera.getViewpoint().orientation.rotateVector(pt);
            Vec2 tf = scale.transform(pt);
            bindTexture(gl, evtr.getSupplier().getGroup());
            if (evtr.isHighlighted()) {
                drawImageScale(gl, tf.x * vp.aspect, tf.y, ICON_SIZE_HIGHLIGHTED, ICON_SIZE_HIGHLIGHTED);
            } else {
                drawImageScale(gl, tf.x * vp.aspect, tf.y, ICON_SIZE, ICON_SIZE);
            }
        }
    }

    private void drawCactusArcScale(GL2 gl, JHVRelatedEvents evtr, JHVEvent evt, long timestamp, GridScale scale, Viewport vp) {
        double angularWidthDegree = SWEKData.readCMEAngularWidthDegree(evt);
        double principalAngleDegree = SWEKData.readCMEPrincipalAngleDegree(evt) - 90;
        double speed = SWEKData.readCMESpeed(evt);
        double factor = Sun.RadiusMeter;
        double distSunBegin = 2.4;
        double distSun = distSunBegin + speed * (timestamp - evt.start) / factor;

        double thetaStart = MathUtils.mapTo0To360(principalAngleDegree - angularWidthDegree / 2.);
        double thetaEnd = MathUtils.mapTo0To360(principalAngleDegree + angularWidthDegree / 2.);

        Color color = evtr.getColor();

        gl.glColor3f(0, 0, 0);
        gl.glLineWidth(LINEWIDTH_CACTUS * 1.2f);
        gl.glBegin(GL2.GL_LINES);
        {
            gl.glVertex2f((float) (scale.getXValueInv(thetaStart) * vp.aspect), (float) scale.getYValueInv(distSun));
            if (thetaEnd < thetaStart) {
                gl.glVertex2f((float) (scale.getXValueInv(360) * vp.aspect), (float) scale.getYValueInv(distSun));
                gl.glVertex2f((float) (scale.getXValueInv(0) * vp.aspect), (float) scale.getYValueInv(distSun));
            }
            gl.glVertex2f((float) (scale.getXValueInv(thetaEnd) * vp.aspect), (float) scale.getYValueInv(distSun));
        }
        gl.glEnd();

        gl.glLineWidth(LINEWIDTH_CACTUS);
        gl.glColor3f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
        gl.glBegin(GL2.GL_LINES);
        {
            gl.glVertex2f((float) (scale.getXValueInv(thetaStart) * vp.aspect), (float) scale.getYValueInv(distSun));
            if (thetaEnd < thetaStart) {
                gl.glVertex2f((float) (scale.getXValueInv(360) * vp.aspect), (float) scale.getYValueInv(distSun));
                gl.glVertex2f((float) (scale.getXValueInv(0) * vp.aspect), (float) scale.getYValueInv(distSun));
            }
            gl.glVertex2f((float) (scale.getXValueInv(thetaEnd) * vp.aspect), (float) scale.getYValueInv(distSun));
            gl.glVertex2f((float) (scale.getXValueInv(thetaStart) * vp.aspect), (float) scale.getYValueInv(distSunBegin));
            gl.glVertex2f((float) (scale.getXValueInv(thetaStart) * vp.aspect), (float) scale.getYValueInv(distSun + 0.05));
            gl.glVertex2f((float) (scale.getXValueInv(thetaEnd) * vp.aspect), (float) scale.getYValueInv(distSunBegin));
            gl.glVertex2f((float) (scale.getXValueInv(thetaEnd) * vp.aspect), (float) scale.getYValueInv(distSun + 0.05));

            gl.glVertex2f((float) (scale.getXValueInv(principalAngleDegree) * vp.aspect), (float) scale.getYValueInv(distSunBegin));
            gl.glVertex2f((float) (scale.getXValueInv(principalAngleDegree) * vp.aspect), (float) scale.getYValueInv(distSun + 0.05));
        }
        gl.glEnd();

        if (icons) {
            bindTexture(gl, evtr.getSupplier().getGroup());
            if (evtr.isHighlighted()) {
                drawImageScale(gl, scale.getXValueInv(principalAngleDegree) * vp.aspect, scale.getYValueInv(distSun), ICON_SIZE_HIGHLIGHTED, ICON_SIZE_HIGHLIGHTED);
            } else {
                drawImageScale(gl, scale.getXValueInv(principalAngleDegree) * vp.aspect, scale.getYValueInv(distSun), ICON_SIZE, ICON_SIZE);
            }
        }
    }

    private static void drawImage3d(GL2 gl, double x, double y, double z, double width, double height) {
        y = -y;

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

    private static final int MOUSE_OFFSET_X = 25;
    private static final int MOUSE_OFFSET_Y = 25;

    private static void drawText(GL2 gl, Viewport vp, JHVRelatedEvents mouseOverJHVEvent, int x, int y) {
        ArrayList<String> txts = new ArrayList<>();
        JHVEvent evt = mouseOverJHVEvent.getClosestTo(controller.currentTime);
        JHVEventParameter[] params = evt.getSimpleVisibleEventParameters();
        for (JHVEventParameter p : params) {
            String name = p.getParameterName();
            if (name != "event_description" && name != "event_title") { // interned
                txts.add(p.getParameterDisplayName() + " : " + p.getSimpleDisplayParameterValue());
            }
        }
        GLText.drawText(gl, vp, txts, x + MOUSE_OFFSET_X, y + MOUSE_OFFSET_Y);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (isVisible[vp.idx]) {
            List<JHVRelatedEvents> eventsToDraw = SWEKData.getActiveEvents(controller.currentTime);
            for (JHVRelatedEvents evtr : eventsToDraw) {
                JHVEvent evt = evtr.getClosestTo(controller.currentTime);
                if (evt.isCactus()) {
                    drawCactusArc(gl, evtr, evt, controller.currentTime);
                } else {
                    drawPolygon(camera, vp, gl, evtr, evt);

                    if (icons) {
                        gl.glDisable(GL2.GL_DEPTH_TEST);
                        drawIcon(gl, evtr, evt);
                        gl.glEnable(GL2.GL_DEPTH_TEST);
                    }
                }
            }
        }
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        if (isVisible[vp.idx]) {
            List<JHVRelatedEvents> eventsToDraw = SWEKData.getActiveEvents(controller.currentTime);
            for (JHVRelatedEvents evtr : eventsToDraw) {
                JHVEvent evt = evtr.getClosestTo(controller.currentTime);
                if (evt.isCactus() && (Displayer.mode == Displayer.DisplayMode.LogPolar || Displayer.mode == Displayer.DisplayMode.Polar)) {
                    drawCactusArcScale(gl, evtr, evt, controller.currentTime, Displayer.mode.scale, vp);
                } else {
                    drawPolygon(camera, vp, gl, evtr, evt);

                    if (icons) {
                        gl.glDisable(GL2.GL_DEPTH_TEST);
                        drawIconScale(gl, evtr, evt, Displayer.mode.scale, camera, vp);
                        gl.glEnable(GL2.GL_DEPTH_TEST);
                    }
                }
            }
        }
    }

    @Override
    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        if (SWEKPopupController.mouseOverJHVEvent != null) {
            drawText(gl, vp, SWEKPopupController.mouseOverJHVEvent, SWEKPopupController.mouseOverX, SWEKPopupController.mouseOverY);
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
        ImageViewerGui.getInputController().removePlugin(controller);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "SWEK Events";
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);

        if (_enabled) {
            Layers.addTimeListener(controller);
            controller.timeChanged(Layers.getLastUpdatedTimestamp().milli);
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
    }

    @Override
    public void dispose(GL2 gl) {
        ImageViewerGui.getInputController().removePlugin(controller);
        Layers.removeTimeListener(controller);

        for (GLTexture el : iconCacheId.values())
            el.delete(gl);
        iconCacheId.clear();
    }

    private JPanel optionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JCheckBox check = new JCheckBox("Icons", icons);
        check.setHorizontalTextPosition(SwingConstants.LEFT);
        check.addActionListener(e -> {
            icons = !icons;
            Displayer.display();
        });

        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.CENTER;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        panel.add(check, c0);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
