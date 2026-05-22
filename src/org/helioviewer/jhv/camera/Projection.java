package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.Quat;

public final class Projection {

    private static final float CLIP_NARROW = (float) (32 * Sun.Radius); // bit more than LASCO C3
    private static final float CLIP_WIDE = (float) (50 * Sun.MeanEarthDistance); // bit further than Pluto

    private Projection() {}

    public static void ortho2D(double aspect, double width, double tx, double ty) {
        Transform.setup((float) (width * aspect), (float) width, -1, 1, (float) tx, (float) ty);
        Transform.cacheMVP();
    }

    public static void ortho(double aspect, double width, double tx, double ty, Quat rotation) {
        float clip = width < 32 ? CLIP_NARROW : CLIP_WIDE;
        Transform.setup((float) (width * aspect), (float) width, -clip, clip, (float) tx, (float) ty);
        Transform.rotateView(rotation);
        Transform.cacheMVP();
    }
}
