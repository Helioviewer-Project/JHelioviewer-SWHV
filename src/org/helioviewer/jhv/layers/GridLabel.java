package org.helioviewer.jhv.layers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.scale.GridType;
import org.helioviewer.jhv.math.MathUtils;
import org.joml.Matrix4f;

class GridLabel {

    // height of text in solar radii
    static final float textScale = (float) (0.06 * Sun.Radius);

    private static final DecimalFormat formatter1 = MathUtils.numberFormatter("0", 1);
    private static final Matrix4f identity = new Matrix4f();

    final String txt;
    final float x;
    final float y;
    final Matrix4f m;

    private GridLabel(String _txt, float _x, float _y, Matrix4f _m) {
        txt = _txt;
        x = _x;
        y = _y;
        m = _m;
    }

    static List<GridLabel> makeRadialLabels(double delta, double radialStep) {
        double size = Sun.Radius;
        double horizontalAdjustment = textScale / 2.;
        double verticalAdjustment = textScale / 3.;

        ArrayList<GridLabel> labels = new ArrayList<>();
        for (double phi = 0; phi < 360; phi += radialStep) {
            String txt = formatter1.format(phi);
            double angle = -phi * Math.PI / 180. + delta;
            float x = (float) (Math.sin(angle) * size - horizontalAdjustment);
            float y = (float) (Math.cos(angle) * size - verticalAdjustment);
            labels.add(new GridLabel(txt, x, y, identity));
        }
        return labels;
    }

    static List<GridLabel> makeLatLabels(double latStep) {
        double size = Sun.Radius * 1.1;
        // adjust for font size in horizontal and vertical direction (centering the text approximately)
        double horizontalAdjustment = textScale / 2.;
        double verticalAdjustment = textScale / 3.;

        ArrayList<GridLabel> labels = new ArrayList<>();
        for (double phi = 0; phi <= 90; phi += latStep) {
            String txt = formatter1.format(phi);
            double angle = (90 - phi) * Math.PI / 180.;
            float x = (float) (Math.sin(angle) * size);
            float y = (float) (Math.cos(angle) * size - verticalAdjustment);

            labels.add(new GridLabel(txt, x, y, identity));
            if (phi != 90) {
                x = (float) (-Math.sin(angle) * size - horizontalAdjustment);
                labels.add(new GridLabel(txt, x, y, identity));
            }
        }
        for (double phi = -latStep; phi >= -90; phi -= latStep) {
            String txt = formatter1.format(phi);
            double angle = (90 - phi) * Math.PI / 180.;
            float x = (float) (Math.sin(angle) * size);
            float y = (float) (Math.cos(angle) * size - verticalAdjustment);

            labels.add(new GridLabel(txt, x, y, identity));
            if (phi != -90) {
                x = (float) (-Math.sin(angle) * size - horizontalAdjustment);
                labels.add(new GridLabel(txt, x, y, identity));
            }
        }
        return labels;
    }

    static List<GridLabel> makeLonLabels(GridType gridType, double lonStep) {
        double size = Sun.Radius * 1.05;

        ArrayList<GridLabel> labels = new ArrayList<>();
        for (double theta = 0; theta <= 180.; theta += lonStep) {
            String txt = formatter1.format(theta);
            double angle = (90 - theta) * Math.PI / 180.;
            float x = (float) (Math.cos(angle) * size);
            float y = (float) (Math.sin(angle) * size);

            Matrix4f m = new Matrix4f();
            m.translation(x, 0, y);
            m.rotateTranslation((float) (theta * Math.PI / 180.), 0, 1, 0, m);
            labels.add(new GridLabel(txt, x, y, m));
        }
        for (double theta = -lonStep; theta > -180.; theta -= lonStep) {
            String txt = gridType == GridType.Carrington ? formatter1.format(theta + 360) : formatter1.format(theta);
            double angle = (90 - theta) * Math.PI / 180.;
            float x = (float) (Math.cos(angle) * size);
            float y = (float) (Math.sin(angle) * size);

            Matrix4f m = new Matrix4f();
            m.translation(x, 0, y);
            m.rotateTranslation((float) (theta * Math.PI / 180.), 0, 1, 0, m);
            labels.add(new GridLabel(txt, x, y, m));
        }
        return labels;
    }

}
