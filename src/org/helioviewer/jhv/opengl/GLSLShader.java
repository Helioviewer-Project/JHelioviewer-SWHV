package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.FileUtils;

abstract class GLSLShader {
    private static final String COMMON_FRAGMENT = "/glsl/solarCommon.frag";

    protected static final class UBO {
        static final int WCS = 0;
        static final int PROJECTION = 1;
        static final int SOLAR_SCREEN = 2;
        static final int DISPLAY = 3;
        static final int LINE_SCREEN = 4;

        private UBO() {
        }
    }

    protected static void setupUBO(int programID, String blockName, int uboID, int binding) {
        int blockIndex = GL.glGetUniformBlockIndex(programID, blockName);
        if (blockIndex < 0)
            return;
        GL.glUniformBlockBinding(programID, blockIndex, binding);
        GL.glBindBufferBase(GL.UNIFORM_BUFFER, binding, uboID);
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
            vertexID = attachShader(GL.VERTEX_SHADER, FileUtils.streamToString(FileUtils.getResource(vertex)));

            String fragmentText = FileUtils.streamToString(FileUtils.getResource(fragment));
            if (common)
                fragmentText = FileUtils.streamToString(FileUtils.getResource(COMMON_FRAGMENT)) + fragmentText;
            fragmentID = attachShader(GL.FRAGMENT_SHADER, fragmentText);

            progID = initializeProgram();
            use();
            initUniforms(progID);
        } catch (Exception e) {
            _dispose();
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

    private static int attachShader(int shaderType, String text) {
        int id = GL.glCreateShader(shaderType);
        try {
            GL.glShaderSource(id, text);
            GL.glCompileShader(id);

            int compileStatus = GL.glGetShaderi(id, GL.COMPILE_STATUS);
            if (compileStatus != 1) {
                Log.error("Shader compile status: " + compileStatus);
                int infoLogLength = GL.glGetShaderi(id, GL.INFO_LOG_LENGTH);
                if (infoLogLength > 0) {
                    String log = GL.glGetShaderInfoLog(id, infoLogLength);
                    Log.error(log);
                    throw new JHVGLException("Cannot compile shader: " + log);
                } else
                    throw new JHVGLException("Cannot compile shader: unknown reason");
            }
            return id;
        } catch (Exception e) {
            GL.glDeleteShader(id);
            throw e;
        }
    }

    private int initializeProgram() {
        int id = GL.glCreateProgram();
        try {
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

            GL.glDetachShader(id, vertexID);
            GL.glDeleteShader(vertexID);
            vertexID = 0;
            GL.glDetachShader(id, fragmentID);
            GL.glDeleteShader(fragmentID);
            fragmentID = 0;
            return id;
        } catch (Exception e) {
            GL.glDeleteProgram(id);
            throw e;
        }
    }

}
