package ch.fhnw.jhv.plugins.pfss.rendering;

import javax.media.opengl.GL;

import ch.fhnw.jhv.plugins.pfss.data.PfssDimension;

/**
 * PfssVisualization Interface
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public interface PfssVisualization {

    /**
     * Contains the different types of Pfss Visualizations
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    public enum PfssVisualizationType {

        CYLINDER(new PfssCylinderVisualization(), "Cylinder"), LINE(new PfssLineVisualization(), "Line");

        /**
         * Label for the gui
         */
        private final String label;

        /**
         * Contains the PFSS Visualization
         */
        private final PfssVisualization vis;

        /**
         * Constructor
         * 
         * @param Visualization
         *            vis
         * @param String
         *            label
         */
        PfssVisualizationType(PfssVisualization vis, String label) {
            this.vis = vis;
            this.label = label;
        }

        /**
         * Return the PFSS Visualization
         * 
         * @return PfssVisualization corresponding visualization
         */
        public PfssVisualization getPfssVisualization() {
            return this.vis;
        }

        /**
         * Return the label
         * 
         * @return String label
         */
        public String getLabel() {
            return this.label;
        }
    }

    /**
     * Render method
     * 
     * @param GL
     *            gl
     */
    public void render(GL gl);

    /**
     * Prepare VBOs
     * 
     * @param GL
     *            gl
     * @param PfssDimension
     *            pfss
     */
    public void prepareVBO(GL gl, PfssDimension pfss);

    /**
     * Clear all VBOs
     * 
     * @param GL
     *            gl
     */
    public void clearVBO(GL gl);
}
