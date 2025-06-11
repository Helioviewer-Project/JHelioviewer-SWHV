package org.helioviewer.jhv.opengl;

import java.nio.charset.StandardCharsets;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.FileUtils;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLException;

abstract class GLSLShader {

    private enum ShaderType {
        vertex(GL3.GL_VERTEX_SHADER), fragment(GL3.GL_FRAGMENT_SHADER);

        final int glType;

        ShaderType(int _glType) {
            glType = _glType;
        }
    }

    private static int usedID; // track state

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
            use(gl);
            initUniforms(gl, progID);
        } catch (Exception e) {
            throw new GLException("Cannot load shader", e);
        }
    }

    protected final void _dispose(GL3 gl) {
        gl.glDeleteShader(vertexID);
        gl.glDeleteShader(fragmentID);
        gl.glDeleteProgram(progID);
    }

    public final void use(GL3 gl) {
        if (progID != usedID) {
            usedID = progID;
            gl.glUseProgram(progID);
        }
    }

    protected abstract void initUniforms(GL3 gl, int id);

    protected static void setTextureUnit(GL3 gl, int id, String texname, GLTexture.Unit unit) {
        int loc = gl.glGetUniformLocation(id, texname);
        if (loc != -1)
            gl.glUniform1i(loc, unit.ordinal());
        else
            Log.error("Invalid texture " + texname);
    }

    private static int attachShader(GL3 gl, ShaderType type, String text) {
        int id = gl.glCreateShader(type.glType);

        String[] akProgramText = new String[1];
        akProgramText[0] = text;

        int[] aiLength = new int[1];
        aiLength[0] = akProgramText[0].length();
        int iCount = 1;

        gl.glShaderSource(id, iCount, akProgramText, aiLength, 0);
        gl.glCompileShader(id);

        int[] params = {0};
        gl.glGetShaderiv(id, GL3.GL_COMPILE_STATUS, params, 0);
        if (params[0] != 1) {
            Log.error("Shader compile status: " + params[0]);
            gl.glGetShaderiv(id, GL3.GL_INFO_LOG_LENGTH, params, 0);
            if (params[0] > 0) {
                byte[] infoLog = new byte[params[0]];
                gl.glGetShaderInfoLog(id, params[0], params, 0, infoLog, 0);

                String log = new String(infoLog, StandardCharsets.UTF_8);
                Log.error(log);
                throw new GLException("Cannot compile " + type + " shader: " + log);
            } else
                throw new GLException("Cannot compile " + type + " shader: unknown reason");
        }
        return id;
    }

    private int initializeProgram(GL3 gl, boolean cleanUp) {
        int id = gl.glCreateProgram();
        gl.glAttachShader(id, vertexID);
        gl.glAttachShader(id, fragmentID);
        gl.glLinkProgram(id);

        int[] params = {0};
        gl.glGetProgramiv(id, GL3.GL_LINK_STATUS, params, 0);
        if (params[0] != 1) {
            Log.error("Shader link status: " + params[0]);
            gl.glGetProgramiv(id, GL3.GL_INFO_LOG_LENGTH, params, 0);
            if (params[0] > 0) {
                byte[] infoLog = new byte[params[0]];
                gl.glGetProgramInfoLog(id, params[0], params, 0, infoLog, 0);

                String log = new String(infoLog, StandardCharsets.UTF_8);
                Log.error(log);
                throw new GLException("Cannot link shader: " + log);
            } else
                throw new GLException("Cannot link shader: unknown reason");
        }

        gl.glValidateProgram(id);

        if (cleanUp) {
            gl.glDetachShader(id, vertexID);
            gl.glDeleteShader(vertexID);
            gl.glDetachShader(id, fragmentID);
            gl.glDeleteShader(fragmentID);
        }
        return id;
    }

}
