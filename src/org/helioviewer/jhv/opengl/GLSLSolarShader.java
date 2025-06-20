package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;

import com.jogamp.opengl.GL3;

public class GLSLSolarShader extends GLSLShader {

    public static final GLSLSolarShader sphere = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarSphere.frag", false);
    public static final GLSLSolarShader ortho = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarOrtho.frag", true);
    public static final GLSLSolarShader lati = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLati.frag", true);
    public static final GLSLSolarShader polar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarPolar.frag", true);
    public static final GLSLSolarShader logpolar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLogPolar.frag", true);

    private final boolean hasCommon;

    private int gridRef;

    private int deltaTRef;

    private int sectorRef;
    private int cutOffDirectionRef;
    private int cutOffValueRef;

    private int slitRef;
    private int sharpenRef;
    private int calculateDepthRef;

    private final int[] intArr = new int[1];
    private final float[] floatArr = new float[8];

    private GLSLSolarShader(String vertex, String fragment, boolean _hasCommon) {
        super(vertex, fragment);
        hasCommon = _hasCommon;
    }

    private static GLBO screenBO;
    private static final FloatBuffer screenBuf = BufferUtils.newFloatBuffer(16 + 4 + 4);
    private static final int SCREEN_SIZE = screenBuf.capacity() * 4;

    private static GLBO wcsBO;
    private static final FloatBuffer wcsBuf = BufferUtils.newFloatBuffer(2 * (4 + 4 + 4 + 4));
    private static final int WCS_SIZE = wcsBuf.capacity() * 4;

    private static GLBO displayBO;
    private static final FloatBuffer displayBuf = BufferUtils.newFloatBuffer(4 + 4 + 4);
    private static final int DISPLAY_SIZE = displayBuf.capacity() * 4;

    public static void init(GL3 gl) {
        screenBO = new GLBO(gl, GL3.GL_UNIFORM_BUFFER, GL3.GL_STREAM_DRAW);
        wcsBO = new GLBO(gl, GL3.GL_UNIFORM_BUFFER, GL3.GL_STREAM_DRAW);
        displayBO = new GLBO(gl, GL3.GL_UNIFORM_BUFFER, GL3.GL_STREAM_DRAW);

        sphere._init(gl, sphere.hasCommon);
        ortho._init(gl, ortho.hasCommon);
        lati._init(gl, lati.hasCommon);
        polar._init(gl, polar.hasCommon);
        logpolar._init(gl, logpolar.hasCommon);
    }

    private static void setupUBO(GL3 gl, int programID, String blockName, int uboID, int binding) {
        int blockIndex = gl.glGetUniformBlockIndex(programID, blockName);
        gl.glUniformBlockBinding(programID, blockIndex, binding);
        gl.glBindBufferBase(GL3.GL_UNIFORM_BUFFER, binding, uboID);
    }

    @Override
    protected void initUniforms(GL3 gl, int id) {
        gridRef = gl.glGetUniformLocation(id, "grid");

        deltaTRef = gl.glGetUniformLocation(id, "deltaT");

        sectorRef = gl.glGetUniformLocation(id, "sector");
        cutOffDirectionRef = gl.glGetUniformLocation(id, "cutOffDirection");
        cutOffValueRef = gl.glGetUniformLocation(id, "cutOffValue");

        sharpenRef = gl.glGetUniformLocation(id, "sharpen");
        slitRef = gl.glGetUniformLocation(id, "slit");
        calculateDepthRef = gl.glGetUniformLocation(id, "calculateDepth");

        setupUBO(gl, id, "ScreenBlock", screenBO.getID(), 0);
        setupUBO(gl, id, "WCSBlock", wcsBO.getID(), 1);
        setupUBO(gl, id, "DisplayBlock", displayBO.getID(), 2);

        if (hasCommon) {
            setTextureUnit(gl, id, "image", GLTexture.Unit.ZERO);
            setTextureUnit(gl, id, "lut", GLTexture.Unit.ONE);
            setTextureUnit(gl, id, "diffImage", GLTexture.Unit.TWO);
        }
    }

    public static void dispose(GL3 gl) {
        sphere._dispose(gl);
        ortho._dispose(gl);
        lati._dispose(gl);
        polar._dispose(gl);
        logpolar._dispose(gl);
        screenBO.delete(gl);
        wcsBO.delete(gl);
        displayBO.delete(gl);
    }

