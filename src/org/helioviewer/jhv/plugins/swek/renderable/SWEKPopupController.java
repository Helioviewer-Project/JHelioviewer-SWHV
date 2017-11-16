package org.helioviewer.jhv.plugins.swek.renderable;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.util.ArrayList;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.data.cache.JHVEventCache;
import org.helioviewer.jhv.data.cache.JHVRelatedEvents;
import org.helioviewer.jhv.data.event.JHVEvent;
import org.helioviewer.jhv.data.event.JHVPositionInformation;
import org.helioviewer.jhv.data.gui.info.SWEKEventInformationDialog;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.TimeListener;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLHelper;

import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;

public class SWEKPopupController extends MouseAdapter implements TimeListener {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private final Component component;
    private final Camera camera;

    private static Cursor lastCursor;

    static JHVRelatedEvents mouseOverJHVEvent = null;
    static int mouseOverX;
    static int mouseOverY;
    long currentTime;

    public SWEKPopupController(Component _component) {
        component = _component;
        camera = Displayer.getCamera();
    }

    @Override
    public void timeChanged(long milli) {
        currentTime = milli;
    }

    private Point calcWindowPosition(Point p, int hekWidth, int hekHeight) {
        int compWidth = component.getWidth();
        int compHeight = component.getHeight();
        int compLocX = component.getLocationOnScreen().x;
        int compLocY = component.getLocationOnScreen().y;

        boolean yCoordInMiddle = false;
        int yCoord;
        if (p.y + hekHeight + yOffset < compHeight) {
            yCoord = p.y + compLocY + yOffset;
        } else {
            yCoord = p.y + compLocY - hekHeight - yOffset;
            if (yCoord < compLocY) {
                yCoord = compLocY + compHeight - hekHeight;
                if (yCoord < compLocY) {
                    yCoord = compLocY;
                }
                yCoordInMiddle = true;
            }
        }

        int xCoord;
        if (p.x + hekWidth + xOffset < compWidth) {
            xCoord = p.x + compLocX + xOffset;
        } else {
            xCoord = p.x + compLocX - hekWidth - xOffset;
            if (xCoord < compLocX && !yCoordInMiddle) {
                xCoord = compLocX + compWidth - hekWidth;
            }
        }

        return new Point(xCoord, yCoord);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (mouseOverJHVEvent != null) {
            SWEKEventInformationDialog hekPopUp = new SWEKEventInformationDialog(mouseOverJHVEvent, mouseOverJHVEvent.getClosestTo(currentTime));
            hekPopUp.pack();
            hekPopUp.setLocation(calcWindowPosition(GLHelper.GL2AWTPoint(e.getX(), e.getY()), hekPopUp.getWidth(), hekPopUp.getHeight()));
            hekPopUp.setVisible(true);

            component.setCursor(helpCursor);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseOverJHVEvent = null;
        JHVEventCache.highlight(null);
    }

    private double computeDistSun(JHVEvent evt) {
        double speed = SWEKData.readCMESpeed(evt);
        double distSun = 2.4;
        distSun += speed * (currentTime - evt.start) / Sun.RadiusMeter;
        return distSun;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        ArrayList<JHVRelatedEvents> eventsToDraw = SWEKData.getActiveEvents(currentTime);
        if (eventsToDraw.isEmpty())
            return;

        mouseOverJHVEvent = null;

        mouseOverX = e.getX();
        mouseOverY = e.getY();

        Viewport vp = Displayer.getActiveViewport();
        for (JHVRelatedEvents evtr : eventsToDraw) {
            JHVEvent evt = evtr.getClosestTo(currentTime);
            JHVPositionInformation pi = evt.getPositionInformation();
            if (pi == null)
                continue;

            if (Displayer.mode == Displayer.DisplayMode.Orthographic) {
                Vec3 hitpoint, pt;
                if (evt.isCactus()) {
                    double principalAngle = Math.toRadians(SWEKData.readCMEPrincipalAngleDegree(evt));
                    double distSun = computeDistSun(evt);
                    Quat q = pi.getEarth().orientation;
                    pt = q.rotateInverseVector(new Vec3(distSun * Math.cos(principalAngle), distSun * Math.sin(principalAngle), 0));

                    hitpoint = CameraHelper.getVectorFromPlane(camera, vp, mouseOverX, mouseOverY, Quat.ZERO, true);
                    if (hitpoint != null) {
                        hitpoint = q.rotateInverseVector(hitpoint);
                    }
                } else {
                    hitpoint = getHitPoint(vp, mouseOverX, mouseOverY);
                    pt = pi.centralPoint();
                }

                if (pt != null && hitpoint != null) {
                    double deltaX = Math.abs(hitpoint.x - pt.x);
                    double deltaY = Math.abs(hitpoint.y - pt.y);
                    double deltaZ = Math.abs(hitpoint.z - pt.z);
                    if (deltaX < 0.08 && deltaZ < 0.08 && deltaY < 0.08) {
                        mouseOverJHVEvent = evtr;
                        break;
                    }
                }
            } else {
                Vec2 tf = null;
                Vec2 mousepos = null;
                if (evt.isCactus()) {
                    if (Displayer.mode == Displayer.DisplayMode.LogPolar || Displayer.mode == Displayer.DisplayMode.Polar) {
                        double principalAngle = SWEKData.readCMEPrincipalAngleDegree(evt) - 90;
                        double distSun = computeDistSun(evt);

                        tf = new Vec2(Displayer.mode.scale.getXValueInv(principalAngle), Displayer.mode.scale.getYValueInv(distSun));
                        mousepos = Displayer.mode.scale.mouseToGridInv(mouseOverX, mouseOverY, vp, camera);
                    }
                } else {
                    Vec3 pt = pi.centralPoint();
                    if (pt != null) {
                        pt = camera.getViewpoint().orientation.rotateVector(pt);
                        tf = Displayer.mode.scale.transform(pt);
                    }
                    mousepos = Displayer.mode.scale.mouseToGridInv(mouseOverX, mouseOverY, vp, camera);
                }

                if (tf != null && mousepos != null) {
                    double deltaX = Math.abs(tf.x - mousepos.x);
                    double deltaY = Math.abs(tf.y - mousepos.y);
                    if (deltaX < 0.02 && deltaY < 0.02) {
                        mouseOverJHVEvent = evtr;
                        break;
                    }
                }
            }
        }

        JHVEventCache.highlight(mouseOverJHVEvent);
        if (helpCursor != component.getCursor())
            lastCursor = component.getCursor();

        if (mouseOverJHVEvent != null) {
            component.setCursor(helpCursor);
        } else {
            component.setCursor(lastCursor);
        }
    }

    private Vec3 getHitPoint(Viewport vp, int x, int y) {
        Vec3 hp = CameraHelper.getVectorFromSphere(camera, vp, x, y, camera.getViewpoint().orientation, true);
        if (hp != null)
            hp.y = -hp.y;
        return hp;
    }

}
