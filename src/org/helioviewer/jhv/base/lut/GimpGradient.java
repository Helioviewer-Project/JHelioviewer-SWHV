package org.helioviewer.jhv.base.lut;

import java.io.BufferedReader;
import java.util.ArrayList;

import org.helioviewer.jhv.base.Regex;

// Represent a Gimp gradient consisting of several GimpGradientSegments
class GimpGradient {

    private final ArrayList<GimpGradientSegment> segments;
    private final String name;

    String getName() {
        return name;
    }

    // Interpolate color according to the gradient
    int getGradientColor(double x) throws Exception {
        for (GimpGradientSegment s : segments) {
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
                segments.add(new GimpGradientSegment(Double.parseDouble(nL[0]), Double.parseDouble(nL[1]), Double.parseDouble(nL[2]), Double.parseDouble(nL[3]), Double.parseDouble(nL[4]), Double.parseDouble(nL[5]), Double.parseDouble(nL[6]), Double.parseDouble(nL[7]), Double.parseDouble(nL[8]), Double.parseDouble(nL[9]), Double.parseDouble(nL[10]), Integer.parseInt(nL[11]), Integer.parseInt(nL[12])/*, Integer.parseInt(nL[13]), Integer.parseInt(nL[14])*/));
            } else
                throw new Exception("Parsing error in segment " + i);
        }
    }

}
