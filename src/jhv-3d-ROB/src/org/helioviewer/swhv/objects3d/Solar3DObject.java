package org.helioviewer.swhv.objects3d;

import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;
import javax.media.opengl.GL3;

import org.helioviewer.swhv.GLSLProgram;

import com.jogamp.opengl.util.GLBuffers;

public class Solar3DObject {
    private static int MAX_TEXTURES = 1;
    private float[] vertexData;
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
    private boolean textureUpdated = false;
    private int mvmUnLoc;
    int XRES = 18;
    int YRES = 18;

    public Solar3DObject() {
        this.vertexData = new float[] { 1.0f, 1.0f, 0.0f, 1.0f, -1f, -1f, 0.0f, 1.0f, -1f, 1.0f, 0.0f, 1.0f, 1.0f, 1.00f, 0.0f, 1.0f, 1f, -1f, 0.0f, 1.0f, -1f, -1.0f, 0.0f, 1.0f, 1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, };

        int count = 0;
        this.vertexData = new float[4 * (XRES + 1) * (YRES + 1)];
        for (int i = 0; i <= XRES; i++) {
            for (int j = 0; j <= YRES; j++) {
                this.vertexData[count] = (float) (-1.f + 1. * i / XRES);
                count++;
                this.vertexData[count] = (float) (-1.f + 1. * j / XRES);
                count++;
                this.vertexData[count] = 0.f;
                count++;
                this.vertexData[count] = 1.f;
                count++;
            }
        }
        this.imdataBufferID[0] = IntBuffer.allocate(1);
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
        programObject.attachVertexShader(gl, this.getClass().getResource("/data/vertex3d.glsl"));
        programObject.attachFragmentShader(gl, this.getClass().getResource("/data/fragment3d.glsl"));
        programObject.attachGeometryShader(gl, this.getClass().getResource("/data/geometry3d.glsl"));

        programObject.initializeProgram(gl, true);
        this.textureUnLoc[0] = gl.glGetUniformLocation(programObject.getProgId(), "solarTexture[" + 0 + "]");
        this.mvmUnLoc = gl.glGetUniformLocation(programObject.getProgId(), "mvmmatrix");

    }

