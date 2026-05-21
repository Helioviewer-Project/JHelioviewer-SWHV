package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.JHVEventCache;
import org.helioviewer.jhv.events.JHVPositionInformation;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.events.info.SWEKEventInformationDialog;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.input.InputController;
import org.helioviewer.jhv.input.InputPointerListener;
import org.helioviewer.jhv.input.InputPointerMotionListener;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.swing.AwtInputAdapter;

class SWEKPopupController implements InputPointerListener, InputPointerMotionListener {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private final SWEKContext swekContext;
    private final Camera camera;

    private Cursor lastCursor;

    SWEKPopupController(SWEKContext _swekContext) {
        swekContext = _swekContext;
        camera = Display.getCamera();
    }

    void install() {
        InputController.addListener(this);
    }

    void uninstall() {
        InputController.removeListener(this);
        resetHover();
    }

    private static Component component() {
        return JHVFrame.getRenderComponent();
    }

    private Point calcWindowPosition(Component component, Point p, int hekWidth, int hekHeight) {
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
    public void mouseClicked(PointerEvent e) {
        JHVRelatedEvents mouseOverJHVEvent = swekContext.mouseOverJHVEvent();
        if (swekContext.isEnabled() && mouseOverJHVEvent != null) {
            Component canvas = component();
            SWEKEventInformationDialog hekPopUp = new SWEKEventInformationDialog(mouseOverJHVEvent, mouseOverJHVEvent.getClosestTo(swekContext.mouseOverTime()));
            hekPopUp.pack();
            hekPopUp.setLocation(calcWindowPosition(canvas, AwtInputAdapter.toAwtPoint(e), hekPopUp.getWidth(), hekPopUp.getHeight()));
            hekPopUp.setVisible(true);

            canvas.setCursor(helpCursor);
        }
    }

    @Override
    public void mouseExited(PointerEvent e) {
        resetHover();
    }

    void resetHover() {
        swekContext.clearHover();
        JHVEventCache.highlight(null);
        component().setCursor(lastCursor != null ? lastCursor : Cursor.getDefaultCursor());
    }

    private static double computeDistSun(JHVEvent evt, long currentTime) {
        double speed = SWEKData.readCMESpeed(evt);
        double distSun = 2.4;
        distSun += speed * (currentTime - evt.start) / Sun.RadiusMeter;
        return distSun;
    }

    @Override
    public void mouseMoved(PointerEvent e) {
        if (!swekContext.isEnabled()) {
            resetHover();
            return;
        }

        Position viewpoint = GLRenderer.getDisplayedViewpoint();
        double displayWidth = GLRenderer.getDisplayView().cameraWidth(Display.getActiveViewport());
        long currentTime = viewpoint.time.milli;
        List<JHVRelatedEvents> activeEvents = SWEKData.getActiveEvents(currentTime);
        if (activeEvents.isEmpty()) {
            resetHover();
            return;
        }

        JHVRelatedEvents mouseOverJHVEvent = null;

        int mouseOverX = e.x();
        int mouseOverY = e.y();

        Viewport vp = Display.getActiveViewport();
        displayWidth = GLRenderer.getDisplayView().cameraWidth(vp);
        ProjectionMode mode = Display.mode;
        for (JHVRelatedEvents evtr : activeEvents) {
            JHVEvent evt = evtr.getClosestTo(currentTime);
            JHVPositionInformation pi = evt.getPositionInformation();
            if (pi == null)
                continue;

            if (mode == ProjectionMode.Orthographic) {
                Vec3 hitpoint, pt;
                if (evt.isCactus()) {
                    double principalAngle = Math.toRadians(SWEKData.readCMEPrincipalAngleDegree(evt));
                    double distSun = computeDistSun(evt, currentTime);
                    Quat q = pi.getEarth().toQuat();
                    pt = q.rotateInverseVector(PolarBasis.vec3(distSun, principalAngle));

                    hitpoint = CameraHelper.unprojectToOutputPlane(camera, vp, displayWidth, mouseOverX, mouseOverY, Quat.ZERO);
                    if (hitpoint != null) {
                        hitpoint = q.rotateInverseVector(hitpoint);
                    }
                } else {
                    hitpoint = CameraHelper.unprojectToOutputSphere(camera, vp, displayWidth, mouseOverX, mouseOverY, viewpoint.toQuat());
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
                if ((mode == ProjectionMode.Polar || mode == ProjectionMode.LogPolar) && evt.isCactus()) {
                    double principalAngle = SWEKData.readCMEPrincipalAngleDegree(evt);
                    double distSun = computeDistSun(evt, currentTime);
                    tf = new Vec2(mode.scale.getXValueInv(principalAngle) * vp.aspect, mode.scale.getYValueInv(distSun));
                } else {
                    Vec3 pt = pi.centralPoint();
                    if (pt != null) {
                        tf = mode.projectToScreen(GLRenderer.getDisplayView(), vp, Display.gridType, pt);
                    }
                }

                if (tf != null) {
                    Vec2 mousepos = mode.mouseToScreen(camera, GLRenderer.getDisplayView(), vp, Display.gridType, mouseOverX, mouseOverY);
                    double deltaX = Math.abs(tf.x - mousepos.x);
                    double deltaY = Math.abs(tf.y - mousepos.y);
                    if (deltaX < 0.02 && deltaY < 0.02) {
                        mouseOverJHVEvent = evtr;
                        break;
                    }
                }
            }
        }

        swekContext.setMouseOver(mouseOverX, mouseOverY, currentTime, mouseOverJHVEvent);
        Component canvas = component();
        JHVEventCache.highlight(mouseOverJHVEvent);
        Cursor cursor = canvas.getCursor();
        if (helpCursor != cursor)
            lastCursor = cursor;

        if (mouseOverJHVEvent != null) {
            canvas.setCursor(helpCursor);
        } else {
            canvas.setCursor(lastCursor != null ? lastCursor : Cursor.getDefaultCursor());
        }
    }

}
