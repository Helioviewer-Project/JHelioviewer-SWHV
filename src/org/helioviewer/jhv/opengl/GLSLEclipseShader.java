package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

public final class GLSLEclipseShader extends GLSLScreenShader {

    private static final FloatBuffer values = BufferUtils.newFloatBuffer(4);
    private static final GLSLEclipseShader shader = new GLSLEclipseShader();

    private int diskRef;
    private int planeDistanceRef;
    private int rotationRef;
    private int colorRef;

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
    }

    public static void render(Vec3 direction, double distance, double sinRadiusSquared, Quat drag, float[] color) {
        shader.use();
        values.clear().put((float) direction.x).put((float) direction.y).put((float) direction.z).put((float) sinRadiusSquared).flip();
        GL.glUniform4fv(shader.diskRef, values);
        GL.glUniform1f(shader.planeDistanceRef, (float) distance);
        drag.setFloatBuffer(values.clear());
        GL.glUniform4fv(shader.rotationRef, values.flip());
        GL.glUniform4fv(shader.colorRef, color);
        shader.draw();
    }

}
