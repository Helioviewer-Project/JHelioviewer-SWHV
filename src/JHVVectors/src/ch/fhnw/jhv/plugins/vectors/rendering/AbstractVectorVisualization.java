package ch.fhnw.jhv.plugins.vectors.rendering;

import java.util.List;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import ch.fhnw.jhv.gui.viewport.components.SunRenderPlugin;
import ch.fhnw.jhv.helper.MathHelper;
import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin.RenderPluginType;
import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;

/**
 * 
 * Basic class for Vector Visualization. This class provides the calculation of
 * a 3d vector based on its parameters
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * @date
 */
public abstract class AbstractVectorVisualization implements VectorVisualization {

    /**
     * Storage of all startpoints of the vectors They are stored in float arrays
     * to reduce overhead of Vector3f
     */
    protected float[] startPoints;

    /**
     * Storage of all endpoints of the vectors They are stored in float arrays
     * to reduce overhead of Vector3f
     */
    protected float[] endPoints;

    /**
     * All vectors that are rendered
     */
    protected VectorData[] vectors;

    /**
     * Is the vector an outgoing vector
     */
    protected boolean isOutgoing[];

    /**
     * Min Strength of all vectors
     */
    protected float minStrength;

    /**
     * Max Strength of all vectors
     */
    protected float maxStrength;

    /**
     * Precalculates all the Start and Endpoints of all the Vectors. the method
     * decides wether to use calculation on sun or on sphere. Note that Start
     * and Endpoint are independent of the vectors direction (outgoing or not)
     * Startpoint is the point on the sun, or on the plane. The resulting points
     * are stored in the protected float arrays of this class (startPoints,
     * endPoints)
     * 
     * @param vectorsCurrentFrame
     *            List of all Vectors
     * @param field
     *            Original Vectorfield
     * @return Number of vectors that were not null inside vectorsCurrentFrame
     */
    public int calculateStartEndPoints(List<VectorData> vectorsCurrentFrame, VectorField field) {

        int size = getSize(vectorsCurrentFrame);

        minStrength = Float.MAX_VALUE;
        maxStrength = Float.MIN_VALUE;

        if (startPoints == null || startPoints.length != 3 * size) {
            startPoints = new float[3 * size];
            endPoints = new float[3 * size];
            isOutgoing = new boolean[size];
            vectors = new VectorData[size];
        }

        if (PluginManager.getInstance().getRenderPluginByType(RenderPluginType.SUN) != null) {
            return calculateOnSphere(vectorsCurrentFrame, field);
        } else {
            return calculateOnPlane(vectorsCurrentFrame, field);
        }

    }

    /**
     * Precalculate Start and Endpoints on the Sun.
     * 
     * @param vectorsCurrentFrame
     *            List of all Vectors
     * @param field
     *            Original Vectorfield
     * @return Number of vectors that were not null inside vectorsCurrentFrame
     */
    protected int calculateOnSphere(List<VectorData> vectorList, VectorField field) {
        Vector3f start = new Vector3f();
        Vector3f end = new Vector3f();

        Matrix4f rotationY = new Matrix4f();
        Matrix4f rotationX = new Matrix4f();
        Matrix4f rotationPosition = new Matrix4f();

        Matrix4f rotationVector = new Matrix4f();
        Matrix4f rotationAzimuth = new Matrix4f();
        Matrix4f rotationInclination = new Matrix4f();

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

                // create rotation-matrix for positioning of current vector
                rotationAzimuth.rotZ(current.azimuth * MathHelper.DEG2RAD);
                if (current.inclination > 90)
                    rotationInclination.rotX(-((180 - current.inclination) + 90.0f) * MathHelper.DEG2RAD);
                else
                    rotationInclination.rotX(-(90.0f - current.inclination) * MathHelper.DEG2RAD);

                rotationVector.set(rotationPosition);
                rotationVector.mul(rotationAzimuth);
                rotationVector.mul(rotationInclination);

                // transform start position
                start.set(0, 0, SunRenderPlugin.SUN_RADIUS);
                rotationPosition.transform(start);

                // create end point of vector
                end.set(0, -current.length, 0);
                rotationVector.transform(end);
                end.add(start, end);

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
     * Precalculate Start and Endpoints on the sun
     * 
     * @param vectorsCurrentFrame
     *            List of all Vectors
     * @param field
     *            Original Vectorfield
     * @return Number of vectors that were not null inside vectorsCurrentFrame
     */
    protected int calculateOnPlane(List<VectorData> vectorsList, VectorField field) {
        Vector3f start = new Vector3f();
        Vector3f end = new Vector3f();

        Matrix4f rotationVector = new Matrix4f();
        Matrix4f rotationAzimuth = new Matrix4f();
        Matrix4f rotationInclination = new Matrix4f();

        float xTranslation = field.sizePixel.x * PlaneRenderPlugin.PLANE_SCALE * 0.5f + (0.5f * PlaneRenderPlugin.PLANE_SCALE);
        float yTranslation = field.sizePixel.y * PlaneRenderPlugin.PLANE_SCALE * 0.5f + (0.5f * PlaneRenderPlugin.PLANE_SCALE);

        int counter = 0;

        for (int i = 0; i < vectorsList.size(); i++) {
            if (vectorsList.get(i) != null) {
                int index = counter * 3;
                VectorData current = vectorsList.get(i);
                vectors[counter] = current;

                // create rotation-matrix for positioning of current vector
                rotationAzimuth.rotY(current.azimuth * MathHelper.DEG2RAD);
                if (current.inclination > 90)
                    rotationInclination.rotX(-((180 - current.inclination) + 90.0f) * MathHelper.DEG2RAD);
                else
                    rotationInclination.rotX(-(90.0f - current.inclination) * MathHelper.DEG2RAD);

                rotationVector.setIdentity();
                rotationVector.mul(rotationAzimuth);
                rotationVector.mul(rotationInclination);

                // transform start position
                start.set(current.x * PlaneRenderPlugin.PLANE_SCALE - xTranslation, 0, current.y * PlaneRenderPlugin.PLANE_SCALE - yTranslation);
                // create end point of vector
                end.set(0, 0, current.length);
                rotationVector.transform(end);
                end.add(start, end);

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
     * Determine the min and max value
     * 
     * @param VectorData
     *            vector
     */
    protected void determineMinMax(VectorData vector) {
        if (minStrength > vector.length)
            minStrength = vector.length;

        if (maxStrength < vector.length)
            maxStrength = vector.length;
    }

    /**
     * Returns the count of VectorData Objects that are NOT NULL in the given
     * list
     * 
     * @param vectors
     *            List of VectorDatas to be counted
     * @return count of elements that are not null in the given list
     */
    protected int getSize(List<VectorData> vectors) {
        int size = 0;

        for (VectorData vector : vectors) {
            if (vector != null) {
                size++;
            }
        }

        return size;
    }
}
