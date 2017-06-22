package org.helioviewer.jhv.renderable.components;

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.MathUtils;

class GridLabel {

    // height of text in solar radii
    static final float textScale = (float) (0.08 * Sun.Radius);

    private static final DecimalFormat formatter1 = MathUtils.numberFormatter("0", 1);

    final String txt;
    final float x;
    final float y;
    final float theta;

    private GridLabel(String _txt, float _x, float _y, float _theta) {
        txt = _txt;
        x = _x;
        y = _y;
        theta = _theta;
    }

    static ArrayList<GridLabel> makeRadialLabels(double delta, double radialStep) {
        double size = Sun.Radius;
        double horizontalAdjustment = textScale / 2.;
        double verticalAdjustment = textScale / 3.;

        ArrayList<GridLabel> labels = new ArrayList<>();
        for (double phi = 0; phi < 360; phi += radialStep) {
            double angle = -phi * Math.PI / 180. + delta;
            String txt = formatter1.format(phi);
            labels.add(new GridLabel(txt, (float) (Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
        }
        return labels;
    }

    static ArrayList<GridLabel> makeLatLabels(double latStep) {
        double size = Sun.Radius * 1.1;
        // adjust for font size in horizontal and vertical direction (centering the text approximately)
        double horizontalAdjustment = textScale / 2.;
        double verticalAdjustment = textScale / 3.;

        ArrayList<GridLabel> labels = new ArrayList<>();
        for (double phi = 0; phi <= 90; phi += latStep) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = formatter1.format(phi);

            labels.add(new GridLabel(txt, (float) (Math.sin(angle) * size),
                    (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            if (phi != 90) {
                labels.add(new GridLabel(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment),
                        (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            }
        }
        for (double phi = -latStep; phi >= -90; phi -= latStep) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = formatter1.format(phi);

            labels.add(new GridLabel(txt, (float) (Math.sin(angle) * size),
                    (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            if (phi != -90) {
                labels.add(new GridLabel(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment),
                        (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            }
        }
        return labels;
    }

    static ArrayList<GridLabel> makeLonLabels(RenderableGrid.GridType gridType, double lonStep) {
        double size = Sun.Radius * 1.05;

        ArrayList<GridLabel> labels = new ArrayList<>();
        for (double theta = 0; theta <= 180.; theta += lonStep) {
            double angle = (90 - theta) * Math.PI / 180.;
            String txt = formatter1.format(theta);
            labels.add(new GridLabel(txt, (float) (Math.cos(angle) * size), (float) (Math.sin(angle) * size), (float) theta));
        }
        for (double theta = -lonStep; theta > -180.; theta -= lonStep) {
            double angle = (90 - theta) * Math.PI / 180.;
            String txt = gridType == RenderableGrid.GridType.Carrington ? formatter1.format(theta + 360) : formatter1.format(theta);
            labels.add(new GridLabel(txt, (float) (Math.cos(angle) * size), (float) (Math.sin(angle) * size), (float) theta));
        }
        return labels;
    }

}
