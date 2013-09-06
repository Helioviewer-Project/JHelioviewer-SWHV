/**
 * 
 */
package ch.fhnw.jhv.plugins.vectors.interpolation;

import java.util.ArrayList;

import javax.media.opengl.GL;

import ch.fhnw.jhv.gui.components.controller.AnimatorController;
import ch.fhnw.jhv.gui.components.controller.AnimatorController.AnimationAction;
import ch.fhnw.jhv.gui.components.controller.AnimatorController.AnimationEvent;
import ch.fhnw.jhv.gui.components.controller.TimeDimensionManager;
import ch.fhnw.jhv.gui.controller.cam.CameraContainer;
import ch.fhnw.jhv.gui.controller.cam.CameraContainer.CameraType;
import ch.fhnw.jhv.gui.viewport.ViewPort;
import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;
import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;
import ch.fhnw.jhv.plugins.vectors.data.VectorFieldManager;
import ch.fhnw.jhv.plugins.vectors.rendering.PlaneRenderPlugin;
import ch.fhnw.jhv.plugins.vectors.rendering.VectorRenderer;

/**
 * FieldAnimatorRenderPlugin is responsible
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 */
public class FieldAnimatorRenderPlugin extends AbstractPlugin implements RenderPlugin, VectorFieldManager.Listener, AnimatorController.Listener {

    private VectorFieldManager vfm = VectorFieldManager.getInstance();
    private VectorRenderer vectorRenderer = VectorRenderer.getInstance();

    /**
     * Vector Data of the currently rendered frame
     */
    private ArrayList<VectorData> vectorsCurrentFrame = null;

    /**
     * Difference of the vector's parameter to the next frame that must be
     * increased per tick. This is used for Interpolation.
     */
    private ArrayList<VectorData> differenceToNextFramePerTick = null;

    private boolean isPlaying = false;
    private int currentTimeDimension = 0;
    private float ticksPerFrameInterpolation;
    private VectorField field = null;
    private boolean interpolate = true;

    /**
     * FieldAnimatorPlugin
     * 
     */
    public FieldAnimatorRenderPlugin() {
        // add myself as a listener to VectorFieldManager
        vfm.addListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ch.fhnw.jhv.plugins.interfaces.RenderPlugin#render(javax.media.opengl.GL,
     * float)
     */
    public void render(GL gl, float currentTime) {
        // call render method
        if (vectorsCurrentFrame != null) {

            if (isPlaying) {
                if ((int) currentTime > currentTimeDimension) {
                    // here we may make an optimization:
                    // reuse the start coordinates!!
                    gotoTimeDimension(currentTimeDimension + 1);

                } else if (interpolate && differenceToNextFramePerTick != null) {
                    interpolate();
                    vectorRenderer.updateVBOS();
                }
            }

            vectorRenderer.render(gl, vectorsCurrentFrame, field);
        }
    }

    /**
     * Reload the current dimension if the vectorfield was adjusted
     */
    public void vectorFieldAdjusted() {
        gotoTimeDimension(currentTimeDimension);
    }

    /**
     * Initialize for a different or new vectorfield to be visualized
     */
    public void vectorFieldLoaded(VectorField vectorField) {
        field = vectorField;

        PluginManager pluginManager = PluginManager.getInstance();

        TimeDimensionManager.getInstance().addTimedimensions(this, vectorField.vectors.length);
        gotoTimeDimension(0);

        ViewPort viewPort = ViewPort.getInstance();

        // We have to change the camera back to the normal cam, it could be that
        // the
        // User is currently in the CameraCenterPoint and for that we have to
        // switch back the camera
        if (pluginManager.getRenderPluginByType(RenderPluginType.SUN) != null && pluginManager.getRenderPluginByType(RenderPluginType.SUN).isActive()) {
            viewPort.setActiveCamera(CameraContainer.getCamera(CameraType.ROTATION_SUN));
        } else if (pluginManager.getRenderPluginByType(RenderPluginType.PLANE) != null && pluginManager.getRenderPluginByType(RenderPluginType.PLANE).isActive()) {
            viewPort.setActiveCamera(CameraContainer.getCamera(CameraType.ROTATION_PLANE));
            ((PlaneRenderPlugin) pluginManager.getRenderPluginByType(RenderPluginType.PLANE)).setLastTme(0);
        }
    }

