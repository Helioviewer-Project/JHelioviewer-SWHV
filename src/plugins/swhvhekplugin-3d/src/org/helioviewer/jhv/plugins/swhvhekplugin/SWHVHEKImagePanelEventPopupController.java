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

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVPoint;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.gui.interfaces.InputControllerPlugin;

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
public class SWHVHEKImagePanelEventPopupController implements MouseListener, MouseMotionListener, InputControllerPlugin {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private Component component;

    private JHVEvent mouseOverJHVEvent = null;
    private Point mouseOverPosition = null;
    private Cursor lastCursor;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setComponent(Component _component) {
        component = _component; // may be null
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
        if (component != null && mouseOverJHVEvent != null) {
            SWEKEventInformationDialog hekPopUp = new SWEKEventInformationDialog(mouseOverJHVEvent);
            hekPopUp.setLocation(calcWindowPosition(mouseOverPosition, hekPopUp.getWidth(), hekPopUp.getHeight()));
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
        if (component == null)
            return;

        JHVEvent lastJHVEvent = mouseOverJHVEvent;
        Date currentDate = Layers.getLastUpdatedTimestamp();

        GL3DVec3d hitpoint = null;
        mouseOverJHVEvent = null;
        mouseOverPosition = null;

        hitpoint = this.getHitPoint(e);

        ArrayList<JHVEvent> toDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(currentDate);
        for (JHVEvent evt : toDraw) {
            HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

            if (pi.containsKey(JHVCoordinateSystem.JHV)) {
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

    private GL3DVec3d getHitPoint(MouseEvent e) {
        return Displayer.getViewport().getCamera().getVectorFromSphere(e.getPoint());
    }

}
