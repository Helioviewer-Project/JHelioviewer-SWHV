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
import org.helioviewer.jhv.gui.interfaces.ComponentViewPlugin;
import org.helioviewer.viewmodel.view.ComponentView;

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
public class SWHVHEKImagePanelEventPopupController implements MouseListener, MouseMotionListener, ComponentViewPlugin {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private Component component;

    private JHVEvent mouseOverJHVEvent = null;
    private Point mouseOverPosition = null;
    private Cursor lastCursor;
    private SWEKEventInformationDialog hekPopUp;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setView(ComponentView componentView) {
        if (componentView == null)
            component = null;
        else
            component = componentView.getComponent();
    }

    private Point calcWindowPosition(Point p) {
        int yCoord = 0;
        boolean yCoordInMiddle = false;
        if (p.y + hekPopUp.getSize().height + yOffset < component.getSize().height) {
            yCoord = p.y + component.getLocationOnScreen().y + yOffset;
        } else {
            yCoord = p.y + component.getLocationOnScreen().y - hekPopUp.getSize().height - yOffset;
            if (yCoord < component.getLocationOnScreen().y) {
                yCoord = component.getLocationOnScreen().y + component.getSize().height - hekPopUp.getSize().height;
                if (yCoord < component.getLocationOnScreen().y) {
                    yCoord = component.getLocationOnScreen().y;
                }
                yCoordInMiddle = true;
            }
        }

        int xCoord = 0;
        if (p.x + hekPopUp.getSize().width + xOffset < component.getSize().width) {
            xCoord = p.x + component.getLocationOnScreen().x + xOffset;
        } else {
            xCoord = p.x + component.getLocationOnScreen().x - hekPopUp.getSize().width - xOffset;
            if (xCoord < component.getLocationOnScreen().x && !yCoordInMiddle) {
                xCoord = component.getLocationOnScreen().x + component.getSize().width - hekPopUp.getSize().width;
            }
        }

        return new Point(xCoord, yCoord);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(final MouseEvent e) {
        //Uncomment to enable point and click
        if (mouseOverJHVEvent != null) {
            // should never be the case
            // if (hekPopUp == null) {
            hekPopUp = new SWEKEventInformationDialog(mouseOverJHVEvent);
            // }
            // hekPopUp.setVisible(false);

            Point windowPosition = calcWindowPosition(mouseOverPosition);
            hekPopUp.setLocation(windowPosition);
            hekPopUp.setVisible(true);
            hekPopUp.pack();
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
        Date currentDate = Displayer.getLastUpdatedTimestamp();

        GL3DVec3d hitpoint = null;
        mouseOverJHVEvent = null;
        mouseOverPosition = null;

        hitpoint = this.getHitPoint(e);

        if (currentDate != null) {
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
    }

    private GL3DVec3d getHitPoint(MouseEvent e) {
        return Displayer.getActiveCamera().getVectorFromSphere(e.getPoint());
    }

}
