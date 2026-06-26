package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.JHVEventCache;
import org.helioviewer.jhv.event.JHVPositionInformation;
import org.helioviewer.jhv.event.JHVRelatedEvents;
import org.helioviewer.jhv.event.info.SWEKEventInformationDialog;
import org.helioviewer.jhv.gui.AwtInputAdapter;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.input.InputController;
import org.helioviewer.jhv.input.InputPointerListener;
import org.helioviewer.jhv.input.InputPointerMotionListener;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLRenderer;

class SWEKPopupController implements InputPointerListener, InputPointerMotionListener {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private final SWEKContext swekContext = new SWEKContext();
    private SWEKLayer layer;
    private boolean guiInstalled;
    private boolean mouseTracking;

    private Cursor lastCursor;

    void setLayer(SWEKLayer _layer) {
        if (layer == _layer) {
            updateMouseTracking();
            return;
        }

        resetHover();
        if (layer != null)
            layer.setContext(null);

        layer = _layer;

        if (layer != null)
            layer.setContext(swekContext);
        updateMouseTracking();
    }

    void install() {
        guiInstalled = true;
        updateMouseTracking();
    }

    void uninstall() {
        guiInstalled = false;
        updateMouseTracking();
    }

    private void updateMouseTracking() {
        boolean shouldTrackMouse = guiInstalled && layer != null && layer.isEnabled();
        if (shouldTrackMouse == mouseTracking)
            return;

        mouseTracking = shouldTrackMouse;
        if (mouseTracking) {
            InputController.addListener(this);
        } else {
            InputController.removeListener(this);
            resetHover();
        }
    }

    private static Component component() {
        return MainFrame.getRenderComponent();
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
        if (mouseOverJHVEvent != null) {
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
        Position viewpoint = GLRenderer.getDisplayedViewpoint();
        long currentTime = viewpoint.time.milli;
        List<JHVRelatedEvents> activeEvents = JHVEventCache.getEvents(currentTime, currentTime);
        if (activeEvents.isEmpty()) {
            resetHover();
            return;
        }

        int mouseOverX = e.x();
        int mouseOverY = e.y();

        Viewport vp = Display.getActiveViewport();
        MapView mv = GLRenderer.getMapView();
        JHVRelatedEvents mouseOverJHVEvent = mv.isOrthographic()
                ? findOrthographicEvent(activeEvents, currentTime, mv.mouseToSurface(vp, mouseOverX, mouseOverY), mv.mouseToPlane(vp, mouseOverX, mouseOverY))
                : findProjectedEvent(activeEvents, currentTime, mv, vp, mv.mouseToScreen(vp, mouseOverX, mouseOverY));

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

    private static JHVRelatedEvents findOrthographicEvent(List<JHVRelatedEvents> activeEvents, long currentTime, Vec3 sphereHitpoint, Vec3 planeHitpoint) {
        for (JHVRelatedEvents evtr : activeEvents) {
            JHVEvent evt = evtr.getClosestTo(currentTime);
            JHVPositionInformation pi = evt.getPositionInformation();
            if (pi == null)
                continue;

            Vec3 hitpoint, pt;
            if (evt.isCactus()) {
                double principalAngle = Math.toRadians(SWEKData.readCMEPrincipalAngleDegree(evt));
                double distSun = computeDistSun(evt, currentTime);
                Quat q = pi.getEarth().toQuat();
                pt = q.rotateInverseVector(PolarBasis.vec3(distSun, principalAngle));
                hitpoint = planeHitpoint == null ? null : q.rotateInverseVector(planeHitpoint);
            } else {
                hitpoint = sphereHitpoint;
                pt = pi.centralPoint();
            }

            if (pt != null && hitpoint != null) {
                double deltaX = Math.abs(hitpoint.x - pt.x);
                double deltaY = Math.abs(hitpoint.y - pt.y);
                double deltaZ = Math.abs(hitpoint.z - pt.z);
                if (deltaX < 0.08 && deltaZ < 0.08 && deltaY < 0.08)
                    return evtr;
            }
        }
        return null;
    }

    private static JHVRelatedEvents findProjectedEvent(List<JHVRelatedEvents> activeEvents, long currentTime, MapView mv, Viewport vp, Vec2 mousePosition) {
        MapScale scale = mv.scale(vp);
        for (JHVRelatedEvents evtr : activeEvents) {
            JHVEvent evt = evtr.getClosestTo(currentTime);
            JHVPositionInformation pi = evt.getPositionInformation();
            if (pi == null)
                continue;

            Vec2 tf = null;
            if (mv.isPolar() && evt.isCactus()) {
                double principalAngle = SWEKData.readCMEPrincipalAngleDegree(evt);
                double distSun = computeDistSun(evt, currentTime);
                tf = new Vec2(scale.getXValueInv(principalAngle) * vp.aspect, scale.getYValueInv(distSun));
            } else {
                Vec3 pt = pi.centralPoint();
                if (pt != null)
                    tf = mv.projectToScreen(vp, pt);
            }

            if (tf != null) {
                double deltaX = Math.abs(tf.x - mousePosition.x);
                double deltaY = Math.abs(tf.y - mousePosition.y);
                if (deltaX < 0.02 && deltaY < 0.02)
                    return evtr;
            }
        }
        return null;
    }
}
