package ch.fhnw.jhv.gui.controller.cam;

import java.awt.event.MouseEvent;

import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin.RenderPluginType;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;
import ch.fhnw.jhv.plugins.vectors.data.VectorFieldManager;
import ch.fhnw.jhv.plugins.vectors.rendering.PlaneRenderPlugin;

/**
 * This Camera is used for the Plane Visualization. It inherits all
 * functionality from LookAtCamera, but it overrides some behaviour
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 25.07.2011
 * 
 */
public class PlaneRotationCamera extends LookAtCamera {

    /**
     * Constructor
     */
    public PlaneRotationCamera() {
        super();

        this.enableVariableZoomDelta(false);
        this.enableZoomLimit(false);

        inclination = 30;
        zoom = 18;

        useNormalZoomFactor = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ch.fhnw.jhv.gui.controller.cam.LookAtCamera#mouseClicked(java.awt.event
     * .MouseEvent)
     */

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {

            if (centerRenderer == null) {
                VectorField field = VectorFieldManager.getInstance().getOriginalField();

                int fieldWidth = (int) (field.sizePixel.x * PlaneRenderPlugin.PLANE_SCALE);
                int fieldHeight = (int) (field.sizePixel.y * PlaneRenderPlugin.PLANE_SCALE);

                centerRenderer = new CameraCenterPointRenderPlugin(lookat, fieldWidth / 2, -fieldWidth / 2, 20, 0, fieldHeight / 2, -fieldHeight / 2);

                PluginManager.getInstance().updateRenderPlugingReference(RenderPluginType.CAMERACENTERPOINT, centerRenderer);
                PluginManager.getInstance().activateRenderPluginType(RenderPluginType.CAMERACENTERPOINT);

            } else {
                PluginManager.getInstance().deactivateRenderPluginType(RenderPluginType.CAMERACENTERPOINT);
                lookat = centerRenderer.getPosition();
                centerRenderer = null;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.gui.controller.cam.LookAtCamera#getRotationFactor()
     */

    protected float getRotationFactor() {
        // don't slow down rotation depending on zoom
        return 1.0f;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.gui.controller.cam.LookAtCamera#clampRotation()
     */

    protected void clampRotation() {
        if (inclination > 80) {
            inclination = 80;
        } else if (inclination < 0) {
            inclination = 0;
        }

        if (azimuth < -360) {
            azimuth += 360;
        } else if (azimuth > 360) {
            azimuth -= 360;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.gui.controller.cam.LookAtCamera#getLabel()
     */

    public String getLabel() {
        return "Plane Rotation Camera";
    }
}