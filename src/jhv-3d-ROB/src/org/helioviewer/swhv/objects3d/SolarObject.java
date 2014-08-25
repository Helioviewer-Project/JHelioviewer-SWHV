package org.helioviewer.swhv.objects3d;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import javax.media.opengl.GL3;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.LUT;
import org.helioviewer.swhv.GLSLProgram;

import com.jogamp.opengl.util.GLBuffers;

public class SolarObject {
    private static int MAX_TEXTURES = 10;
    private final float[] vertexData;
    private final int[] vertexBufferObject = new int[1];
    private GLSLProgram programObject;

    private final int[] textureUnLoc = new int[MAX_TEXTURES];
    private final IntBuffer imdataBufferID[] = new IntBuffer[MAX_TEXTURES];
    ByteBuffer[] buffer = new ByteBuffer[MAX_TEXTURES];
    private final int[] width = new int[MAX_TEXTURES];
    private final int[] height = new int[MAX_TEXTURES];
    private final boolean[] newBufferData = new boolean[MAX_TEXTURES];
    private final boolean[] activeTexture = new boolean[MAX_TEXTURES];

    private final int[] incomingWidth = new int[MAX_TEXTURES];
    private final int[] incomingHeight = new int[MAX_TEXTURES];
    private boolean texturesUpdated = false;
    private int count = 0;

    private final boolean[] newLUT = new boolean[MAX_TEXTURES];
    private boolean lutUpdated = false;
    private final IntBuffer[] lutdataBufferID = new IntBuffer[MAX_TEXTURES];
    private final IntBuffer[] lutBuffer = new IntBuffer[MAX_TEXTURES];
    private final int[] lutUnLoc = new int[MAX_TEXTURES];
    private final boolean[] activeLUT = new boolean[MAX_TEXTURES];
    private int mvmUnLoc;

    public SolarObject() {
        this.vertexData = new float[] { 1.0f, 1.00f, 0.0f, 1.0f, -1f, -1f, 0.0f, 1.0f, -1f, 1.0f, 0.0f, 1.0f, 1.0f, 1.00f, 0.0f, 1.0f, 1f, -1f, 0.0f, 1.0f, -1f, -1.0f, 0.0f, 1.0f, 1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, };
        File addOnDir = JHVDirectory.COLOR_PLUGINS.getFile();
        Map<String, LUT> luts = LUT.getStandardList();
        LUT l = luts.get("SDO-AIA 171 Å");
        Object[] str = luts.keySet().toArray();
        for (int i = 0; i < str.length; i++) {
            System.out.println(luts.keySet().toArray()[i]);
        }
        for (int i = 0; i < MAX_TEXTURES; i++) {
            this.height[i] = 0;
            this.width[i] = 0;
            this.incomingHeight[i] = 0;
            this.incomingWidth[i] = 0;
            this.newBufferData[i] = false;
            this.activeTexture[i] = false;
            this.imdataBufferID[i] = IntBuffer.allocate(1);

            this.newLUT[i] = false;
            this.activeLUT[i] = false;

            this.lutBuffer[i] = IntBuffer.wrap(l.getLut8());
            this.lutdataBufferID[i] = IntBuffer.allocate(1);
        }
        this.newLUT[0] = true;
        this.activeLUT[0] = true;
        this.lutUpdated = true;
    }

