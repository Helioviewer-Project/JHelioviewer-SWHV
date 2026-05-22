package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Viewport;

class GLSLLineShader extends GLSLShader {

    static final GLSLLineShader line = new GLSLLineShader("/glsl/line.vert", "/glsl/line.frag");

    private static GLBO screenBO;
    private static final FloatBuffer screenBuf = BufferUtils.newFloatBuffer(16 + 4 + 4);
    private static final int SCREEN_SIZE = screenBuf.capacity() * 4;

    private GLSLLineShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init() {
        screenBO = new GLBO(GL.UNIFORM_BUFFER, GL.STREAM_DRAW);
        line._init(false);
    }

    public static void dispose() {
        line._dispose();
        screenBO.delete();
    }

    @Override
    protected void initUniforms(int id) {
        setupUBO(id, "ScreenBlock", screenBO.getID(), UBO.LINE_SCREEN);
    }

    void bindParams(Viewport vp, double _thickness) {
        FloatBuffer mvp = Transform.get();
        screenBuf.put(mvp);
        mvp.flip();
        screenBuf.put(vp.glslArray).put((float) (0.5 * _thickness)); // +3 floats padding
        screenBO.setBufferData(screenBuf.flip().limit(), SCREEN_SIZE, screenBuf); // always changes
    }

}
