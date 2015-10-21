package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.helioviewer.base.astronomy.Position;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVPoint;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.interfaces.InputControllerPlugin;
import org.helioviewer.jhv.layers.TimeListener;
import org.helioviewer.jhv.opengl.GLHelper;

/**
 * Implementation of ImagePanelPlugin for showing event popups.
 *
 * <p>
 * This plugin provides the capability to open an event popup when clicking on
 * an event icon within the main image. Apart from that, it changes the mouse
 * pointer when hovering over an event icon to indicate that it is clickable.
 *
 * @author Markus Langenberg
 * @author Malte Nuhn
 *
 */
public class SWHVHEKImagePanelEventPopupController implements MouseListener, MouseMotionListener, InputControllerPlugin, TimeListener {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private Component component;

    private JHVEvent mouseOverJHVEvent = null;
    private Point mouseOverPosition = null;
    private Cursor lastCursor;

    public Date currentTime;

    @Override
    public void setComponent(Component _component) {
        component = _component;
    }

    @Override
    public void timeChanged(Date date) {
        currentTime = date;
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
        JHVEvent lastJHVEvent = mouseOverJHVEvent;

        GL3DVec3d hitpoint = null;

        mouseOverJHVEvent = null;
        mouseOverPosition = null;

        hitpoint = getHitPoint(e);
        ArrayList<JHVEvent> eventsToDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(currentTime);
        for (JHVEvent evt : eventsToDraw) {
            HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();
            if (evt.getName().equals("Coronal Mass Ejection")) {

                double principalAngle = 0;
                Collection<JHVEventParameter> params = evt.getAllEventParameters().values();
                double distSun = 2.4;
                double speed = 500;
                for (JHVEventParameter param : params) {
                    String name = param.getParameterName();
                    String value = param.getParameterValue();
                    if (name.equals("event_coord1")) {
                        principalAngle = Double.parseDouble(value) * Math.PI / 180. + Math.PI / 2;
                    }
                    if (name.equals("cme_radiallinvel")) {
                        speed = Double.parseDouble(value);
                    }
                }
                double factor = Sun.RadiusMeter;

                distSun += speed * (currentTime.getTime() - evt.getStartDate().getTime()) / factor;

                Date date = new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2);
                Position.Latitudinal p = Sun.getEarth(date);
                GL3DQuatd localRotation = new GL3DQuatd(p.lat, p.lon);
                GL3DVec3d hitpointPlane = localRotation.rotateInverseVector(getHitPointPlane(e));

                GL3DQuatd q = new GL3DQuatd(p.lat, p.lon);
                GL3DVec3d pt = q.rotateInverseVector(new GL3DVec3d(distSun * Math.cos(principalAngle), distSun * Math.sin(principalAngle), 0));

                if (pt != null) {
                    if (hitpointPlane != null) {
                        double deltaX = Math.abs(hitpointPlane.x - pt.x);
                        double deltaY = Math.abs(hitpointPlane.y - pt.y);
                        double deltaZ = Math.abs(hitpointPlane.z - pt.z);
                        if (deltaX < 0.08 && deltaZ < 0.08 && deltaY < 0.08) {
                            mouseOverJHVEvent = evt;
                            mouseOverPosition = new Point(e.getX(), e.getY());
                        }
                    }
                }
            }
            else if (pi.containsKey(JHVCoordinateSystem.JHV)) {
                JHVPoint pt = pi.get(JHVCoordinateSystem.JHV).centralPoint();
                if (pt != null) {
                    if (hitpoint != null) {
                        double deltaX = Math.abs(hitpoint.x - pt.getCoordinate1());
                        double deltaY = Math.abs(hitpoint.y + pt.getCoordinate2());
                        double deltaZ = Math.abs(hitpoint.z - pt.getCoordinate3());
                        if (deltaX < 0.08 && deltaZ < 0.08 && deltaY < 0.08) {
                            mouseOverJHVEvent = evt;
                            mouseOverPosition = new Point(e.getX(), e.getY());
                        }
                    }
                }
            }
        }

        if (mouseOverJHVEvent != null) {
            mouseOverJHVEvent.highlight(true, this);
        }
        if (lastJHVEvent != mouseOverJHVEvent && lastJHVEvent != null) {
            lastJHVEvent.highlight(false, this);
        }
        if (lastJHVEvent == null && mouseOverJHVEvent != null) {
            lastCursor = component.getCursor();
            component.setCursor(helpCursor);
        } else if (lastJHVEvent != null && mouseOverJHVEvent == null) {
            component.setCursor(lastCursor);
        }
    }

    static Point highlightedMousePosition = new Point(0, 0);

    private GL3DVec3d getHitPointPlane(MouseEvent e) {
        highlightedMousePosition = e.getPoint();
        return Displayer.getViewport().getCamera().getVectorFromPlane(e.getPoint());
    }

    private GL3DVec3d getHitPoint(MouseEvent e) {
        return Displayer.getViewport().getCamera().getVectorFromSphere(e.getPoint());
    }

}
