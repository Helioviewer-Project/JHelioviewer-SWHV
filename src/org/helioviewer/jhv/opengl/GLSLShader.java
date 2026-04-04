package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;
import org.lwjgl.opengl.GL33;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.FileUtils;

abstract class GLSLShader {

    private enum ShaderType {
        vertex(GL33.GL_VERTEX_SHADER), fragment(GL33.GL_FRAGMENT_SHADER);

        final int glType;

        ShaderType(int _glType) {
            glType = _glType;
        }
    }

    private int progID;
    private int vertexID;
    private int fragmentID;

    private final String vertex;
    private final String fragment;

    GLSLShader(String _vertex, String _fragment) {
        vertex = _vertex;
        fragment = _fragment;
    }

    protected final void _init(GL3 gl, boolean common) {
        try {
            String vertexText = FileUtils.streamToString(FileUtils.getResource(vertex));
            vertexID = attachShader(gl, ShaderType.vertex, vertexText);

            String fragmentCommonText = common ? FileUtils.streamToString(FileUtils.getResource("/glsl/solarCommon.frag")) : "";
            String fragmentText = fragmentCommonText + FileUtils.streamToString(FileUtils.getResource(fragment));
            fragmentID = attachShader(gl, ShaderType.fragment, fragmentText);

            progID = initializeProgram(gl, true);
            use();
            initUniforms(gl, progID);
        } catch (Exception e) {
            throw new JHVGLException("Cannot load shader", e);
        }
    }

    protected final void _dispose(GL3 gl) {
        if (progID != 0) {
            GL33.glUseProgram(0);
        }
        if (vertexID != 0) {
            GL33.glDeleteShader(vertexID);
            vertexID = 0;
        }
        if (fragmentID != 0) {
            GL33.glDeleteShader(fragmentID);
            fragmentID = 0;
        }
        if (progID != 0) {
            GL33.glDeleteProgram(progID);
            progID = 0;
        }
    }

    public final void use() {
        GL33.glUseProgram(progID);
    }

    protected abstract void initUniforms(GL3 gl, int id);

    protected static void setTextureUnit(GL3 gl, int id, String texname, GLTexture.Unit unit) {
        int loc = GL33.glGetUniformLocation(id, texname);
        if (loc != -1)
            GL33.glUniform1i(loc, unit.ordinal());
        else
            Log.error("Invalid texture " + texname);
    }

    private static int attachShader(GL3 gl, ShaderType type, String text) {
        int id = GL33.glCreateShader(type.glType);
        GL33.glShaderSource(id, text);
        GL33.glCompileShader(id);

        int compileStatus = GL33.glGetShaderi(id, GL33.GL_COMPILE_STATUS);
        if (compileStatus != 1) {
            Log.error("Shader compile status: " + compileStatus);
            int infoLogLength = GL33.glGetShaderi(id, GL33.GL_INFO_LOG_LENGTH);
            if (infoLogLength > 0) {
                String log = GL33.glGetShaderInfoLog(id, infoLogLength);
                Log.error(log);
                throw new JHVGLException("Cannot compile " + type + " shader: " + log);
            } else
                throw new JHVGLException("Cannot compile " + type + " shader: unknown reason");
        }
        return id;
    }

    private int initializeProgram(GL3 gl, boolean cleanUp) {
        int id = GL33.glCreateProgram();
        GL33.glAttachShader(id, vertexID);
        GL33.glAttachShader(id, fragmentID);
        GL33.glLinkProgram(id);

        int linkStatus = GL33.glGetProgrami(id, GL33.GL_LINK_STATUS);
        if (linkStatus != 1) {
            Log.error("Shader link status: " + linkStatus);
            int infoLogLength = GL33.glGetProgrami(id, GL33.GL_INFO_LOG_LENGTH);
            if (infoLogLength > 0) {
                String log = GL33.glGetProgramInfoLog(id, infoLogLength);
                Log.error(log);
                throw new JHVGLException("Cannot link shader: " + log);
            } else
                throw new JHVGLException("Cannot link shader: unknown reason");
        }

        GL33.glValidateProgram(id);

        if (cleanUp) {
            GL33.glDetachShader(id, vertexID);
            GL33.glDeleteShader(vertexID);
            vertexID = 0;
            GL33.glDetachShader(id, fragmentID);
            GL33.glDeleteShader(fragmentID);
            fragmentID = 0;
        }
        return id;
    }

}
