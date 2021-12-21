package org.helioviewer.jhv.base.lut;

import java.awt.Color;

/**
 * Creates a gimp gradient segment with the given parameters
 *
 * @param leftStop      Position of left stoppoint
 * @param midStop       Position of middle stoppoint
 * @param rightStop     Position of right stoppoint
 * @param rl            R of left stop point
 * @param gl            G of left stop point
 * @param bl            B of left stop point
 * @param al            A of left stop point
 * @param rr            R of right stop point
 * @param gr            G of right stop point
 * @param br            B of right stop point
 * @param ar            A of right stop point
 * @param blendingType  Blending function type
 * @param blendingColor Blending function color
 */
record GimpGradientSegment(double leftStop, double midStop, double rightStop, double rl, double gl, double bl,
                           double al, double rr, double gr, double br, double ar, int blendingType, int blendingColor) {

    /**
     * Gives back the color for point x within this segment
     *
     * @param x Point in segment
     * @return Color according to gradient
     */
    int getGradientColor(double x) throws Exception {
        // Normalize the segment geometry.
        double mid = (midStop - leftStop) / (rightStop - leftStop);
        double pos = (x - leftStop) / (rightStop - leftStop);

        // Assume linear (most common, and needed by most others).
        double f = pos <= mid ? 0.5 * (pos / mid) : 0.5 * (pos - mid) / (1.0 - mid) + 0.5;

        // Find the correct interpolation factor.
        if (blendingType == 1) { // Curved
            f = Math.pow(pos, Math.log(.5) / Math.log(midStop));
        } else if (blendingType == 2) { // Sinusoidal
            f = (Math.sin(-Math.PI / 2 + Math.PI * f) + 1.0) / 2.0;
        } else if (blendingType == 3) { // Spherical increasing
            f -= 1.0;
            f = Math.sqrt(1.0 - f * f);
        } else if (blendingType == 4) { // Spherical decreasing
            f = 1.0 - Math.sqrt(1 - f * f);
        } else if (blendingType != 0) {
            throw new Exception("Unknown blending type " + blendingType + " for gimp gradient file");
        }

        // Ignore foreground/background stuff
        int r = 0;
        // Interpolate the colors
        if (blendingColor == 0) {
            r |= appD(rl + (rr - rl) * f) << 16;
            r |= appD(gl + (gr - gl) * f) << 8;
            r |= appD(bl + (br - bl) * f);
        } else {
            float[] lHSV = Color.RGBtoHSB(appD(rl), appD(gl), appD(bl), null);
            float[] rHSV = Color.RGBtoHSB(appD(rr), appD(gr), appD(br), null);
            // Making lshv the new color
            lHSV[1] += (float) ((rHSV[1] - lHSV[1]) * f);
            lHSV[2] += (float) ((rHSV[2] - lHSV[2]) * f);
            switch (blendingColor) {
                case 1:
                    if (lHSV[0] < rHSV[0]) {
                        lHSV[0] += (float) ((rHSV[0] - lHSV[0]) * f);
                    } else {
                        lHSV[0] += (float) ((1. - (lHSV[0] - rHSV[0])) * f);
                        if (lHSV[0] > 1.0)
                            lHSV[0]--;
                    }
                    break;
                case 2:
                    if (rHSV[0] < lHSV[0]) {
                        lHSV[0] -= (float) ((lHSV[0] - rHSV[0]) * f);
                    } else {
                        lHSV[0] -= (float) ((1. - (rHSV[0] - lHSV[0])) * f);
                        if (lHSV[0] < 0.0)
                            lHSV[0]++;
                    }
                    break;
                default:
                    throw new Exception("Unknown blending color " + blendingColor + " for gimp gradient file");
            }
            r = Color.HSBtoRGB(lHSV[0], lHSV[1], lHSV[1]);
        }
        // Set alpha value
        r |= appD(al + (ar - al) * f) << 24;
        return r;
    }

    /**
     * Internal function (approximate double) to set the colors
     *
     * @param x value
     * @return approximated as byte
     */
    private static int appD(double x) {
        return ((int) (x * 0xff)) & 0xff;
    }

}
