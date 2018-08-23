package org.helioviewer.jhv.opengl;

import java.nio.charset.StandardCharsets;

import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;

public class GLSLShader {

    private enum ShaderType {
        vertex(GL2.GL_VERTEX_SHADER), fragment(GL2.GL_FRAGMENT_SHADER);

        final int glType;

        ShaderType(int _glType) {
            glType = _glType;
        }
    }

    private static int boundID; // track state

    private int vertexID;
    private int fragmentID;
    protected int progID;

    private final String vertex;
    private final String fragment;

    GLSLShader(String _vertex, String _fragment) {
        vertex = _vertex;
        fragment = _fragment;
    }

    protected void _init(GL2 gl, boolean common) {
        try {
            String vertexText = FileUtils.streamToString(FileUtils.getResource(vertex));
            vertexID = attachShader(gl, ShaderType.vertex, vertexText);

            String fragmentCommonText = common ? FileUtils.streamToString(FileUtils.getResource("/glsl/solarCommon.frag")) : "";
            String fragmentText = fragmentCommonText + FileUtils.streamToString(FileUtils.getResource(fragment));
            fragmentID = attachShader(gl, ShaderType.fragment, fragmentText);

            initializeProgram(gl, true);
            _after_init(gl);
        } catch (Exception e) {
            throw new GLException("Cannot load shader", e);
        }
    }

    protected void bindAttribs(GL2 gl) {
    }

    protected void _after_init(GL2 gl) {
    }

    protected void _dispose(GL2 gl) {
        gl.glDeleteShader(vertexID);
        gl.glDeleteShader(fragmentID);
        gl.glDeleteProgram(progID);
    }

    public final void bind(GL2 gl) {
        if (progID != boundID) {
            boundID = progID;
            gl.glUseProgram(progID);
        }
    }

    protected final void setTextureUnit(GL2 gl, String texname, GLTexture.Unit unit) {
        int id = gl.glGetUniformLocation(progID, texname);
        if (id == -1) {
            Log.error("Warning: Invalid texture " + texname);
            return;
        }
        gl.glUniform1i(id, unit.ordinal());
    }

    private static int attachShader(GL2 gl, ShaderType type, String text) {
        int iID = gl.glCreateShader(type.glType);

        String[] akProgramText = new String[1];
        akProgramText[0] = text;

        int[] aiLength = new int[1];
        aiLength[0] = akProgramText[0].length();
        int iCount = 1;

        gl.glShaderSource(iID, iCount, akProgramText, aiLength, 0);
        gl.glCompileShader(iID);

        int[] params = {0};
        gl.glGetShaderiv(iID, GL2.GL_COMPILE_STATUS, params, 0);
        if (params[0] != 1) {
            Log.error("shader compile status: " + params[0]);
            gl.glGetShaderiv(iID, GL2.GL_INFO_LOG_LENGTH, params, 0);

            byte[] abInfoLog = new byte[params[0]];
            gl.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);

            String log = new String(abInfoLog, StandardCharsets.UTF_8);
            Log.error(log);
            throw new GLException("Cannot compile " + type + " shader: " + log);
        }
        return iID;
    }

    private void initializeProgram(GL2 gl, boolean cleanUp) {
        progID = gl.glCreateProgram();
        gl.glAttachShader(progID, vertexID);
        gl.glAttachShader(progID, fragmentID);

        bindAttribs(gl);
        gl.glLinkProgram(progID);

        int[] params = {0};
        gl.glGetProgramiv(progID, GL2.GL_LINK_STATUS, params, 0);
        if (params[0] != 1) {
            Log.error("link status: " + params[0]);
            gl.glGetProgramiv(progID, GL2.GL_INFO_LOG_LENGTH, params, 0);

            byte[] abInfoLog = new byte[params[0]];
            gl.glGetProgramInfoLog(progID, params[0], params, 0, abInfoLog, 0);

            String log = new String(abInfoLog, StandardCharsets.UTF_8);
            Log.error(log);
            throw new GLException("Cannot link shaders : " + log);
        }

        gl.glValidateProgram(progID);

        if (cleanUp) {
            gl.glDetachShader(progID, vertexID);
            gl.glDeleteShader(vertexID);
            gl.glDetachShader(progID, fragmentID);
            gl.glDeleteShader(fragmentID);
        }
    }

}
