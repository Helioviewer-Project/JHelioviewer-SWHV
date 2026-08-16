package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.opengl.model.ModelMaterial;

class GLSLMeshShader extends GLSLShader {

    static final GLSLMeshShader mesh = new GLSLMeshShader();

    private final float[] baseColor = new float[4];

    private int refModelViewProjectionMatrix;
    private int baseColorRef;
    private int hasBaseColorTextureRef;
    private int alphaModeRef;
    private int alphaCutoffRef;

    private GLSLMeshShader() {
        super("/glsl/mesh.vert", "/glsl/mesh.frag");
    }

    static void init() {
        mesh._init(false);
    }

    static void dispose() {
        mesh._dispose();
    }

    @Override
    protected void initUniforms(int id) {
        refModelViewProjectionMatrix = GL.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        baseColorRef = GL.glGetUniformLocation(id, "baseColor");
        hasBaseColorTextureRef = GL.glGetUniformLocation(id, "hasBaseColorTexture");
        alphaModeRef = GL.glGetUniformLocation(id, "alphaMode");
        alphaCutoffRef = GL.glGetUniformLocation(id, "alphaCutoff");
        setTextureUnit(id, "baseColorTexture", GLTexture.Unit.THREE);
    }

    void bind(ModelMaterial material, boolean hasBaseColorTexture) {
        baseColor[0] = material.red();
        baseColor[1] = material.green();
        baseColor[2] = material.blue();
        baseColor[3] = material.alpha();
        GL.glUniform4fv(baseColorRef, baseColor);
        GL.glUniform1i(hasBaseColorTextureRef, hasBaseColorTexture ? 1 : 0);
        GL.glUniform1i(alphaModeRef, switch (material.alphaMode()) {
            case OPAQUE -> 0;
            case MASK -> 1;
            case BLEND -> 2;
        });
        GL.glUniform1f(alphaCutoffRef, material.alphaCutoff());
        GL.glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
