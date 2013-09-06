package ch.fhnw.jhv.gui.controller.cam;

import java.awt.event.MouseWheelEvent;

import ch.fhnw.jhv.gui.viewport.components.SunRenderPlugin;

/**
 * An abstract camera that supports zooming by mousewheel.
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 05.07.2011
 * 
 */
public abstract class AbstractZoomCamera extends AbstractCamera {

    /**
     * For any zoom that is higher than normalZoom, the zoom steps are not
     * adjusted. For a zoom that is lower than normalZoom, the zoomDelta becomes
     * smaller
     */
    protected float normalZoom = 30.0f;

    /**
     * the zoom-delta that is added to the zoom if the current zoom is greater
     * or equal to normalZoom
     */
    protected float maxZoomDelta = 1f;

    /**
     * the zoom-delta that is added to the zoom if the camera is the closest
     * possible to the sun
     */
    protected float minZoomDelta = 0.05f;

    /**
     * current zoom
     */
    float zoom = 55.0f;

    /**
     * If the flag is set, there is no zoom limit
     */
    private boolean limitZoom = true;

    /**
     * If the flag is set, the zoom speed will decrease if the zoom factor gets
     * smaller
     */
    private boolean variableZoomDelta = true;

    /**
     * Adjusts the zoom of the camera. If the camera gets near the sun the zoom
     * is adjusted in smaller steps.
     */

    public void mouseWheelMoved(MouseWheelEvent e) {

        /**
         * adjust zoom delta! The closer the viewer gets to the sun, the smaller
         * becomes the zoomdelta.
         */
        float zoomDelta;
        if (variableZoomDelta) {
            zoomDelta = zoom - SunRenderPlugin.SUN_RADIUS > normalZoom ? maxZoomDelta : minZoomDelta + (zoom - SunRenderPlugin.SUN_RADIUS) * ((maxZoomDelta - minZoomDelta) / normalZoom);
        } else {
            zoomDelta = maxZoomDelta;
        }

        if (e.getWheelRotation() > 0) {
            zoom += zoomDelta;
        } else {
            zoom -= zoomDelta;
        }

        // avoid the camera inside the sun!
        if (limitZoom) {
            zoom = (zoom > SunRenderPlugin.SUN_RADIUS + near) ? zoom : SunRenderPlugin.SUN_RADIUS + near;
        } else {
            zoom = zoom < 1 ? 1 : zoom;
        }
    }

    /**
     * Enable the zoom limitations
     * 
     * @param enabled
     */
    public void enableZoomLimit(boolean enabled) {
        this.limitZoom = enabled;
    }

    /**
     * Enable a variable zoom delta. if enabled, the zoom delta decrease when
     * zoomed in.
     * 
     * @param enabled
     */
    public void enableVariableZoomDelta(boolean enabled) {
        this.variableZoomDelta = enabled;
    }

}
