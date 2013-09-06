package ch.fhnw.jhv.plugins.vectors.rendering;

/**
 * VectorVisualization Implementation - Cones
 * 
 * Visualize the Vector as a cone. With that implementation it should be more
 * clear in what direction the vector is going.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class VectorVisualizationCones extends VectorVisualizationCylinder {

    /**
     * Radius head
     */
    float radiusSmall = 0.045f;

    /**
     * Radius tail
     */
    float radiusBig = 0.01f;

    /**
     * Get the radius for the start
     * 
     * @param cylinderIndex
     *            index of the cylinder
     */

    public float getRadiusStart(int cylinderIndex) {
        if (isOutgoing[cylinderIndex]) {
            return radiusSmall;
        } else {
            return radiusBig;
        }
    }

    /**
     * Get the radius for the end
     * 
     * @param cylinderIndex
     *            index of the cylinder
     */

    public float getRadiusEnd(int cylinderIndex) {
        if (isOutgoing[cylinderIndex]) {
            return radiusBig;
        } else {
            return radiusSmall;
        }
    }

}
