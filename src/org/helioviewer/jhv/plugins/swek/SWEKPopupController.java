package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.JHVEventCache;
import org.helioviewer.jhv.events.JHVPositionInformation;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.events.gui.info.SWEKEventInformationDialog;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.TimeListener;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLHelper;

class SWEKPopupController extends MouseAdapter implements TimeListener {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private final Component component;
    private final Camera camera;

    private Cursor lastCursor;

    static JHVRelatedEvents mouseOverJHVEvent = null;
    static int mouseOverX;
    static int mouseOverY;
    long currentTime;

    SWEKPopupController(Component _component) {
        component = _component;
        camera = Display.getCamera();
    }

    @Override
    public void timeChanged(long milli) {
        currentTime = milli;
    }

    private Point calcWindowPosition(Point p, int hekWidth, int hekHeight) {
        int compWidth = component.getWidth();
        int compHeight = component.getHeight();
        Point compLoc = component.getLocationOnScreen();
        int compLocX = compLoc.x;
        int compLocY = compLoc.y;

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
        List<JHVRelatedEvents> activeEvents = SWEKData.getActiveEvents(currentTime);
        if (activeEvents.isEmpty())
            return;

        mouseOverJHVEvent = null;

        mouseOverX = e.getX();
        mouseOverY = e.getY();

        Viewport vp = Display.getActiveViewport();
        for (JHVRelatedEvents evtr : activeEvents) {
            JHVEvent evt = evtr.getClosestTo(currentTime);
            JHVPositionInformation pi = evt.getPositionInformation();
            if (pi == null)
                continue;

            if (Display.mode == Display.DisplayMode.Orthographic) {
                Vec3 hitpoint, pt;
                if (evt.isCactus()) {
                    double principalAngle = Math.toRadians(SWEKData.readCMEPrincipalAngleDegree(evt));
                    double distSun = computeDistSun(evt);
                    Quat q = pi.getEarth().toQuat();
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
                if ((Display.mode == Display.DisplayMode.LogPolar || Display.mode == Display.DisplayMode.Polar) && evt.isCactus()) {
                    double principalAngle = SWEKData.readCMEPrincipalAngleDegree(evt) - 90;
                    double distSun = computeDistSun(evt);
                    tf = new Vec2(Display.mode.scale.getXValueInv(principalAngle), Display.mode.scale.getYValueInv(distSun));
                } else {
                    Vec3 pt = pi.centralPoint();
                    if (pt != null) {
                        Position viewpoint = camera.getViewpoint();
                        pt = viewpoint.toQuat().rotateVector(pt);
                        tf = Display.mode.xform.transform(viewpoint, pt, Display.mode.scale);
                    }
                }

                if (tf != null) {
                    Vec2 mousepos = Display.mode.scale.mouseToGridInv(mouseOverX, mouseOverY, vp, camera);
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
        Cursor cursor = component.getCursor();
        if (helpCursor != cursor)
            lastCursor = cursor;

        if (mouseOverJHVEvent != null) {
            component.setCursor(helpCursor);
        } else {
            component.setCursor(lastCursor);
        }
    }

    @Nullable
    private Vec3 getHitPoint(Viewport vp, int x, int y) {
        Vec3 hp = CameraHelper.getVectorFromSphere(camera, vp, x, y, camera.getViewpoint().toQuat(), true);
        if (hp != null)
            hp.y = -hp.y;
        return hp;
    }

}