    private void generateTexture(GL3 gl) {
        try {
            BufferedImage img1 = ImageIO.read(new File("/Users/freekv/swhv/build/data/pfss/ttt/Br0.png"));
            BufferedImage img2 = ImageIO.read(new File("/Users/freekv/swhv/build/data/pfss/ttt/Bt0.png"));
            BufferedImage img3 = ImageIO.read(new File("/Users/freekv/swhv/build/data/pfss/ttt/Bp0.png"));

            int[] packedPixels1 = new int[img1.getWidth() * img1.getHeight()];
            int[] packedPixels2 = new int[img2.getWidth() * img2.getHeight()];
            int[] packedPixels3 = new int[img3.getWidth() * img3.getHeight()];

            PixelGrabber pixelgrabber1 = new PixelGrabber(img1, 0, 0, img1.getWidth(), img1.getHeight(), packedPixels1, 0, img1.getWidth());
            PixelGrabber pixelgrabber2 = new PixelGrabber(img2, 0, 0, img2.getWidth(), img2.getHeight(), packedPixels2, 0, img2.getWidth());
            PixelGrabber pixelgrabber3 = new PixelGrabber(img3, 0, 0, img3.getWidth(), img3.getHeight(), packedPixels3, 0, img3.getWidth());

            try {
                pixelgrabber1.grabPixels();
                pixelgrabber2.grabPixels();
                pixelgrabber3.grabPixels();
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
            boolean storeAlphaChannel = false;
            int bytesPerPixel = storeAlphaChannel ? 4 : 3;
            ByteBuffer tVolumeData = GLBuffers.newDirectByteBuffer(packedPixels1.length * bytesPerPixel * 100);
            for (int i = 0; i < 100; i++) {
                System.out.println("/Users/freekv/swhv/build/data/pfss/ttt/tttt" + i + ".png");
                img1 = ImageIO.read(new File("/Users/freekv/swhv/build/data/pfss/ttt/Br" + i + ".png"));
                img2 = ImageIO.read(new File("/Users/freekv/swhv/build/data/pfss/ttt/Bt" + i + ".png"));
                img3 = ImageIO.read(new File("/Users/freekv/swhv/build/data/pfss/ttt/Bp" + i + ".png"));

                packedPixels1 = new int[img1.getWidth() * img1.getHeight()];
                packedPixels2 = new int[img2.getWidth() * img2.getHeight()];
                packedPixels3 = new int[img3.getWidth() * img3.getHeight()];

                pixelgrabber1 = new PixelGrabber(img1, 0, 0, img1.getWidth(), img1.getHeight(), packedPixels1, 0, img1.getWidth());
                pixelgrabber2 = new PixelGrabber(img2, 0, 0, img2.getWidth(), img2.getHeight(), packedPixels2, 0, img2.getWidth());
                pixelgrabber3 = new PixelGrabber(img3, 0, 0, img3.getWidth(), img3.getHeight(), packedPixels3, 0, img3.getWidth());
                try {
                    pixelgrabber1.grabPixels();
                    pixelgrabber2.grabPixels();
                    pixelgrabber3.grabPixels();
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }
                for (int row = img1.getHeight() - 1; row >= 0; row--) {
                    for (int col = 0; col < img1.getWidth(); col++) {
                        int packedPixel1 = packedPixels1[row * img1.getWidth() + col];
                        int packedPixel2 = packedPixels2[row * img2.getWidth() + col];
                        int packedPixel3 = packedPixels3[row * img3.getWidth() + col];
                        tVolumeData.put((byte) ((packedPixel3 >> 16) & 0xFF));
                        tVolumeData.put((byte) ((packedPixel2 >> 8) & 0xFF));
                        tVolumeData.put((byte) ((packedPixel1 >> 0) & 0xFF));
                    }
                }
            }
            tVolumeData.flip();
            tVolumeData.rewind();
            gl.glGenTextures(1, imdataBufferID[0]);
            gl.glBindTexture(GL3.GL_TEXTURE_3D, imdataBufferID[0].get(0));
            gl.glTexImage3D(GL3.GL_TEXTURE_3D, 0, GL3.GL_RGB, img1.getWidth(), img1.getHeight(), 100, 0, GL3.GL_RGB, GL3.GL_UNSIGNED_BYTE, tVolumeData);
            gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MAX_LEVEL, 0);
            gl.glBindTexture(GL3.GL_TEXTURE_3D, imdataBufferID[0].get(0));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    static float alpha = 0;

    public float[] rotationMatrixZ(double alpha) {
        float[] mat = { (float) Math.cos(alpha), (float) Math.sin(alpha), 0.0f, 0.0f, 0.f, (float) -Math.sin(alpha), (float) Math.cos(alpha), 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, };
        return mat;
    }

    public float[] rotationMatrixX(double alpha) {
        float[] mat = { 1.f, 0.f, 0.f, 0.f, 0.0f, (float) Math.cos(alpha), (float) Math.sin(alpha), 0.0f, 0.f, (float) -Math.sin(alpha), (float) Math.cos(alpha), 0.0f, 0.0f, 0.0f, 0.0f, 1.0f };
        return mat;
    }

    public float[] rotationMatrixY(double alpha) {
        float[] mat = { (float) Math.cos(alpha), 0.0f, (float) Math.sin(alpha), 0.0f, 0.f, 1.f, 0.f, 0.f, (float) -Math.sin(alpha), 0.0f, (float) Math.cos(alpha), 0.0f, 0.0f, 0.0f, 0.0f, 1.0f };
        return mat;
    }

    public void render(GL3 gl, float[] fs) {
        alpha = alpha + 0.1f;
        gl.glDisable(GL3.GL_CULL_FACE);
        gl.glEnable(GL3.GL_BLEND);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        programObject.bind(gl);
        //gl.glUniformMatrix4fv(mvmUnLoc, 1, false, rotationMatrixX(alpha), 0);
        gl.glUniformMatrix4fv(mvmUnLoc, 1, false, fs, 0);

        {
            if (!textureUpdated) {
                generateTexture(gl);
                textureUpdated = true;
            }
            //generateTexture(gl);

            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferObject[0]);

            gl.glEnableVertexAttribArray(0);
            //gl.glEnableVertexAttribArray(1);
            {

                gl.glActiveTexture(GL3.GL_TEXTURE0);
                gl.glBindTexture(GL3.GL_TEXTURE_3D, imdataBufferID[0].get(0));
                gl.glUniform1i(textureUnLoc[0], 0);

                gl.glVertexAttribPointer(0, 4, GL3.GL_FLOAT, false, 0, 0);
                //gl.glVertexAttribPointer(1, 2, GL3.GL_FLOAT, false, 0, 6 * 4 * 4);

                gl.glDrawArrays(GL3.GL_POINTS, 0, (XRES + 1) * (YRES + 1));

            }
            gl.glDisableVertexAttribArray(0);
            //gl.glDisableVertexAttribArray(1);

        }
        programObject.unbind(gl);
    }

}