    /**
     * Load the vectors of the specified dimension.
     * 
     * @param dimension
     *            Dimension to be loaded
     */
    private void gotoTimeDimension(int dimension) {
        if (dimension >= 0 && dimension < field.vectors.length) {
            // get adapted field
            vectorsCurrentFrame = vfm.getAdaptedVectorField(dimension);

            if (interpolate && dimension + 1 < field.vectors.length) {
                // if interpolation is on and a next field exists, load it
                differenceToNextFramePerTick = vfm.getAdaptedVectorField(dimension + 1);

                // create the differences per tick to the next frame
                for (int i = 0; i < vectorsCurrentFrame.size(); i++) {
                    if (vectorsCurrentFrame.get(i) != null && differenceToNextFramePerTick.get(i) != null) {

                        float diffInclination, diffAzimuth, diffLength;
                        VectorData current = vectorsCurrentFrame.get(i);
                        VectorData next = differenceToNextFramePerTick.get(i);

                        if (current.inclination > 90 && next.inclination <= 90 || current.inclination <= 90 && next.inclination > 90) {
                            diffInclination = diffAzimuth = diffLength = 0;
                        } else {
                            diffInclination = (differenceToNextFramePerTick.get(i).inclination - vectorsCurrentFrame.get(i).inclination) / ticksPerFrameInterpolation;

                            diffAzimuth = (differenceToNextFramePerTick.get(i).azimuth - vectorsCurrentFrame.get(i).azimuth) / ticksPerFrameInterpolation;

                            diffLength = (differenceToNextFramePerTick.get(i).length - vectorsCurrentFrame.get(i).length) / ticksPerFrameInterpolation;
                        }

                        differenceToNextFramePerTick.get(i).inclination = diffInclination;
                        differenceToNextFramePerTick.get(i).azimuth = diffAzimuth;
                        differenceToNextFramePerTick.get(i).length = diffLength;

                    }
                }
            } else {
                differenceToNextFramePerTick = null;
            }

            currentTimeDimension = dimension;
            vectorRenderer.updateVBOS();
        }
    }

    public void interpolate() {

        for (int i = 0; i < vectorsCurrentFrame.size(); i++) {
            if (vectorsCurrentFrame.get(i) != null && differenceToNextFramePerTick.get(i) != null) {
                vectorsCurrentFrame.get(i).inclination += differenceToNextFramePerTick.get(i).inclination;
                vectorsCurrentFrame.get(i).azimuth += differenceToNextFramePerTick.get(i).azimuth;
                vectorsCurrentFrame.get(i).length += differenceToNextFramePerTick.get(i).length;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.RenderPlugin#getType()
     */
    public RenderPluginType getType() {
        return RenderPluginType.FIELDANIMATOR;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.gui.components.controller.AnimatorController.Listener#
     * animationAction
     * (ch.fhnw.jhv.gui.components.controller.AnimatorController.AnimationEvent)
     */
    public void animationAction(AnimationEvent event) {

        AnimationAction action = event.getAction();
        if (action == AnimationAction.PLAY) {
            // retrieve
            isPlaying = true;
            interpolate = event.isInterpolated();
            ticksPerFrameInterpolation = event.getTimeDeltaPerFrame() * ViewPort.FPS;
            gotoTimeDimension(currentTimeDimension);
        } else if (action == AnimationAction.PAUSE) {
            isPlaying = false;
        } else if (action == AnimationAction.GOTOFRAME) {
            gotoTimeDimension((int) event.getFrameNumber());
        } else if (action == AnimationAction.STOP) {
            isPlaying = false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.AbstractPlugin#deactivate()
     */

    public void deactivate() {
        super.deactivate();

        if (vectorsCurrentFrame != null) {
            TimeDimensionManager.getInstance().unloadLayers();
        }

        vectorRenderer.clearVBOS();
    }
}
