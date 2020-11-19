package org.helioviewer.jhv.layers.connect;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.math.Vec3;

public class OrthoScaleList {

    private static final double radius = 1.01;

    public final List<Vec3> ortho;
    public final List<Vec3> scale;

    OrthoScaleList(List<Vec3> cart) {
        int size = cart.size();
        ortho = new ArrayList<>(size);
        scale = new ArrayList<>(size);

        cart.forEach(v -> {
            ortho.add(new Vec3(radius * v.x, radius * v.y, radius * v.z));
            scale.add(new Vec3(v.x, -v.y, v.z));
        });
    }

}
