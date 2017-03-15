package org.helioviewer.jhv.base.lut;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Representing a gimp gradient consisting of several GimpGradientSegments
 * 
 * Inspired by --- Read Gimp .ggr gradient files. Ned Batchelder,
 * http://nedbatchelder.com This code is in the public domain. --- and see in
 * gimp source code app/core/gimpgradient.c
 * 
 * @author Helge Dietert
 */
class GimpGradient {
    /**
     * List of segments uses in this gradient
     */
    private final List<GimpGradientSegment> segments;
    /**
     * Name from the gradient file
     */
    private final String name;

    /**
     * Give back the name
     * 
     * @return name form the file
     */
    public String getName() {
        return name;
    }

    /**
     * Interpolates the color according to the gradient
     * 
     * @param x
     *            point along the gradient scaled as [0,1)
     * @return rgb integer for that point
     * @throws GradientEvaluationError
     *             Error for evaluating the color
     */
    public int getGradientColor(double x) throws Exception {
        for (GimpGradientSegment s : segments) {
            if (s.leftStop <= x && x <= s.rightStop) {
                return s.getGradientColor(x);
            }
        }
        throw new Exception("Cannot find segment for point " + x);
    }

    /**
     * Creates a gimp gradient with the data from the stream expected in the
     * gimp gradient file format
     * 
     * @param ggr
     */
    public GimpGradient(BufferedReader ggr) throws Exception {
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

            String[] nL = ln.split(" ");
            // Either all exist or the last may not
            if (nL.length == 15)
                segments.add(new GimpGradientSegment(Double.parseDouble(nL[0]), Double.parseDouble(nL[1]), Double.parseDouble(nL[2]), Double.parseDouble(nL[3]), Double.parseDouble(nL[4]), Double.parseDouble(nL[5]), Double.parseDouble(nL[6]), Double.parseDouble(nL[7]), Double.parseDouble(nL[8]), Double.parseDouble(nL[9]), Double.parseDouble(nL[10]), Integer.parseInt(nL[11]), Integer.parseInt(nL[12])/*, Integer.parseInt(nL[13]), Integer.parseInt(nL[14])*/));
            else if (nL.length == 13)
                segments.add(new GimpGradientSegment(Double.parseDouble(nL[0]), Double.parseDouble(nL[1]), Double.parseDouble(nL[2]), Double.parseDouble(nL[3]), Double.parseDouble(nL[4]), Double.parseDouble(nL[5]), Double.parseDouble(nL[6]), Double.parseDouble(nL[7]), Double.parseDouble(nL[8]), Double.parseDouble(nL[9]), Double.parseDouble(nL[10]), Integer.parseInt(nL[11]), Integer.parseInt(nL[12])/*, 0, 0*/));
            else
                throw new Exception("Parsing error in segment " + i);
        }
    }

}
