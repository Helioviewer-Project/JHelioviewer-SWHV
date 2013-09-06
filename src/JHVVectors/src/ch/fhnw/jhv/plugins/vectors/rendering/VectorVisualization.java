/**
 * 
 */
package ch.fhnw.jhv.plugins.vectors.rendering;

import java.util.ArrayList;

import javax.media.opengl.GL;

import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;

/**
 * VectorVisualiation Interface
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public interface VectorVisualization {

    /**
     * Contains the different types of Vector Visualizations
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    public enum VectorVisualizationType {

        CYLINDERS(new VectorVisualizationCylinder(), "Cylinder"), STRAIGHTCYLINDER(new VectorVisualizationStraightCylinder(), "Upright Cylinder"), CONES(new VectorVisualizationCones(), "Cones"), FATCONES(new VectorVisualizationFatCones(), "Cones (varying width)"), ARROWS(new VectorVisualizationArrows(), "Arrows");

        /**
         * Label for the gui
         */
        private final String label;

        /**
         * Contains the Visualization
         */
        private final VectorVisualization vis;

        /**
         * Constructor
         * 
         * @param Visualization
         *            vis
         * @param String
         *            label
         */
        VectorVisualizationType(VectorVisualization vis, String label) {
            this.vis = vis;
            this.label = label;
        }

        /**
         * Return the Visualization
         * 
         * @return VectorVisualization corresponding visualization
         */
        public VectorVisualization getVisualization() {
            return this.vis;
        }

        public String getLabel() {
            return this.label;
        }
    }

    /**
     * Prepare the Vertex Buffer Objects (VBO's)
     * 
     * @param GL
     *            gl
     */
    void prepareVBO(GL gl, ArrayList<VectorData> vectors, VectorField field);

    /**
     * Render the Vertex Buffer Objects (VBO's)
     * 
     * @param GL
     *            gl
     */
    void render(GL gl);

    /**
     * Clear all old existing VBOS
     * 
     * @param GL
     *            gl
     */
    void clearOldVBOS(GL gl);
}
