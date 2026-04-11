package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.FileUtils;

abstract class GLSLShader {

    private enum ShaderType {
        vertex(GL.VERTEX_SHADER), fragment(GL.FRAGMENT_SHADER);

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

    protected final void _init(boolean common) {
        try {
            String vertexText = GL.adaptShaderSource(FileUtils.streamToString(FileUtils.getResource(vertex)), true);
            vertexID = attachShader(ShaderType.vertex, vertexText);

            String fragmentCommonText = common ? FileUtils.streamToString(FileUtils.getResource("/glsl/solarCommon.frag")) : "";
            String fragmentText = GL.adaptShaderSource(fragmentCommonText + FileUtils.streamToString(FileUtils.getResource(fragment)), false);
            fragmentID = attachShader(ShaderType.fragment, fragmentText);

            progID = initializeProgram(true);
            use();
            initUniforms(progID);
        } catch (Exception e) {
            throw new JHVGLException("Cannot load shader", e);
        }
    }

    protected final void _dispose() {
        if (progID != 0) {
            GL.glUseProgram(0);
        }
        if (vertexID != 0) {
            GL.glDeleteShader(vertexID);
            vertexID = 0;
        }
        if (fragmentID != 0) {
            GL.glDeleteShader(fragmentID);
            fragmentID = 0;
        }
        if (progID != 0) {
            GL.glDeleteProgram(progID);
            progID = 0;
        }
    }

    public final void use() {
        GL.glUseProgram(progID);
    }

    protected abstract void initUniforms(int id);

    protected static void setTextureUnit(int id, String texname, GLTexture.Unit unit) {
        int loc = GL.glGetUniformLocation(id, texname);
        if (loc != -1)
            GL.glUniform1i(loc, unit.ordinal());
        else
            Log.error("Invalid texture " + texname);
    }

    private static int attachShader(ShaderType type, String text) {
        int id = GL.glCreateShader(type.glType);
        GL.glShaderSource(id, text);
        GL.glCompileShader(id);

        int compileStatus = GL.glGetShaderi(id, GL.COMPILE_STATUS);
        if (compileStatus != 1) {
            Log.error("Shader compile status: " + compileStatus);
            int infoLogLength = GL.glGetShaderi(id, GL.INFO_LOG_LENGTH);
            if (infoLogLength > 0) {
                String log = GL.glGetShaderInfoLog(id, infoLogLength);
                Log.error(log);
                throw new JHVGLException("Cannot compile " + type + " shader: " + log);
            } else
                throw new JHVGLException("Cannot compile " + type + " shader: unknown reason");
        }
        return id;
    }

    private int initializeProgram(boolean cleanUp) {
        int id = GL.glCreateProgram();
        GL.glAttachShader(id, vertexID);
        GL.glAttachShader(id, fragmentID);
        GL.glLinkProgram(id);

        int linkStatus = GL.glGetProgrami(id, GL.LINK_STATUS);
        if (linkStatus != 1) {
            Log.error("Shader link status: " + linkStatus);
            int infoLogLength = GL.glGetProgrami(id, GL.INFO_LOG_LENGTH);
            if (infoLogLength > 0) {
                String log = GL.glGetProgramInfoLog(id, infoLogLength);
                Log.error(log);
                throw new JHVGLException("Cannot link shader: " + log);
            } else
                throw new JHVGLException("Cannot link shader: unknown reason");
        }

        GL.glValidateProgram(id);

        if (cleanUp) {
            GL.glDetachShader(id, vertexID);
            GL.glDeleteShader(vertexID);
            vertexID = 0;
            GL.glDetachShader(id, fragmentID);
            GL.glDeleteShader(fragmentID);
            fragmentID = 0;
        }
        return id;
    }

}
