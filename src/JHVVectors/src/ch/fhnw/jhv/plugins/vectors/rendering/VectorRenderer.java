package ch.fhnw.jhv.plugins.vectors.rendering;

import java.util.ArrayList;

import javax.media.opengl.GL;

import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;
import ch.fhnw.jhv.plugins.vectors.rendering.VectorVisualization.VectorVisualizationType;

/**
 * VectorRenderer is the main render Plugin in the VectorPlugin. It handles the
 * recreation of the VBOs.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public final class VectorRenderer {

    /**
     * Vector Visualization method which is currently used
     */
    private VectorVisualization vectorVis;

    /**
     * Render method first time called
     */
    private boolean firstTime = true;

    /**
     * If this flag is set, VBOS need to be recreated
     */
    private boolean updateVBOS = false;

    /**
     * GL object
     */
    private GL gl;

    /**
     * Holder Class for the singleton pattern
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    private static class Holder {
        private static final VectorRenderer INSTANCE = new VectorRenderer();
    }

    /**
     * Get the only existing instance of the VectorRendererPlugin
     * 
     * @return VectorRenderPlugin vectorRendererPlugin
     */
    public static VectorRenderer getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Constructor
     * 
     */
    private VectorRenderer() {
        vectorVis = VectorVisualizationType.CONES.getVisualization();

        this.gl = null;
    }

    /**
     * Render the VBO's
     * 
     * @param GL
     *            gl
     * @param ArrayList
     *            <VectorData> vectorData
     * @param VectorField
     *            vector field
     * 
     */
    public void render(GL gl, ArrayList<VectorData> vectors, VectorField field) {

        this.gl = gl;

        // if its the first time rendering or if an update of the VBOS is
        // necesarray
        // clean the old VBOs and create the new ones
        if (firstTime || updateVBOS) {
            // clear old existing VBOs
            if (!firstTime)
                vectorVis.clearOldVBOS(gl);

            // prepare new VBOs
            vectorVis.prepareVBO(gl, vectors, field);

            firstTime = false;
            updateVBOS = false;
        }

        // render the VBO directly in the visualization class
        vectorVis.render(gl);
    }

    /**
     * Return the current visualization method
     * 
     * @return VectorVisualization
     */
    public VectorVisualization getVectorVisualization() {
        return this.vectorVis;
    }

    /**
     * Define the current Visualization method
     * 
     * @param VectorVisualization
     *            type
     */
    public void setVectorVis(VectorVisualizationType type) {
        vectorVis = type.getVisualization();
    }

    /**
     * Set flag for updating the VBOS
     */
    public void updateVBOS() {
        updateVBOS = true;
    }

    /**
     * Clear the VBOS of the current used visualization method
     */
    public void clearVBOS() {
        if (this.gl != null)
            vectorVis.clearOldVBOS(gl);
    }
}
