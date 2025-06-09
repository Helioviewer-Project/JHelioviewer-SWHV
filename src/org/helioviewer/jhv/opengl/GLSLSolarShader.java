package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;

import com.jogamp.opengl.GL3;

public class GLSLSolarShader extends GLSLShader {

    public static final GLSLSolarShader sphere = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarSphere.frag", false);
    public static final GLSLSolarShader ortho = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarOrtho.frag", true);
    public static final GLSLSolarShader lati = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLati.frag", true);
    public static final GLSLSolarShader polar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarPolar.frag", true);
    public static final GLSLSolarShader logpolar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLogPolar.frag", true);

    private final boolean hasCommon;

    private int isDiffRef;

    private int gridRef;

    private int crvalRef;
    private int crotaRef;
    private int rectRef;

    private int deltaTRef;

    private int sectorRef;
    private int radiiRef;
    private int polarRadiiRef;
    private int cutOffDirectionRef;
    private int cutOffValueRef;

    private int slitRef;
    private int brightRef;
    private int colorRef;
    private int sharpenRef;
    private int ditherEnhancedRef;
    private int calculateDepthRef;

    private int cameraDifferenceRef;

    private final int[] intArr = new int[2];
    private final float[] floatArr = new float[8];

    private GLSLSolarShader(String vertex, String fragment, boolean _hasCommon) {
        super(vertex, fragment);
        hasCommon = _hasCommon;
    }

    private static int uboID;

