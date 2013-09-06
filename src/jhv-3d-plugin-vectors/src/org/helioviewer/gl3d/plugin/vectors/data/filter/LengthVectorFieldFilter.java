package org.helioviewer.gl3d.plugin.vectors.data.filter;

import org.helioviewer.gl3d.plugin.vectors.data.VectorData;
import org.helioviewer.gl3d.plugin.vectors.data.VectorField;

public class LengthVectorFieldFilter extends VectorFieldFilter {

    protected void applyFilter(VectorField vectorField) {
        double minimalLength = extractMinimalLength(vectorField);

        for (int t = 0; t < vectorField.vectors.length; t++) {
            VectorData[] vectors = vectorField.vectors[t];

            for (int i = 0; i < vectors.length; i++) {
                VectorData vd = vectors[i];
                if (vd.length < minimalLength) {
                    // null out if length is not great enough
                    vectors[i] = null;
                }
            }
        }
    }

    private double extractMinimalLength(VectorField vectorField) {
        double l = 0.0;
        int counter = 0;

        for (int t = 0; t < vectorField.vectors.length; t++) {
            VectorData[] vectors = vectorField.vectors[t];

            for (int i = 0; i < vectors.length; i++) {
                VectorData vd = vectors[i];
                l += vd.length;
                counter++;
            }
        }
        return l / counter;
    }
}
