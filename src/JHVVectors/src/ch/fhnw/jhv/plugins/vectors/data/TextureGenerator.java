/**
 * 
 */
package ch.fhnw.jhv.plugins.vectors.data;

import java.awt.image.BufferedImage;

/**
 * TextureGenerator
 * 
 * Creates a texture out of a given vectorfield. Range of the color values are
 * between 0 .. 255.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class TextureGenerator {

    /**
     * The original vectorfield
     */
    private VectorField vectorField;

    /**
     * Existing max strength
     */
    private float maxStrength;

    /**
     * Existing min strength
     */
    private float minStrength;

    /**
     * Max possible value after scale
     */
    private float toMax = 255;

    /**
     * Min possible value after scale
     */
    private float toMin = 0;

    /**
     * Constructor
     * 
     * @param vectorField
     */
    public TextureGenerator() {
    }

    /**
     * Determine min/max value of all strengths. Later used for a correct
     * scaling.
     * 
     * @param time
     *            when (on what time dimension)
     */
    private void determineMinMaxStrength(int time) {

        maxStrength = Float.MIN_VALUE;
        minStrength = Float.MAX_VALUE;

        // iterate over the current time
        for (VectorData data : vectorField.vectors[time]) {

            // set minStrength
            if (data.length < minStrength)
                minStrength = data.length;

            // set maxStrengh
            if (data.length > maxStrength)
                maxStrength = data.length;
        }
    }

    /**
     * Create the texture as a BufferedImage
     * 
     * @param time
     *            when (on what time dimension)
     * 
     * @return BufferedImage
     */
    public BufferedImage receiveTexture(int time) {

        BufferedImage image = null;

        image = new BufferedImage((int) vectorField.sizePixel.x, (int) vectorField.sizePixel.y, BufferedImage.TYPE_INT_ARGB);

        System.out.println(time);

        // Set max/min strength. Later used for correct scaling.
        determineMinMaxStrength(time);

        for (VectorData vector : vectorField.vectors[time]) {

            boolean isOutgoing = false;

            // check if the vectors is an outgoing or ingoing vector
            if (vector.inclination > 0 && vector.inclination <= 90) {
                isOutgoing = true;
            } else {
                isOutgoing = false;
            }

            /*
             * outgoing <----------> incoming
             * ||--------------------|--------------------|| black gray white 0
             * 128 255
             */

            // calculate the scaled value which should be between 0 .. 255
            float scaledValue = ((vector.length - minStrength) * (toMax - toMin) / (maxStrength - minStrength) + toMin) / 2;

            if (isOutgoing) {
                scaledValue = 128 - scaledValue;
            } else {
                scaledValue = 128 + scaledValue;
            }

            // calculate the AGBR bit value which is later set in the
            // BufferedImage
            int bitValue = (0xFF << 24) | ((int) scaledValue << 16) | ((int) scaledValue << 8) | ((int) scaledValue);

            // set the AGBR bit value at the right position (x,y)
            image.setRGB((int) vector.x, (int) vector.y, bitValue);
        }

        return image;
    }

    /**
     * Setter for vectorField
     * 
     * @param vf
     *            VectorField
     */
    public void setVectorField(VectorField vf) {
        this.vectorField = vf;
    }
}
