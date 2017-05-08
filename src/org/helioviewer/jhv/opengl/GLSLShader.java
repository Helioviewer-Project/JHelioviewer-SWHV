package org.helioviewer.jhv.opengl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.logging.Log;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;

public class GLSLShader {

    private int vertexID;
    private int fragmentID;
    protected int progID;

    private final String vertex;
    private final String fragment;

    public GLSLShader(String _vertex, String _fragment) {
        vertex = _vertex;
        fragment = _fragment;
    }

    protected void _init(GL2 gl,boolean common) {
    	String fragmentCommonText = "";
    	if(common){
        InputStream fragmentCommonStream = FileUtils.getResourceInputStream("/data/fragmentcommon.glsl");
         fragmentCommonText = FileUtils.convertStreamToString(fragmentCommonStream);
    	}
        InputStream fragmentStream = FileUtils.getResourceInputStream(fragment);
        String fragmentText = FileUtils.convertStreamToString(fragmentStream);
        fragmentText = fragmentCommonText + fragmentText;
        InputStream vertexStream = FileUtils.getResourceInputStream(vertex);
        String vertexText = FileUtils.convertStreamToString(vertexStream);
        attachVertexShader(gl, vertexText);
        attachFragmentShader(gl, fragmentText);

        initializeProgram(gl, true);
        _after_init(gl);
    }

    protected void _after_init(GL2 gl) {

    }

    protected void _dispose(GL2 gl) {
        gl.glDeleteShader(vertexID);
        gl.glDeleteShader(fragmentID);
        gl.glDeleteProgram(progID);

    }

    public final void bind(GL2 gl) {
        gl.glUseProgram(progID);
    }

    public static void unbind(GL2 gl) {
        gl.glUseProgram(0);
    }

    public final void setTextureUnit(GL2 gl, String texname, int texunit) {
        int[] params = { 0 };
        gl.glGetProgramiv(progID, GL2.GL_LINK_STATUS, params, 0);
        if (params[0] != 1) {
            Log.error("Error: setTextureUnit needs program to be linked");
        }
        int id = gl.glGetUniformLocation(progID, texname);
        if (id == -1) {
            Log.error("Warning: Invalid texture " + texname);
            return;
        }
        gl.glUniform1i(id, texunit);
    }

    private void attachVertexShader(GL2 gl, String vertexText) {
        int iID = gl.glCreateShader(GL2.GL_VERTEX_SHADER);

        String[] akProgramText = new String[1];
        akProgramText[0] = vertexText;

        int[] aiLength = new int[1];
        aiLength[0] = akProgramText[0].length();
        int iCount = 1;

        gl.glShaderSource(iID, iCount, akProgramText, aiLength, 0);
        gl.glCompileShader(iID);

        int[] params = { 0 };
        gl.glGetShaderiv(iID, GL2.GL_COMPILE_STATUS, params, 0);
        if (params[0] != 1) {
            Log.error("vertex compile status: " + params[0]);
            gl.glGetShaderiv(iID, GL2.GL_INFO_LOG_LENGTH, params, 0);

            byte[] abInfoLog = new byte[params[0]];
            gl.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);

            String log = new String(abInfoLog, StandardCharsets.UTF_8);
            Log.error(log);
            throw new GLException("Cannot compile vertex shader : " + log);
        }
        vertexID = iID;
    }

    private void attachFragmentShader(GL2 gl, String fragmentText) {
        int iID = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);

        String[] akProgramText = new String[1];
        akProgramText[0] = fragmentText;

        int[] aiLength = new int[1];
        aiLength[0] = akProgramText[0].length();
        int iCount = 1;

        gl.glShaderSource(iID, iCount, akProgramText, aiLength, 0);
        gl.glCompileShader(iID);

        int[] params = { 0 };
        gl.glGetShaderiv(iID, GL2.GL_COMPILE_STATUS, params, 0);
        if (params[0] != 1) {
            Log.error("fragment compile status: " + params[0]);
            gl.glGetShaderiv(iID, GL2.GL_INFO_LOG_LENGTH, params, 0);

            byte[] abInfoLog = new byte[params[0]];
            gl.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);

            String log = new String(abInfoLog, StandardCharsets.UTF_8);
            Log.error(log);
            throw new GLException("Cannot compile fragment shader : " + log);
        }
        fragmentID = iID;
    }

    private void initializeProgram(GL2 gl, boolean cleanUp) {
        progID = gl.glCreateProgram();
        gl.glAttachShader(progID, vertexID);
        gl.glAttachShader(progID, fragmentID);

        this.bindAttribs(gl);
        gl.glLinkProgram(progID);

        int[] params = { 0 };
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

	protected void bindAttribs(GL2 gl) {
		
	}

}
