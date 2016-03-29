package org.helioviewer.jhv.camera.annotate;

import java.awt.Color;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Displayer.DisplayMode;
import org.helioviewer.jhv.renderable.components.RenderableGrid.GridChoiceType;

public abstract class AbstractAnnotateable implements Annotateable {

    private static final Color colorActive = Color.red;
    private static final Color colorDrag = Color.yellow;
    private static final Color colorBase = Color.blue;

    protected static final float[] activeColor = new float[] { colorActive.getRed() / 255f, colorActive.getGreen() / 255f, colorActive.getBlue() / 255f };
    protected static final float[] dragColor = new float[] { colorDrag.getRed() / 255f, colorDrag.getGreen() / 255f, colorDrag.getBlue() / 255f };
    protected static final float[] baseColor = new float[] { colorBase.getRed() / 255f, colorBase.getGreen() / 255f, colorBase.getBlue() / 255f };

    protected static final float lineWidth = 2;
    protected static final double radius = 1.01;

    protected final Camera camera;

    public AbstractAnnotateable(Camera _camera) {
        camera = _camera;
    }

    protected static Vec3 toSpherical(Vec3 p) {
        Vec3 pt = new Vec3();
        pt.x = p.length();
        pt.y = Math.acos(p.y / pt.x);
        pt.z = Math.atan2(p.x, p.z);

        return pt;
    }

    protected static Vec3 toCart(double x, double y, double z) {
        return new Vec3(x * Math.sin(y) * Math.sin(z),
                x * Math.cos(y),
                x * Math.sin(y) * Math.cos(z));
    }

    protected Vec3 computePoint(MouseEvent e) {
        Vec3 pt;
        if (Displayer.mode == DisplayMode.ORTHO) {
            pt = CameraHelper.getVectorFromSphere(camera, Displayer.getActiveViewport(), e.getPoint());
        }
        else {
            pt = GridScale.current.transformInverse(GridScale.current.mouseToGrid(e.getPoint(), Displayer.getActiveViewport(), camera, GridChoiceType.VIEWPOINT));
        }
        return pt;
    }
}
