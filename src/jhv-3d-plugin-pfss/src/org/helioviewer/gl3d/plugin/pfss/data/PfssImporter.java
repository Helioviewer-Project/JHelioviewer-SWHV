package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3f;

/**
 * Importer for Files which are generated from PFSS
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class PfssImporter {

    /**
     * Curve precision
     */
    private int curvePrecision = 15;

    /**
     * Parse PFSS file and store the data in the PfssDimension Datastructure.
     * 
     * @param filename
     *            filename of the PFSS File
     * 
     * @return PfssDimension pfssDimension Datastructure
     * 
     * @throws IOException
     * @throws Exception
     */
    public PfssDimension readPfssExport(String filename) throws IncorrectPfssFileException, IOException {

        PfssDimension dimension = new PfssDimension(0, 0);
        FileReader filereader = new FileReader(filename);
        BufferedReader reader = new BufferedReader(filereader);

        String line = null;
        boolean newCurve = true;

        PfssCurve curve = null;

        int skip = curvePrecision;
        int count = 0;

        try {
            while ((line = reader.readLine()) != null) {

                if (newCurve) {

                    // get the color
                    ArrayList<String> values = new ArrayList<String>(Arrays.asList(line.split(" ")));

                    Iterator<String> iter = values.iterator();

                    while (iter.hasNext()) {
                        String val = iter.next();

                        if (val.equals("")) {
                            iter.remove();
                        }
                    }

                    curve = new PfssCurve();
                    // curve.points = new ArrayList<GL3DVec3d>();
                    curve.color = new GL3DVec3f(Float.parseFloat(values.get(0)), Float.parseFloat(values.get(1)), Float.parseFloat(values.get(2)));

                    curve.color.x /= 255;
                    curve.color.y /= 255;
                    curve.color.z /= 255;

                    newCurve = false;
                } else {
                    // get all the other points
                    ArrayList<String> values = new ArrayList<String>(Arrays.asList(line.split(" ")));

                    Iterator<String> iter = values.iterator();

                    while (iter.hasNext()) {
                        String val = iter.next();

                        if (val.equals("")) {
                            iter.remove();
                        }
                    }

                    // new curve is probably coming
                    if (values.get(0).equals("-") || values.get(0).equals("")) {
                        // add curve
                        dimension.curves.add(curve);

                        // new curve is starting
                        newCurve = true;
                    } else {
                        if (count-- == 0) {
                            // add the curve point
                            GL3DVec3d point = new GL3DVec3d(Double.parseDouble(values.get(0)), Double.parseDouble(values.get(1)), Double.parseDouble(values.get(2)));

                            double z = point.y;
                            point.y = point.z;
                            point.z = z;

                            // curve.points.add(point);
                            count = skip;
                        }
                    }
                }
            }
        } catch (NumberFormatException number) {
            throw new IncorrectPfssFileException(number);
        }

        return dimension;
    }

    /**
     * Read a list of PFSS files and store it in a ArrayList<PfssDimension>
     * 
     * @param files
     *            List<String>
     * @param curvePrecision
     *            precision of the curve
     * 
     * @return List<PfssDimension> list of the loaded pfss dimensions
     * 
     * @throws IncorrectPfssFileException
     * @throws IOException
     */
    public List<PfssDimension> readPfssExport(List<String> files, int curvePrecision) throws IncorrectPfssFileException, IOException {
        this.curvePrecision = curvePrecision;
        ArrayList<PfssDimension> dimensions = new ArrayList<PfssDimension>(files.size());

        for (String file : files) {
            dimensions.add(this.readPfssExport(file));
        }

        return dimensions;
    }
}