    public static void bindScreen(GL3 gl, Viewport vp) {
        FloatBuffer inv = Transform.getInverse();
        screenBuf.put(inv);
        inv.flip();
        screenBuf.put(vp.glslArray).put((float) (1 / vp.aspect));
        screenBuf.put((float) Display.mode.scale.getYstart()).put((float) Display.mode.scale.getYstop());

        screenBO.setBufferData(gl, SCREEN_SIZE, SCREEN_SIZE, screenBuf.flip());
    }

    public void bindWCS(GL3 gl,
                        Quat cameraDiff0, Region r0, Quat crota0, float[] crval0,
                        Quat cameraDiff1, Region r1, Quat crota1, float[] crval1) {
        cameraDiff0.setFloatBuffer(wcsBuf);
        wcsBuf.put(r0.glslArray);
        crota0.setFloatBuffer(wcsBuf);
        wcsBuf.put(crval0); // has padding

        cameraDiff1.setFloatBuffer(wcsBuf);
        wcsBuf.put(r1.glslArray);
        crota1.setFloatBuffer(wcsBuf);
        wcsBuf.put(crval1);

        wcsBO.setBufferData(gl, WCS_SIZE, WCS_SIZE, wcsBuf.flip());
    }

    public void bindDisplay(GL3 gl,
                            float red, float green, float blue, double alpha, double blend,
                            double bOffset, double bScale, int enhanced, int isDiff,
                            float innerRadius, float outerRadius) {
        // https://amindforeverprogramming.blogspot.com/2013/07/why-alpha-premultiplied-colour-blending.html
        displayBuf.put((float) (red * alpha)).put((float) (green * alpha)).put((float) (blue * alpha)).put((float) (alpha * blend));
        displayBuf.put((float) bOffset).put((float) bScale).put(enhanced).put(isDiff);
        displayBuf.put(innerRadius).put(outerRadius);

        displayBO.setBufferData(gl, DISPLAY_SIZE, DISPLAY_SIZE, displayBuf.flip());
    }

    public void bindDeltaT(GL3 gl, double deltaT0, double deltaT1) {
        floatArr[0] = (float) deltaT0;
        floatArr[1] = (float) deltaT1;
        gl.glUniform1fv(deltaTRef, 2, floatArr, 0);
    }

    public void bindSlit(GL3 gl, double left, double right) {
        floatArr[0] = (float) left;
        floatArr[1] = (float) right;
        gl.glUniform1fv(slitRef, 2, floatArr, 0);
    }

    public void bindSharpen(GL3 gl, double weight, double pixelWidth, double pixelHeight) {
        floatArr[0] = (float) pixelWidth;
        floatArr[1] = (float) pixelHeight;
        floatArr[2] = -2 * (float) weight; // used for mix
        gl.glUniform3fv(sharpenRef, 1, floatArr, 0);
    }

    public void bindCalculateDepth(GL3 gl, boolean calculateDepth) {
        intArr[0] = calculateDepth ? 1 : 0;
        gl.glUniform1iv(calculateDepthRef, 1, intArr, 0);
    }

    public void bindCutOffValue(GL3 gl, float val) {
        floatArr[0] = val;
        gl.glUniform1fv(cutOffValueRef, 1, floatArr, 0);
    }

    public void bindCutOffDirection(GL3 gl, float x, float y) {
        floatArr[0] = x;
        floatArr[1] = y;
        gl.glUniform2fv(cutOffDirectionRef, 1, floatArr, 0);
    }

    public void bindAnglesLatiGrid(GL3 gl, double lon, double lat, double hglt, double dlon, double dlat, double dhglt) {
        floatArr[0] = (float) lon;
        floatArr[1] = (float) lat;
        floatArr[2] = (float) hglt;
        floatArr[3] = (float) dlon;
        floatArr[4] = (float) dlat;
        floatArr[5] = (float) dhglt;
        gl.glUniform3fv(gridRef, 2, floatArr, 0);
    }

    public void bindSector(GL3 gl, double sector0, double sector1) {
        floatArr[0] = /*sector0 + 2 * Math.PI == sector1*/ sector0 == sector1 ? 0 : 1; // common case
        floatArr[1] = (float) sector0;
        floatArr[2] = (float) sector1;
        gl.glUniform1fv(sectorRef, 3, floatArr, 0);
    }

}