    public static void init(GL3 gl) {
        int[] tmp = new int[1];
        gl.glGenBuffers(1, tmp, 0);
        uboID = tmp[0];

        gl.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uboID);
        gl.glBufferData(GL3.GL_UNIFORM_BUFFER, 16 * 4 + 2 * 4 * 4, null, GL3.GL_DYNAMIC_DRAW);
        gl.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        setupUBO(gl, sphere._init(gl, sphere.hasCommon));
        setupUBO(gl, ortho._init(gl, ortho.hasCommon));
        setupUBO(gl, lati._init(gl, lati.hasCommon));
        setupUBO(gl, polar._init(gl, polar.hasCommon));
        setupUBO(gl, logpolar._init(gl, logpolar.hasCommon));
    }

    private static void setupUBO(GL3 gl, int programID) {
        int blockIndex = gl.glGetUniformBlockIndex(programID, "ScreenBlock");
        gl.glUniformBlockBinding(programID, blockIndex, 0);
        gl.glBindBufferBase(GL3.GL_UNIFORM_BUFFER, 0, uboID);
    }

    @Override
    protected void initUniforms(GL3 gl, int id) {
        isDiffRef = gl.glGetUniformLocation(id, "isdifference");

        gridRef = gl.glGetUniformLocation(id, "grid");

        crvalRef = gl.glGetUniformLocation(id, "crval");
        crotaRef = gl.glGetUniformLocation(id, "crota");

        deltaTRef = gl.glGetUniformLocation(id, "deltaT");

        sectorRef = gl.glGetUniformLocation(id, "sector");
        radiiRef = gl.glGetUniformLocation(id, "radii");
        polarRadiiRef = gl.glGetUniformLocation(id, "polarRadii");
        cutOffDirectionRef = gl.glGetUniformLocation(id, "cutOffDirection");
        cutOffValueRef = gl.glGetUniformLocation(id, "cutOffValue");

        sharpenRef = gl.glGetUniformLocation(id, "sharpen");
        slitRef = gl.glGetUniformLocation(id, "slit");
        brightRef = gl.glGetUniformLocation(id, "brightness");
        colorRef = gl.glGetUniformLocation(id, "color");
        ditherEnhancedRef = gl.glGetUniformLocation(id, "ditherEnhanced");
        calculateDepthRef = gl.glGetUniformLocation(id, "calculateDepth");

        rectRef = gl.glGetUniformLocation(id, "rect");

        cameraDifferenceRef = gl.glGetUniformLocation(id, "cameraDifference");

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
        gl.glDeleteBuffers(1, new int[]{uboID}, 0);
    }

    public static void bindScreen(GL3 gl, float offsetX, float offsetY, float width, float height) {
        gl.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uboID);

        FloatBuffer buffer = gl.glMapBuffer(GL3.GL_UNIFORM_BUFFER, GL3.GL_WRITE_ONLY).asFloatBuffer();
        FloatBuffer inv = Transform.getInverse();
        buffer.put(inv);
        inv.flip();
        buffer.put(width);
        buffer.put(height);
        buffer.put(height / width);
        buffer.put(0f); // padding
        buffer.put(offsetX);
        buffer.put(offsetY);
        //buffer.flip();

        gl.glUnmapBuffer(GL3.GL_UNIFORM_BUFFER);
    }

    public void bindCameraDifference(GL3 gl, Quat quat, Quat quatDiff) {
        quat.setFloatArray(floatArr, 0);
        quatDiff.setFloatArray(floatArr, 4);
        gl.glUniform4fv(cameraDifferenceRef, 2, floatArr, 0);
    }

    public void bindCRVAL(GL3 gl, Vec2 vec, Vec2 vecDiff) {
        floatArr[0] = (float) vec.x;
        floatArr[1] = (float) vec.y;
        floatArr[2] = (float) vecDiff.x;
        floatArr[3] = (float) vecDiff.y;
        gl.glUniform2fv(crvalRef, 2, floatArr, 0);
    }

    public void bindCROTA(GL3 gl, Quat quat, Quat quatDiff) {
        quat.setFloatArray(floatArr, 0);
        quatDiff.setFloatArray(floatArr, 4);
        gl.glUniform4fv(crotaRef, 2, floatArr, 0);
    }

    public void bindRect(GL3 gl, Region r, Region dr) {
        floatArr[0] = (float) r.llx;
        floatArr[1] = (float) r.lly;
        floatArr[2] = (float) (1. / r.width);
        floatArr[3] = (float) (1. / r.height);
        floatArr[4] = (float) dr.llx;
        floatArr[5] = (float) dr.lly;
        floatArr[6] = (float) (1. / dr.width);
        floatArr[7] = (float) (1. / dr.height);
        gl.glUniform4fv(rectRef, 2, floatArr, 0);
    }

    public void bindDeltaT(GL3 gl, double deltaT, double deltaTDiff) {
        floatArr[0] = (float) deltaT;
        floatArr[1] = (float) deltaTDiff;
        gl.glUniform1fv(deltaTRef, 2, floatArr, 0);
    }

    public void bindColor(GL3 gl, float red, float green, float blue, double alpha, double blend) {
        floatArr[0] = (float) (red * alpha);
        floatArr[1] = (float) (green * alpha);
        floatArr[2] = (float) (blue * alpha);
        floatArr[3] = (float) (alpha * blend); // https://amindforeverprogramming.blogspot.com/2013/07/why-alpha-premultiplied-colour-blending.html
        gl.glUniform4fv(colorRef, 1, floatArr, 0);
    }

    public void bindSlit(GL3 gl, double left, double right) {
        floatArr[0] = (float) left;
        floatArr[1] = (float) right;
        gl.glUniform1fv(slitRef, 2, floatArr, 0);
    }

    public void bindBrightness(GL3 gl, double offset, double scale, double gamma) {
        floatArr[0] = (float) offset;
        floatArr[1] = (float) scale;
        floatArr[2] = (float) gamma;
        gl.glUniform3fv(brightRef, 1, floatArr, 0);
    }

    public void bindSharpen(GL3 gl, double weight, double pixelWidth, double pixelHeight) {
        floatArr[0] = (float) pixelWidth;
        floatArr[1] = (float) pixelHeight;
        floatArr[2] = -2 * (float) weight; // used for mix
        gl.glUniform3fv(sharpenRef, 1, floatArr, 0);
    }

    public void bindDitherEnhanced(GL3 gl, boolean dither, boolean enhanced) {
        intArr[0] = dither ? 1 : 0;
        intArr[1] = enhanced ? 1 : 0;
        gl.glUniform1iv(ditherEnhancedRef, 2, intArr, 0);
    }

    public void bindCalculateDepth(GL3 gl, boolean calculateDepth) {
        intArr[0] = calculateDepth ? 1 : 0;
        gl.glUniform1iv(calculateDepthRef, 1, intArr, 0);
    }

    public void bindIsDiff(GL3 gl, int isDiff) {
        intArr[0] = isDiff;
        gl.glUniform1iv(isDiffRef, 1, intArr, 0);
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

    public void bindRadii(GL3 gl, float innerRadius, float outerRadius) {
        floatArr[0] = innerRadius;
        floatArr[1] = outerRadius;
        gl.glUniform1fv(radiiRef, 2, floatArr, 0);
    }

    public void bindPolarRadii(GL3 gl, double start, double stop) {
        floatArr[0] = (float) start;
        floatArr[1] = (float) stop;
        gl.glUniform1fv(polarRadiiRef, 2, floatArr, 0);
    }

}
