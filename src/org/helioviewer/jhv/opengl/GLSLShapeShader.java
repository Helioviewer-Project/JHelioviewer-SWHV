package org.helioviewer.jhv.opengl;

class GLSLShapeShader extends GLSLShader {

    static final GLSLShapeShader point = new GLSLShapeShader("/glsl/point.vert", "/glsl/point.frag");
    static final GLSLShapeShader shape = new GLSLShapeShader("/glsl/shape.vert", "/glsl/shape.frag");

    private int refModelViewProjectionMatrix;
    private int factorRef;

    private GLSLShapeShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init() {
        point._init(false);
        shape._init(false);
    }

    public static void dispose() {
        point._dispose();
        shape._dispose();
    }

    @Override
    protected void initUniforms(int id) {
        refModelViewProjectionMatrix = GL.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        factorRef = GL.glGetUniformLocation(id, "factor");
    }

    void bindParams(double _factor) {
        GL.glUniform1f(factorRef, (float) _factor);
    }

    void bindMVP() {
        GL.glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
