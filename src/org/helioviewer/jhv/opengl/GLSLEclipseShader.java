package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.math.Vec3;

public final class GLSLEclipseShader extends GLSLScreenShader {

    private static final FloatBuffer values = BufferUtils.newFloatBuffer(4);
    private static final GLSLEclipseShader shader = new GLSLEclipseShader();

    private int diskRef;
    private int planeDistanceRef;
    private int rotationRef;
    private int colorRef;
    private int hpcRef;

    private GLSLEclipseShader() {
        super("/glsl/eclipse.frag");
    }

    static void init() {
        shader._init();
    }

    static void dispose() {
        shader._dispose();
    }

    @Override
    protected void initUniforms(int id) {
        super.initUniforms(id);
        diskRef = requiredUniform(id, "disk");
        planeDistanceRef = requiredUniform(id, "planeDistance");
        rotationRef = requiredUniform(id, "rotation");
        colorRef = requiredUniform(id, "color");
        hpcRef = requiredUniform(id, "hpc");
    }

    public static void render(MapView mv, Vec3 direction, double sinRadiusSquared, float[] color) {
        shader.use();
        values.clear().put((float) direction.x).put((float) direction.y).put((float) direction.z).put((float) sinRadiusSquared).flip();
        GL.glUniform4fv(shader.diskRef, values);
        GL.glUniform1f(shader.planeDistanceRef, (float) mv.viewpoint().distance);
        mv.dragRotation().setFloatBuffer(values.clear());
        GL.glUniform4fv(shader.rotationRef, values.flip());
        GL.glUniform4fv(shader.colorRef, color);
        GL.glUniform1i(shader.hpcRef, mv.isHpc() ? 1 : 0);
        shader.draw();
    }

}
