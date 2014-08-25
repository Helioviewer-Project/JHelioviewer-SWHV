package org.helioviewer.swhv.objects3d;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL3;

import org.helioviewer.swhv.GLSLProgram;

import com.jogamp.opengl.util.GLBuffers;

public class Cube {
    private final float[] vertexData;
    private final int[] vertexBufferObject = new int[1];
    private GLSLProgram programObject;

    private int mvmUnLoc;

    public Cube() {
        this.vertexData = new float[] {

        0.f, 0.f, 0.f, 1.f, .5f, 0.f, 0.f, 1.f,

        0.f, 0.f, 0.f, 1.f, 0.f, .5f, 0.f, 1.f,

        0.f, 0.f, 0.f, 1.f, 0.f, 0.f, 1.f, 1.f,

        };

        for (int i = 0; i < this.vertexData.length; i++) {
        }
    }

    public void initializeObject(GL3 gl) {
        initializeVertexBuffer(gl);
        buildShaders(gl);
    }

    private void initializeVertexBuffer(GL3 gl) {
        gl.glGenBuffers(1, IntBuffer.wrap(this.vertexBufferObject));
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferObject[0]);
        {
            FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(vertexData);
            gl.glBufferData(GL3.GL_ARRAY_BUFFER, vertexData.length * 4, buffer, GL3.GL_STATIC_DRAW);
        }
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    private void buildShaders(GL3 gl) {
        programObject = new GLSLProgram(gl);
        programObject.attachVertexShader(gl, this.getClass().getResource("/data/vertexcube.glsl"));
        programObject.attachFragmentShader(gl, this.getClass().getResource("/data/fragmentcube.glsl"));

        programObject.initializeProgram(gl, true);
        this.mvmUnLoc = gl.glGetUniformLocation(programObject.getProgId(), "mvmmatrix");

    }

    public void render(GL3 gl, float[] fs) {
        programObject.bind(gl);
        gl.glDisable(GL3.GL_CULL_FACE);

        gl.glUniformMatrix4fv(mvmUnLoc, 1, false, fs, 0);
        {
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferObject[0]);
            gl.glEnableVertexAttribArray(0);
            {
                gl.glActiveTexture(GL3.GL_TEXTURE0);
                gl.glVertexAttribPointer(0, 4, GL3.GL_FLOAT, false, 0, 0);
                gl.glDrawArrays(GL3.GL_LINES, 0, this.vertexData.length);
            }
            gl.glDisableVertexAttribArray(0);
        }
        programObject.unbind(gl);
    }
}
