package ch.fhnw.jhv.plugins.vectors.rendering;

import java.util.List;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import ch.fhnw.jhv.gui.viewport.components.SunRenderPlugin;
import ch.fhnw.jhv.helper.MathHelper;
import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;

/**
 * VectorVisualization of Upright Cylinders. With this visualization method all
 * the vectors are looking 90¡ from the sun upright. Specially for the sun it
 * should give a better overview because the vectors don't cross others way.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class VectorVisualizationStraightCylinder extends VectorVisualizationCylinder {

    /**
     * Calculate the points on the sphere
     * 
     * @param vectorList
     *            A list of prepared VectorData
     * @param vf
     *            The actual vector field
     * 
     * @return How many vectors are calculated
     */

    protected int calculateOnSphere(List<VectorData> vectorList, VectorField field) {
        Vector3f start = new Vector3f();
        Vector3f end = new Vector3f();

        Matrix4f rotationY = new Matrix4f();
        Matrix4f rotationX = new Matrix4f();
        Matrix4f rotationPosition = new Matrix4f();

        float deltaX = field.sizeArcsec.x / field.sizePixel.x;
        float deltaY = field.sizeArcsec.y / field.sizePixel.y;

        int counter = 0;
        for (int i = 0; i < vectorList.size(); i++) {
            if (vectorList.get(i) != null) {
                int index = counter * 3;
                VectorData current = vectorList.get(i);
                this.vectors[counter] = current;

                // create rotation-matrix for surface point of sun
                rotationY.rotY(((field.posArcsec.x + deltaX * current.x) * MathHelper.ARCSECONDS2DEG * MathHelper.DEG2RAD));
                rotationX.rotX((-field.posArcsec.y + deltaY * current.y) * MathHelper.ARCSECONDS2DEG * MathHelper.DEG2RAD);
                rotationPosition.mul(rotationY, rotationX);

                // transform start position
                start.set(0, 0, SunRenderPlugin.SUN_RADIUS);
                rotationPosition.transform(start);

                // create end point of vector
                end.set(0, 0, SunRenderPlugin.SUN_RADIUS + current.length);
                rotationPosition.transform(end);

                startPoints[index] = start.x;
                startPoints[index + 1] = start.y;
                startPoints[index + 2] = start.z;

                endPoints[index] = end.x;
                endPoints[index + 1] = end.y;
                endPoints[index + 2] = end.z;

                isOutgoing[counter] = (current.inclination < 90);

                // Determine the min and max value. Later used in the different
                // visualizations for scaling.
                determineMinMax(current);

                counter++;
            }
        }
        return counter;
    }

    /**
     * Calculate the points on the plane
     * 
     * @param vectorList
     *            A list of prepared VectorData
     * @param field
     *            The actual vector field
     * 
     * @return How many vectors are calculated
     */

    protected int calculateOnPlane(List<VectorData> vectorsList, VectorField field) {
        Vector3f start = new Vector3f();
        Vector3f end = new Vector3f();

        float xTranslation = field.sizePixel.x * PlaneRenderPlugin.PLANE_SCALE * 0.5f;
        float yTranslation = field.sizePixel.y * PlaneRenderPlugin.PLANE_SCALE * 0.5f;

        int counter = 0;

        for (int i = 0; i < vectorsList.size(); i++) {
            if (vectorsList.get(i) != null) {
                int index = counter * 3;
                VectorData current = vectorsList.get(i);
                vectors[counter] = current;

                // transform start position
                start.set(current.x * PlaneRenderPlugin.PLANE_SCALE - xTranslation, 0, current.y * PlaneRenderPlugin.PLANE_SCALE - yTranslation);

                // create end point of vector
                end.set(start.x, current.length, start.z);

                startPoints[index] = start.x;
                startPoints[index + 1] = start.y;
                startPoints[index + 2] = start.z;

                endPoints[index] = end.x;
                endPoints[index + 1] = end.y;
                endPoints[index + 2] = end.z;

                isOutgoing[counter] = (current.inclination < 90);

                // Determine the min and max value. Later used in the different
                // visualizations for scaling.
                determineMinMax(current);

                counter++;
            }
        }

        return counter;
    }

}
