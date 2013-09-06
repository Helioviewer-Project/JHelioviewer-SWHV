package ch.fhnw.jhv.plugins.vectors.rendering;

import ch.fhnw.jhv.plugins.vectors.data.VectorFieldManager;

/**
 * VectorVisualization Implementation - Cone with varying with
 * 
 * Visualize the Vector as a cone. With that implementation it should be more
 * clear in what direction the vector is going.
 * 
 * Special on this implementation is that the vectors have a varying with. This
 * means the stronger the vector is the higher is the width of the cone.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class VectorVisualizationFatCones extends VectorVisualizationCones {

    /**
     * Scale minimum factor
     */
    float scaleMin = 0.3f;

    /**
     * Scale maximum factor
     */
    float scaleMax = 5.0f;

    /**
     * Radius at the tail
     */
    float radiusStart = 0.045f;

    /**
     * Radius at the head
     */
    float radiusEnd = 0.01f;

    /**
     * Scale value for the cylinder
     */
    float cylinderScale = 1.0f;

    /**
     * Get the radius for the start
     * 
     * @param cylinderIndex
     *            index of the cylinder
     */

    public float getRadiusStart(int cylinderIndex) {
        int currentAverageFactor = VectorFieldManager.getInstance().getAverageFactor();
        float offset = currentAverageFactor / 2.5f;
        cylinderScale = ((vectors[cylinderIndex].length - minStrength) * (scaleMax - scaleMin) / (maxStrength - minStrength) + scaleMin) + offset;
        return super.getRadiusStart(cylinderIndex) * cylinderScale;
    }

    /**
     * Get the radius for the end
     * 
     * @param cylinderIndex
     *            index of the cylinder
     */

    public float getRadiusEnd(int cylinderIndex) {
        int currentAverageFactor = VectorFieldManager.getInstance().getAverageFactor();
        float offset = currentAverageFactor / 2.5f;
        cylinderScale = ((vectors[cylinderIndex].length - minStrength) * (scaleMax - scaleMin) / (maxStrength - minStrength) + scaleMin) + offset;
        return super.getRadiusEnd(cylinderIndex) * cylinderScale;
    }

}