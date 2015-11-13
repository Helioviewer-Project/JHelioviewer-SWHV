package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.Vec2;

import com.jogamp.opengl.GL2;

public class CameraHelper {

    private static final double clipNear = Sun.Radius * 3;
    private static final double clipFar = Sun.Radius * 10000;

    public static Mat4 getOrthoMatrixInverse(Camera camera, double aspect) {
        double width = camera.getWidth();
        return Mat4.orthoInverse(-width * aspect, width * aspect, -width, width, clipNear, clipFar);
    }

    public static void applyPerspective(GL2 gl, Camera camera, double aspect) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        double width = camera.getWidth();
        gl.glOrtho(-width * aspect, width * aspect, -width, width, clipNear, clipFar);

        Vec2 translation = camera.getCurrentTranslation();
        Mat4 cameraTransformation = camera.getRotation().translate(translation.x, translation.y, -camera.getDistance());
        // applyCamera
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadMatrixd(cameraTransformation.m, 0);
    }

}