    public SolarObject(float[] vertexData) {
        this.vertexData = vertexData;
        for (int i = 0; i < MAX_TEXTURES; i++) {
            this.height[i] = 0;
            this.width[i] = 0;
            this.incomingHeight[i] = 0;
            this.incomingWidth[i] = 0;
            this.newBufferData[i] = false;
            this.activeTexture[i] = false;
            this.imdataBufferID[i] = IntBuffer.allocate(1);
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
        programObject.attachVertexShader(gl, this.getClass().getResource("/data/vertex.glsl"));
        programObject.attachFragmentShader(gl, this.getClass().getResource("/data/fragment.glsl"));
        programObject.initializeProgram(gl, true);
        for (int i = 0; i < this.MAX_TEXTURES; i++) {
            this.textureUnLoc[i] = gl.glGetUniformLocation(programObject.getProgId(), "solarTexture[" + i + "]");
            this.lutUnLoc[i] = gl.glGetUniformLocation(programObject.getProgId(), "lut[" + i + "]");
        }
        this.mvmUnLoc = gl.glGetUniformLocation(programObject.getProgId(), "mvmmatrix");
    }

    private void generateTexture(GL3 gl, int index) {
        gl.glGenTextures(1, imdataBufferID[index]);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, imdataBufferID[index].get(0));
        gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_R8, width[index], height[index], 0, GL3.GL_RED, GL3.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_LEVEL, 0);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, imdataBufferID[index].get(0));
    }

    private void updateTexture(GL3 gl, int index) {
        if (this.width[index] < this.incomingWidth[index] || this.height[index] < this.incomingHeight[index]) {
            gl.glDeleteTextures(1, imdataBufferID[index]);
            this.width[index] = this.incomingWidth[index];
            this.height[index] = this.incomingHeight[index];
            generateTexture(gl, index);
        }
        gl.glBindTexture(GL3.GL_TEXTURE_2D, imdataBufferID[index].get(0));
        gl.glTexSubImage2D(GL3.GL_TEXTURE_2D, 0, 0, 0, width[index], height[index], GL3.GL_RED, GL3.GL_UNSIGNED_BYTE, buffer[index]);
    }

    private void removeTexture(GL3 gl, int index) {
        gl.glGenTextures(1, imdataBufferID[index]);
        gl.glDeleteTextures(1, imdataBufferID[index]);
        gl.glDeleteTextures(1, lutdataBufferID[index]);
    }

    private void generateTexture1D(GL3 gl, int index) {
        gl.glGenTextures(1, lutdataBufferID[index]);
        gl.glBindTexture(GL3.GL_TEXTURE_1D, lutdataBufferID[index].get(0));
        gl.glPixelStorei(GL3.GL_UNPACK_SKIP_PIXELS, 0);
        gl.glPixelStorei(GL3.GL_UNPACK_SKIP_ROWS, 0);
        gl.glPixelStorei(GL3.GL_UNPACK_ROW_LENGTH, 0);
        gl.glPixelStorei(GL3.GL_UNPACK_ALIGNMENT, 4);

        gl.glTexImage1D(GL3.GL_TEXTURE_1D, 0, GL3.GL_RGBA, lutBuffer[index].limit(), 0, GL3.GL_BGRA, GL3.GL_UNSIGNED_INT_8_8_8_8_REV, lutBuffer[index]);
        gl.glTexParameteri(GL3.GL_TEXTURE_1D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
        gl.glTexParameteri(GL3.GL_TEXTURE_1D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
        gl.glTexParameteri(GL3.GL_TEXTURE_1D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
    }

    private void updateLUT(GL3 gl, int index) {
        gl.glDeleteTextures(1, lutdataBufferID[index]);
        generateTexture1D(gl, index);
    }

    private void upDateTextures(GL3 gl) {
        for (int i = 0; i < MAX_TEXTURES; i++) {
            if (this.newBufferData[i]) {
                updateTexture(gl, i);
                this.newBufferData[i] = false;
            }
        }
    }

    private void updateLUTs(GL3 gl) {
        for (int i = 0; i < MAX_TEXTURES; i++) {
            if (this.newLUT[i]) {
                updateLUT(gl, i);
                this.newLUT[i] = false;
            }
        }
    }

    public void render(GL3 gl, float[] m) {
        programObject.bind(gl);
        gl.glUniformMatrix4fv(mvmUnLoc, 1, false, m, 0);
        {
            if (lutUpdated) {
                updateLUTs(gl);
                lutUpdated = false;
            }
            if (texturesUpdated) {
                upDateTextures(gl);
                texturesUpdated = false;
            }
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferObject[0]);

            gl.glEnableVertexAttribArray(0);
            gl.glEnableVertexAttribArray(1);
            {
                int i = 0;
                for (; i < this.MAX_TEXTURES; i++) {
                    if (this.activeTexture[i]) {
                        gl.glActiveTexture(GL3.GL_TEXTURE0 + i);
                        gl.glBindTexture(GL3.GL_TEXTURE_2D, imdataBufferID[i].get(0));
                        gl.glUniform1i(textureUnLoc[i], i);
                    }
                }
                for (int j = 0; j < this.MAX_TEXTURES; j++) {
                    if (this.activeLUT[j]) {
                        gl.glActiveTexture(GL3.GL_TEXTURE0 + i + j);
                        gl.glBindTexture(GL3.GL_TEXTURE_1D, this.lutdataBufferID[j].get(0));
                        gl.glUniform1i(lutUnLoc[j], i + j);
                    }
                }
                gl.glVertexAttribPointer(0, 4, GL3.GL_FLOAT, false, 0, 0);
                gl.glVertexAttribPointer(1, 2, GL3.GL_FLOAT, false, 0, 6 * 4 * 4);

                gl.glDrawArrays(GL3.GL_TRIANGLES, 0, 6);

            }
            gl.glDisableVertexAttribArray(0);
            gl.glDisableVertexAttribArray(1);

        }
        programObject.unbind(gl);
    }

    synchronized public void updateBufferData(int index, int width, int height, ByteBuffer buffer) {
        this.incomingWidth[index] = width;
        this.incomingHeight[index] = height;
        this.buffer[index] = buffer;
        this.activeTexture[index] = true;
        this.newBufferData[index] = true;
        this.texturesUpdated = true;
        this.count = this.count + 1;
    }
}
