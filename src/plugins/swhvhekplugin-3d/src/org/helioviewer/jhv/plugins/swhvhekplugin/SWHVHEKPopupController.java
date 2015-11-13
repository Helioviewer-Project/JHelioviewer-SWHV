package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.interfaces.InputControllerPlugin;
import org.helioviewer.jhv.layers.TimeListener;
import org.helioviewer.jhv.opengl.GLHelper;

public class SWHVHEKPopupController implements MouseListener, MouseMotionListener, InputControllerPlugin, TimeListener {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private Component component;

    protected static JHVEvent mouseOverJHVEvent = null;
    protected static Point mouseOverPosition = null;
    private static Cursor lastCursor;

    protected Date currentTime;

    @Override
    public void setComponent(Component _component) {
        component = _component;
    }

    @Override
    public void timeChanged(JHVDate date) {
        currentTime = date.getDate();
    }

    private Point calcWindowPosition(Point p, int hekWidth, int hekHeight) {
        int yCoord = 0;
        boolean yCoordInMiddle = false;

        int compWidth = component.getWidth();
        int compHeight = component.getHeight();
        int compLocX = component.getLocationOnScreen().x;
        int compLocY = component.getLocationOnScreen().y;

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

        int xCoord = 0;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (mouseOverJHVEvent != null) {
            SWEKEventInformationDialog hekPopUp = new SWEKEventInformationDialog(mouseOverJHVEvent);
            hekPopUp.setLocation(calcWindowPosition(GLHelper.GL2AWTPoint(mouseOverPosition), hekPopUp.getWidth(), hekPopUp.getHeight()));
            hekPopUp.pack();
            hekPopUp.setVisible(true);

            component.setCursor(helpCursor);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {
        mouseOverPosition = null;
        mouseOverJHVEvent = null;
        JHVEventContainer.highlight(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        mouseOverJHVEvent = null;
        mouseOverPosition = null;
        Vec3 pt = null;
        Vec3 hitpoint = null;
        ArrayList<JHVEvent> eventsToDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(currentTime);
        for (JHVEvent evt : eventsToDraw) {
            HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

            if (evt.getName().equals("Coronal Mass Ejection")) {
                Map<String, JHVEventParameter> params = evt.getAllEventParameters();
                double principalAngle = Math.toRadians(SWHVHEKData.readCMEPrincipalAngleDegree(params));
                double speed = SWHVHEKData.readCMESpeed(params);
                double distSun = 2.4;
                distSun += speed * (currentTime.getTime() - evt.getStartDate().getTime()) / Sun.RadiusMeter;

                Position.Latitudinal p = Sun.getEarth((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2);
                Quat localRotation = new Quat(p.lat, p.lon);
                hitpoint = localRotation.rotateInverseVector(getHitPointPlane(e));

                Quat q = new Quat(p.lat, p.lon);
                pt = q.rotateInverseVector(new Vec3(distSun * Math.cos(principalAngle), distSun * Math.sin(principalAngle), 0));
            } else if (pi.containsKey(JHVCoordinateSystem.JHV)) {
                hitpoint = getHitPoint(e);
                pt = pi.get(JHVCoordinateSystem.JHV).centralPoint();
            }

            if (pt != null && hitpoint != null) {
                double deltaX = Math.abs(hitpoint.x - pt.x);
                double deltaY = Math.abs(hitpoint.y - pt.y);
                double deltaZ = Math.abs(hitpoint.z - pt.z);
                if (deltaX < 0.08 && deltaZ < 0.08 && deltaY < 0.08) {
                    mouseOverJHVEvent = evt;
                    mouseOverPosition = e.getPoint();
                    break;
                }
            }
        }
        JHVEventContainer.highlight(mouseOverJHVEvent);
        if (helpCursor != component.getCursor())
            lastCursor = component.getCursor();

        if (mouseOverJHVEvent != null) {
            component.setCursor(helpCursor);
        } else {
            component.setCursor(lastCursor);
        }
    }

    private Vec3 getHitPointPlane(MouseEvent e) {
        return CameraHelper.getVectorFromPlane(Displayer.getCamera(), Displayer.getViewport(), e.getPoint());
    }

    private Vec3 getHitPoint(MouseEvent e) {
        Vec3 hp = CameraHelper.getVectorFromSphere(Displayer.getCamera(), Displayer.getViewport(), e.getPoint());
        if (hp != null)
            hp.y = -hp.y;
        return hp;
    }

}
