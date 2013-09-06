package ch.fhnw.jhv.plugins.pfss.rendering;

import java.util.List;

import javax.media.opengl.GL;

import ch.fhnw.jhv.gui.components.controller.AnimatorController;
import ch.fhnw.jhv.gui.components.controller.AnimatorController.AnimationEvent;
import ch.fhnw.jhv.gui.components.controller.TimeDimensionManager;
import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;
import ch.fhnw.jhv.plugins.pfss.data.PfssDimension;

/**
 * The Renderer Plugin that controls which PFSS-Dimension is rendered and what
 * visualiation to use for rendering.
 * 
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 *         10.08.2011
 */
public class PfssRenderer extends AbstractPlugin implements RenderPlugin, AnimatorController.Listener {

    /**
     * All the PFSS Dimensions
     */
    private List<PfssDimension> pfssData;

    /**
     * Current rendered dimensions
     */
    private PfssDimension currentPfssDimension;

    /**
     * Current visualization used for displaying pfss lines
     */
    private PfssVisualization visualization;

    /**
     * Currently used PfssVisualzation
     */
    private PfssVisualization newVis;

    /**
     * Current index in pfssData that is rendered
     */
    private int currentPfssIndex = -1;

    /**
     * True if the visualization must clear the VBOs
     */
    private boolean needClear = false;

    /**
     * True if it is needed to update the VBOs
     */
    private boolean updateVBO = true;

    /**
     * True if new VBOs have been created
     */
    private boolean vboCreated = false;

    /**
     * GL Object
     */
    GL gl;

    /**
     * Constructor
     */
    public PfssRenderer() {
        this.visualization = PfssVisualization.PfssVisualizationType.CYLINDER.getPfssVisualization();
    }

    /**
     * Load PFSS-Data
     * 
     * @param dimensions
     *            List<PfssDimension> PFSS-Data to load
     */
    public void loadData(List<PfssDimension> dimensions) {
        this.pfssData = dimensions;

        currentPfssDimension = null;

        TimeDimensionManager.getInstance().addTimedimensions(this, dimensions.size());
    }

    /**
     * Render method, called every frame. Make sure that the Visualizations
     * prepares and clears its VBO
     * 
     * @param GL
     *            gl
     * @param float
     */
    public void render(GL gl, float currentTime) {
        this.gl = gl;
        if (active && pfssData != null) {

            if (currentPfssDimension != null && newVis != null) {
                if (vboCreated) {
                    visualization.clearVBO(gl);
                }
                visualization = newVis;
                newVis = null;
                vboCreated = false;
                updateVBO = true;
            }

            if (currentPfssDimension == null || currentPfssIndex != (int) currentTime) {
                currentPfssIndex = (int) currentTime;
                if (currentPfssIndex < pfssData.size()) {
                    currentPfssDimension = pfssData.get(currentPfssIndex);
                    if (vboCreated) {
                        visualization.clearVBO(gl);
                    }
                    updateVBO = true;
                }
            }

            if (currentPfssDimension != null) {

                if (updateVBO) {
                    visualization.prepareVBO(gl, currentPfssDimension);
                    vboCreated = true;
                    updateVBO = false;
                }

                visualization.render(gl);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.RenderPlugin#getType()
     */
    public RenderPluginType getType() {
        return RenderPluginType.PFSSCURVES;
    }

    /**
     * Deactivate plugin
     */

    public void deactivate() {
        if (needClear) {
            visualization.clearVBO(gl);
            TimeDimensionManager.getInstance().unloadLayers();
        }
    }

    /**
     * Handle Movie Panel Events
     */
    public void animationAction(AnimationEvent event) {
        // do nothing
    }

    /**
     * Return current visualization
     * 
     * @return PfssVisualization visualization
     */
    public PfssVisualization getPfssVisualization() {
        return visualization;
    }

    /**
     * Set current visualization
     * 
     * param PfssVisualization visualization
     */
    public void setPfssVisualization(PfssVisualization vis) {
        if (visualization == null) {
            visualization = vis;
        } else {
            newVis = vis;
        }
    }
}
