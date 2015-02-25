package org.helioviewer.jhv.plugins.swhvhekplugin.controller;

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

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVPoint;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.BasicImagePanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.swhvhekplugin.cache.SWHVHEKData;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.opengl.GL3DComponentView;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

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

    private View view;
    private ViewportView viewportView;
    private RegionView regionView;

    private BasicImagePanel imagePanel;

    private JHVEvent mouseOverJHVEvent = null;
    private Point mouseOverPosition = null;
    private Cursor lastCursor;
    private SWEKEventInformationDialog hekPopUp;

    private boolean state3D = false;

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
    public View getView() {
        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setView(View newView) {
        view = newView;

        viewportView = view.getAdapter(ViewportView.class);
        regionView = view.getAdapter(RegionView.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImagePanel(BasicImagePanel newImagePanel) {
        imagePanel = newImagePanel;
        imagePanel.addMouseListener(this);
        imagePanel.addMouseMotionListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicImagePanel getImagePanel() {
        return imagePanel;
    }

    /**
     * Converts physical coordinate to screen coordinates
     *
     * @param x
     *            Physical x-coordinate
     * @param y
     *            Physical y-coordinate
     * @return Corresponding screen coordinate
     */
    private Vector2dInt convertPhysicalToScreen(double x, double y) {
        if (regionView == null || viewportView == null) {
            return null;
        }
        ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(viewportView.getViewport(), regionView.getRegion());

        Vector2dInt offset = ViewHelper.convertImageToScreenDisplacement(-regionView.getRegion().getUpperLeftCorner().getX(), regionView.getRegion().getUpperLeftCorner().getY(), regionView.getRegion(), viewportImageSize);

        return ViewHelper.convertImageToScreenDisplacement(x, y, regionView.getRegion(), viewportImageSize).add(offset);
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
                    Displayer.getSingletonInstance().display();
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
        /*
         * GL3DVec3d hitpoint = getHitPoint(e); if (hitpoint != null &&
         * isAPressed()) { Displayer.pointList.add(new GL3DVec3d(hitpoint.x,
         * -hitpoint.y, hitpoint.z)); }
         */
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

        Date currentDate = LayersModel.getSingletonInstance().getLastUpdatedTimestamp();

        state3D = false;
        GL3DVec3d hitpoint = null;
        mouseOverJHVEvent = null;
        mouseOverPosition = null;
        if (view instanceof GL3DComponentView && GL3DState.get() != null && GL3DState.get().getActiveCamera() != null) {
            state3D = true;

            GL3DComponentView gl3dview = (GL3DComponentView) view;
            GL3DSceneGraphView scenegraphview = (GL3DSceneGraphView) gl3dview.getView();
            GL3DRayTracer rayTracer = new GL3DRayTracer(scenegraphview.getHitReferenceShape(), GL3DState.get().getActiveCamera());
            GL3DRay ray = null;

            ray = rayTracer.cast(e.getX() * Displayer.screenScale, e.getY() * Displayer.screenScale);

            if (ray != null) {
                if (ray.getHitPoint() != null) {

                    hitpoint = ray.getHitPoint();
                }
            }
        }
        if (currentDate != null) {
            ArrayList<JHVEvent> toDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(currentDate);

            for (JHVEvent evt : toDraw) {
                if (state3D) {
                    HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

                    if (pi.containsKey(JHVCoordinateSystem.JHV)) {
                        JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV);
                        if (el.centralPoint() != null) {
                            JHVPoint pt = el.centralPoint();
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
                } else {
                    HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

                    if (pi.containsKey(JHVCoordinateSystem.JHV2D)) {
                        JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV2D);
                        if (el.centralPoint() != null) {
                            Vector2dInt screenPos = convertPhysicalToScreen(el.centralPoint().getCoordinate1(), el.centralPoint().getCoordinate2());
                            double x = e.getX() * Displayer.screenScale;
                            double y = e.getY() * Displayer.screenScale;

                            if (screenPos != null && x >= screenPos.getX() - 8 && x <= screenPos.getX() + 8 && y >= screenPos.getY() - 8 && y <= screenPos.getY() + 8) {
                                mouseOverJHVEvent = evt;
                                mouseOverPosition = new Point(screenPos.getX(), screenPos.getY());
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

    public GL3DVec3d getHitPoint(MouseEvent e) {
        GL3DVec3d hitpoint = null;

        if (view instanceof GL3DComponentView && GL3DState.get() != null && GL3DState.get().getActiveCamera() != null) {
            state3D = true;

            GL3DComponentView gl3dview = (GL3DComponentView) view;
            GL3DSceneGraphView scenegraphview = (GL3DSceneGraphView) gl3dview.getView();
            scenegraphview.getHitReferenceShape().setHitCoronaPlane(true);
            scenegraphview.getHitReferenceShape().setUseEarthPlane(true);
            GL3DRayTracer rayTracer = new GL3DRayTracer(scenegraphview.getHitReferenceShape(), GL3DState.get().getActiveCamera());

            GL3DRay ray = null;

            ray = rayTracer.cast(e.getX(), e.getY());

            if (ray != null) {
                if (ray.getHitPoint() != null) {

                    hitpoint = ray.getHitPoint();
                }
            }
            scenegraphview.getHitReferenceShape().setHitCoronaPlane(false);

        }
        return hitpoint;
    }
}
