package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.Cursor;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVPoint;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.MainImagePanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.opengl.GL3DComponentView;

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
public class SWHVHEKImagePanelEventPopupController implements KeyEventDispatcher, ImagePanelPlugin, MouseListener, MouseMotionListener {

    // ///////////////////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////////////////

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;
    private boolean aPressed = false;

    private GL3DComponentView view;

    private MainImagePanel imagePanel;

    private JHVEvent mouseOverJHVEvent = null;
    private Point mouseOverPosition = null;
    private Cursor lastCursor;
    private SWEKEventInformationDialog hekPopUp;

    // ///////////////////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////////////////
    public SWHVHEKImagePanelEventPopupController() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentView getView() {
        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setView(ComponentView newView) {
        view = (GL3DComponentView) newView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImagePanel(MainImagePanel newImagePanel) {
        imagePanel = newImagePanel;
        imagePanel.addMouseListener(this);
        imagePanel.addMouseMotionListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MainImagePanel getImagePanel() {
        return imagePanel;
    }

    private Point calcWindowPosition(Point p) {
        int yCoord = 0;
        boolean yCoordInMiddle = false;
        if (p.y + hekPopUp.getSize().height + yOffset < imagePanel.getSize().height) {
            yCoord = p.y + imagePanel.getLocationOnScreen().y + yOffset;
        } else {
            yCoord = p.y + imagePanel.getLocationOnScreen().y - hekPopUp.getSize().height - yOffset;
            if (yCoord < imagePanel.getLocationOnScreen().y) {
                yCoord = imagePanel.getLocationOnScreen().y + imagePanel.getSize().height - hekPopUp.getSize().height;
                if (yCoord < imagePanel.getLocationOnScreen().y) {
                    yCoord = imagePanel.getLocationOnScreen().y;
                }
                yCoordInMiddle = true;
            }
        }

        int xCoord = 0;
        if (p.x + hekPopUp.getSize().width + xOffset < imagePanel.getSize().width) {
            xCoord = p.x + imagePanel.getLocationOnScreen().x + xOffset;
        } else {
            xCoord = p.x + imagePanel.getLocationOnScreen().x - hekPopUp.getSize().width - xOffset;
            if (xCoord < imagePanel.getLocationOnScreen().x && !yCoordInMiddle) {
                xCoord = imagePanel.getLocationOnScreen().x + imagePanel.getSize().width - hekPopUp.getSize().width;
            }
        }

        return new Point(xCoord, yCoord);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ke) {
        synchronized (this) {
            switch (ke.getID()) {
            case KeyEvent.KEY_PRESSED:
                if (ke.getKeyCode() == KeyEvent.VK_A) {
                    aPressed = true;
                }
                if (ke.getKeyCode() == KeyEvent.VK_R) {
                    Displayer.display();
                }
                break;

            case KeyEvent.KEY_RELEASED:
                if (ke.getKeyCode() == KeyEvent.VK_A) {
                    aPressed = false;
                }
                break;
            }

            return false;
        }
    }

    public boolean isAPressed() {
        synchronized (this) {
            return aPressed;
        }
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
            imagePanel.setCursor(helpCursor);
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
                lastCursor = imagePanel.getCursor();
                imagePanel.setCursor(helpCursor);
            } else if (lastJHVEvent != null && mouseOverJHVEvent == null) {
                imagePanel.setCursor(lastCursor);
            }
        }
    }

    @Override
    public void detach() {
    }

    private GL3DVec3d getHitPoint(MouseEvent e) {
        GL3DState state = GL3DState.get();
        /* workaround for null GL3DState on startup */
        if (state != null) {
            GL3DCamera activeCamera = state.getActiveCamera();
            GL3DVec3d pt = activeCamera.getVectorFromSphere(e.getPoint());
            return pt;
        } else
            return null;
    }

}
