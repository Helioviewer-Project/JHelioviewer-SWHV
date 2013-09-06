package ch.fhnw.jhv.plugins.vectors.data;

/**
 * VectorField Utils class provides some helper methods
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class VectorFieldUtils {

    /**
     * Copy VectorField
     * 
     * @param srcVectorField
     *            source VectorField
     * @param destVectorField
     *            destination VectorField
     * 
     */
    public static void copyVectorField(VectorField srcVectorField, VectorField destVectorField) {

        int i = 0;

        // initialize the vectors array
        destVectorField.vectors = new VectorData[1][(int) srcVectorField.sizePixel.x * (int) srcVectorField.sizePixel.y];

        // Deep clone of the VectorData Objects
        for (VectorData data : srcVectorField.vectors[0]) {
            destVectorField.vectors[0][i] = data.clone();
            i++;
        }
    }
}
