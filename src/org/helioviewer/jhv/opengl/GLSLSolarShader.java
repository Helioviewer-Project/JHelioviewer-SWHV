package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;

import com.jogamp.opengl.GL2;

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
    private int enhancedRef;
    private int calculateDepthRef;

    private int viewportRef;
    private int viewportOffsetRef;

    private int cameraTransformationInverseRef;
    private int cameraDifferenceRef;

    private final int[] intArr = new int[1];
    private final float[] floatArr = new float[8];

    private GLSLSolarShader(String vertex, String fragment, boolean _hasCommon) {
        super(vertex, fragment);
        hasCommon = _hasCommon;
    }

    public static void init(GL2 gl) {
        sphere._init(gl, sphere.hasCommon);
        ortho._init(gl, ortho.hasCommon);
        lati._init(gl, lati.hasCommon);
        polar._init(gl, polar.hasCommon);
        logpolar._init(gl, logpolar.hasCommon);
    }

    @Override
    protected void initUniforms(GL2 gl, int id) {
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
        enhancedRef = gl.glGetUniformLocation(id, "enhanced");
        calculateDepthRef = gl.glGetUniformLocation(id, "calculateDepth");

        rectRef = gl.glGetUniformLocation(id, "rect");
        viewportRef = gl.glGetUniformLocation(id, "viewport");
        viewportOffsetRef = gl.glGetUniformLocation(id, "viewportOffset");

        cameraTransformationInverseRef = gl.glGetUniformLocation(id, "cameraTransformationInverse");
        cameraDifferenceRef = gl.glGetUniformLocation(id, "cameraDifference");

        if (hasCommon) {
            setTextureUnit(gl, id, "image", GLTexture.Unit.ZERO);
            setTextureUnit(gl, id, "lut", GLTexture.Unit.ONE);
            setTextureUnit(gl, id, "diffImage", GLTexture.Unit.TWO);
        }
    }

    public static void dispose(GL2 gl) {
        sphere._dispose(gl);
        ortho._dispose(gl);
        lati._dispose(gl);
        polar._dispose(gl);
        logpolar._dispose(gl);
    }

    public void bindInverseCamera(GL2 gl) {
        gl.glUniformMatrix4fv(cameraTransformationInverseRef, 1, false, Transform.getInverse());
    }

    public void bindCameraDifference(GL2 gl, Quat quat, Quat quatDiff) {
        quat.setFloatArray(floatArr, 0);
        quatDiff.setFloatArray(floatArr, 4);
        gl.glUniform4fv(cameraDifferenceRef, 2, floatArr, 0);
    }

    public void bindCRVAL(GL2 gl, Vec2 vec, Vec2 vecDiff) {
        floatArr[0] = (float) vec.x;
        floatArr[1] = (float) vec.y;
        floatArr[2] = (float) vecDiff.x;
        floatArr[3] = (float) vecDiff.y;
        gl.glUniform2fv(crvalRef, 2, floatArr, 0);
    }

    public void bindCROTA(GL2 gl, Quat quat, Quat quatDiff) {
        quat.setFloatArray(floatArr, 0);
        quatDiff.setFloatArray(floatArr, 4);
        gl.glUniform4fv(crotaRef, 2, floatArr, 0);
    }

    public void bindRect(GL2 gl, Region r, Region dr) {
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

    public void bindDeltaT(GL2 gl, double deltaT, double deltaTDiff) {
        floatArr[0] = (float) deltaT;
        floatArr[1] = (float) deltaTDiff;
        gl.glUniform1fv(deltaTRef, 2, floatArr, 0);
    }

    public void bindColor(GL2 gl, float red, float green, float blue, double alpha, double blend) {
        floatArr[0] = (float) (red * alpha);
        floatArr[1] = (float) (green * alpha);
        floatArr[2] = (float) (blue * alpha);
        floatArr[3] = (float) (alpha * blend); // https://amindforeverprogramming.blogspot.com/2013/07/why-alpha-premultiplied-colour-blending.html
        gl.glUniform4fv(colorRef, 1, floatArr, 0);
    }

    public void bindSlit(GL2 gl, double left, double right) {
        floatArr[0] = (float) left;
        floatArr[1] = (float) right;
        gl.glUniform1fv(slitRef, 2, floatArr, 0);
    }

    public void bindBrightness(GL2 gl, double offset, double scale, double gamma) {
        floatArr[0] = (float) offset;
        floatArr[1] = (float) scale;
        floatArr[2] = (float) gamma;
        gl.glUniform3fv(brightRef, 1, floatArr, 0);
    }

    public void bindSharpen(GL2 gl, double weight, double pixelWidth, double pixelHeight) {
        floatArr[0] = (float) pixelWidth;
        floatArr[1] = (float) pixelHeight;
        floatArr[2] = -2 * (float) weight; // used for mix
        gl.glUniform3fv(sharpenRef, 1, floatArr, 0);
    }

    public void bindEnhanced(GL2 gl, boolean enhanced) {
        intArr[0] = enhanced ? 1 : 0;
        gl.glUniform1iv(enhancedRef, 1, intArr, 0);
    }

    public void bindCalculateDepth(GL2 gl, boolean calculateDepth) {
        intArr[0] = calculateDepth ? 1 : 0;
        gl.glUniform1iv(calculateDepthRef, 1, intArr, 0);
    }

    public void bindIsDiff(GL2 gl, int isDiff) {
        intArr[0] = isDiff;
        gl.glUniform1iv(isDiffRef, 1, intArr, 0);
    }

    public void bindViewport(GL2 gl, float offsetX, float offsetY, float width, float height) {
        floatArr[0] = offsetX;
        floatArr[1] = offsetY;
        gl.glUniform2fv(viewportOffsetRef, 1, floatArr, 0);
        floatArr[0] = width;
        floatArr[1] = height;
        floatArr[2] = height / width;
        gl.glUniform3fv(viewportRef, 1, floatArr, 0);
    }

    public void bindCutOffValue(GL2 gl, float val) {
        floatArr[0] = val;
        gl.glUniform1fv(cutOffValueRef, 1, floatArr, 0);
    }

    public void bindCutOffDirection(GL2 gl, float x, float y) {
        floatArr[0] = x;
        floatArr[1] = y;
        gl.glUniform2fv(cutOffDirectionRef, 1, floatArr, 0);
    }

    public void bindAnglesLatiGrid(GL2 gl, double lon, double lat, double hglt, double dlon, double dlat, double dhglt) {
        floatArr[0] = (float) lon;
        floatArr[1] = (float) lat;
        floatArr[2] = (float) hglt;
        floatArr[3] = (float) dlon;
        floatArr[4] = (float) dlat;
        floatArr[5] = (float) dhglt;
        gl.glUniform3fv(gridRef, 2, floatArr, 0);
    }

    public void bindSector(GL2 gl, double sector0, double sector1) {
        floatArr[0] = /*sector0 + 2 * Math.PI == sector1*/ sector0 == sector1 ? 0 : 1; // common case
        floatArr[1] = (float) sector0;
        floatArr[2] = (float) sector1;
        gl.glUniform1fv(sectorRef, 3, floatArr, 0);
    }

    public void bindRadii(GL2 gl, float innerRadius, float outerRadius) {
        floatArr[0] = innerRadius;
        floatArr[1] = outerRadius;
        gl.glUniform1fv(radiiRef, 2, floatArr, 0);
    }

    public void bindPolarRadii(GL2 gl, double start, double stop) {
        floatArr[0] = (float) start;
        floatArr[1] = (float) stop;
        gl.glUniform1fv(polarRadiiRef, 2, floatArr, 0);
    }

}
