package org.helioviewer.jhv.base.lut;

import java.awt.Color;
import java.io.BufferedReader;
import java.util.ArrayList;

import org.helioviewer.jhv.base.Regex;

// Represent a Gimp gradient consisting of several Segments
class GimpGradient {

    private final ArrayList<Segment> segments;
    private final String name;

    String getName() {
        return name;
    }

    // Interpolate color according to the gradient
    int getGradientColor(double x) throws Exception {
        for (Segment s : segments) {
            if (s.leftStop() <= x && x <= s.rightStop()) {
                return s.getGradientColor(x);
            }
        }
        throw new Exception("Cannot find segment for point " + x);
    }

    // Create Gimp gradient with the data from the stream expected in the
    // Gimp gradient file format
    GimpGradient(BufferedReader ggr) throws Exception {
        String ln = ggr.readLine();
        if (!"GIMP Gradient".equals(ln))
            throw new Exception("Not a GIMP gradient file");

        ln = ggr.readLine();
        if (ln == null || !ln.startsWith("Name: "))
            throw new Exception("Not a GIMP gradient file");
        name = ln.substring(6);

        ln = ggr.readLine();
        int nSeg;
        if (ln == null || (nSeg = Integer.parseInt(ln)) <= 0)
            throw new Exception("GIMP gradient file parsing error");

        segments = new ArrayList<>(nSeg);
        for (int i = 0; i < nSeg; i++) {
            ln = ggr.readLine();
            if (ln == null)
                throw new Exception("EOF reached before reading all segments");

            String[] nL = Regex.Space.split(ln);
            // Either all exist or the last may not
            if (nL.length == 13 || nL.length == 15) {
                segments.add(new Segment(Double.parseDouble(nL[0]), Double.parseDouble(nL[1]), Double.parseDouble(nL[2]), Double.parseDouble(nL[3]), Double.parseDouble(nL[4]), Double.parseDouble(nL[5]), Double.parseDouble(nL[6]), Double.parseDouble(nL[7]), Double.parseDouble(nL[8]), Double.parseDouble(nL[9]), Double.parseDouble(nL[10]), Integer.parseInt(nL[11]), Integer.parseInt(nL[12])/*, Integer.parseInt(nL[13]), Integer.parseInt(nL[14])*/));
            } else
                throw new Exception("Parsing error in segment " + i);
        }
    }

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
    private record Segment(double leftStop, double midStop, double rightStop, double rl, double gl,
                           double bl, double al, double rr, double gr, double br, double ar,
                           int blendingType, int blendingColor) {
        /**
         * Gives back the color for point x within this segment
         *
         * @param x Point in segment
         * @return Color according to gradient
         */
        int getGradientColor(double x) throws Exception {
            // Normalize the segment geometry.
            double f = getF(x);
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
                    case 1 -> {
                        if (lHSV[0] < rHSV[0]) {
                            lHSV[0] += (float) ((rHSV[0] - lHSV[0]) * f);
                        } else {
                            lHSV[0] += (float) ((1. - (lHSV[0] - rHSV[0])) * f);
                            if (lHSV[0] > 1.0)
                                lHSV[0]--;
                        }
                    }
                    case 2 -> {
                        if (rHSV[0] < lHSV[0]) {
                            lHSV[0] -= (float) ((lHSV[0] - rHSV[0]) * f);
                        } else {
                            lHSV[0] -= (float) ((1. - (rHSV[0] - lHSV[0])) * f);
                            if (lHSV[0] < 0.0)
                                lHSV[0]++;
                        }
                    }
                    default ->
                            throw new Exception("Unknown blending color " + blendingColor + " for gimp gradient file");
                }
                r = Color.HSBtoRGB(lHSV[0], lHSV[1], lHSV[2]);
            }
            // Set alpha value
            r |= appD(al + (ar - al) * f) << 24;
            return r;
        }

        private double getF(double x) throws Exception {
            double mid = (midStop - leftStop) / (rightStop - leftStop);
            double pos = (x - leftStop) / (rightStop - leftStop);
            // Assume linear (most common, and needed by most others)
            double f = pos <= mid ? 0.5 * (pos / mid) : 0.5 * (pos - mid) / (1.0 - mid) + 0.5;
            // Find the correct interpolation factor
            return switch (blendingType) {
                case 0 -> f;
                case 1 -> Math.pow(pos, Math.log(0.5) / Math.log(midStop)); // Curved
                case 2 -> (Math.sin(-Math.PI / 2 + Math.PI * f) + 1) / 2; // Sinusoidal
                case 3 -> Math.sqrt(1 - (f - 1) * (f - 1)); // Spherical increasing
                case 4 -> 1 - Math.sqrt(1 - f * f); // Spherical decreasing
                default -> throw new Exception("Unknown blending type " + blendingType + " for gimp gradient file");
            };
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

}
