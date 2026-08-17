package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.volume.VolumeData;

class GLSLVolumeShader extends GLSLShader {

    static final GLSLVolumeShader volume = new GLSLVolumeShader();

    private final float[] corner = new float[3];
    private final float[] dimensions = new float[3];
    private final float[] axisX = new float[3];
    private final float[] axisY = new float[3];
    private final float[] axisZ = new float[3];
    private final float[] rayDirection = new float[3];

    private int mvpRef;
    private int cornerRef;
    private int dimensionsRef;
    private int axisXRef;
    private int axisYRef;
    private int axisZRef;
    private int rayDirectionRef;
    private int opacityRef;
    private int cropMinRef;
    private int cropMaxRef;

    private GLSLVolumeShader() {
        super("/glsl/volume.vert", "/glsl/volume.frag");
    }

    static void init() {
        volume._init(false);
    }

    static void dispose() {
        volume._dispose();
    }

    @Override
    protected void initUniforms(int id) {
        mvpRef = GL.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        cornerRef = GL.glGetUniformLocation(id, "corner");
        dimensionsRef = GL.glGetUniformLocation(id, "dimensions");
        axisXRef = GL.glGetUniformLocation(id, "axisX");
        axisYRef = GL.glGetUniformLocation(id, "axisY");
        axisZRef = GL.glGetUniformLocation(id, "axisZ");
        rayDirectionRef = GL.glGetUniformLocation(id, "rayDirection");
        opacityRef = GL.glGetUniformLocation(id, "opacity");
        cropMinRef = GL.glGetUniformLocation(id, "cropMin");
        cropMaxRef = GL.glGetUniformLocation(id, "cropMax");
        setTextureUnit(id, "volume", GLTexture.Unit.THREE);
        setTextureUnit(id, "validityMask", GLTexture.Unit.TWO);
        setTextureUnit(id, "lut", GLTexture.Unit.ONE);
    }

    void bind(VolumeData data, double opacity, float[] cropMin, float[] cropMax) {
        set(corner, data.corner());
        set(axisX, data.axisX());
        set(axisY, data.axisY());
        set(axisZ, data.axisZ());
        dimensions[0] = data.width();
        dimensions[1] = data.height();
        dimensions[2] = data.depth();
        Transform.viewRayDirection(rayDirection);
        double worldX = rayDirection[0];
        double worldY = rayDirection[1];
        double worldZ = rayDirection[2];
        Vec3 x = data.axisX();
        Vec3 y = data.axisY();
        Vec3 z = data.axisZ();
        double determinant = data.determinant();
        rayDirection[0] = (float) ((worldX * (y.y * z.z - y.z * z.y) + worldY * (y.z * z.x - y.x * z.z) +
                worldZ * (y.x * z.y - y.y * z.x)) / determinant);
        rayDirection[1] = (float) ((worldX * (z.y * x.z - z.z * x.y) + worldY * (z.z * x.x - z.x * x.z) +
                worldZ * (z.x * x.y - z.y * x.x)) / determinant);
        rayDirection[2] = (float) ((worldX * (x.y * y.z - x.z * y.y) + worldY * (x.z * y.x - x.x * y.z) +
                worldZ * (x.x * y.y - x.y * y.x)) / determinant);

        GL.glUniformMatrix4fv(mvpRef, false, Transform.get());
        GL.glUniform3fv(cornerRef, corner);
        GL.glUniform3fv(dimensionsRef, dimensions);
        GL.glUniform3fv(axisXRef, axisX);
        GL.glUniform3fv(axisYRef, axisY);
        GL.glUniform3fv(axisZRef, axisZ);
        GL.glUniform3fv(rayDirectionRef, rayDirection);
        GL.glUniform1f(opacityRef, (float) opacity);
        GL.glUniform3fv(cropMinRef, cropMin);
        GL.glUniform3fv(cropMaxRef, cropMax);
    }

    private static void set(float[] target, Vec3 source) {
        target[0] = (float) source.x;
        target[1] = (float) source.y;
        target[2] = (float) source.z;
    }
}
