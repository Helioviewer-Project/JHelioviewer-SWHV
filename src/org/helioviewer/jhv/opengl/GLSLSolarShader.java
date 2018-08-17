package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.position.Position;

import com.jogamp.opengl.GL2;

public class GLSLSolarShader extends GLSLShader {

    private static final float[] blurKernel;
    private static final float[] offsets = { -1.2004377f, 0, 1.2004377f };

   static {
        // float[] v = { 0.06136f, 0.24477f, 0.38774f, 0.24477f, 0.06136f };
        // http://rastergrid.com/blog/2010/09/efficient-gaussian-blur-with-linear-sampling/
        float[] v = { .30613f, .38774f, .30613f };

        blurKernel = new float[] {
            v[0] * v[0], v[0] * v[1], v[0] * v[2],
            v[1] * v[0], v[1] * v[1], v[1] * v[2],
            v[2] * v[0], v[2] * v[1], v[2] * v[2],
        };
    }

    public static final GLSLSolarShader ortho = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarOrtho.frag");
    public static final GLSLSolarShader lati = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLati.frag");
    public static final GLSLSolarShader polar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarPolar.frag");
    public static final GLSLSolarShader logpolar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLogPolar.frag");

    private int isDiffRef;

    private int hgltRef;
    private int hglnRef;
    private int hgltDiffRef;
    private int hglnDiffRef;
    private int crotaRef;
    private int crotaDiffRef;

    private int brightRef;
    private int colorRef;
    private int sharpenRef;
    private int cutOffRadiusRef;
    private int cutOffDirectionRef;
    private int cutOffValueRef;
    private int polarRadiiRef;
    private int enhancedRef;

    private int rectRef;
    private int diffRectRef;
    private int viewportRef;
    private int viewportOffsetRef;

    private int cameraTransformationInverseRef;
    private int cameraDifferenceRotationQuatRef;
    private int diffCameraDifferenceRotationQuatRef;

    private final int[] isDiff = new int[1];

    private final float[] hglt = new float[1];
    private final float[] hgln = new float[1];
    private final float[] crota = new float[1];
    private final float[] hgltDiff = new float[1];
    private final float[] hglnDiff = new float[1];
    private final float[] crotaDiff = new float[1];

    private final float[] bright = new float[3];
    private final float[] color = new float[4];
    private final float[] sharpen = new float[3];
    private final float[] cutOffRadius = new float[2];
    private final float[] cutOffDirection = new float[3];
    private final float[] cutOffValue = new float[1];
    private final float[] polarRadii = new float[2];
    private final int[] enhanced = new int[1];

    private final float[] rect = new float[4];
    private final float[] diffRect = new float[4];
    private final float[] viewport = new float[3];
    private final float[] viewportOffset = new float[2];

    private GLSLSolarShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL2 gl) {
        ortho._init(gl, true);
        lati._init(gl, true);
        polar._init(gl, true);
        logpolar._init(gl, true);
    }

    @Override
    protected void _after_init(GL2 gl) {
        isDiffRef = gl.glGetUniformLocation(progID, "isdifference");

        hgltRef = gl.glGetUniformLocation(progID, "hglt");
        hglnRef = gl.glGetUniformLocation(progID, "hgln");
        crotaRef = gl.glGetUniformLocation(progID, "crota");
        hgltDiffRef = gl.glGetUniformLocation(progID, "hgltDiff");
        hglnDiffRef = gl.glGetUniformLocation(progID, "hglnDiff");
        crotaDiffRef = gl.glGetUniformLocation(progID, "crotaDiff");

        polarRadiiRef = gl.glGetUniformLocation(progID, "polarRadii");

        sharpenRef = gl.glGetUniformLocation(progID, "sharpen");
        brightRef = gl.glGetUniformLocation(progID, "brightness");
        colorRef = gl.glGetUniformLocation(progID, "color");
        cutOffRadiusRef = gl.glGetUniformLocation(progID, "cutOffRadius");
        cutOffDirectionRef = gl.glGetUniformLocation(progID, "cutOffDirection");
        cutOffValueRef = gl.glGetUniformLocation(progID, "cutOffValue");
        enhancedRef = gl.glGetUniformLocation(progID, "enhanced");

        rectRef = gl.glGetUniformLocation(progID, "rect");
        diffRectRef = gl.glGetUniformLocation(progID, "differencerect");
        viewportRef = gl.glGetUniformLocation(progID, "viewport");
        viewportOffsetRef = gl.glGetUniformLocation(progID, "viewportOffset");

        cameraTransformationInverseRef = gl.glGetUniformLocation(progID, "cameraTransformationInverse");
        cameraDifferenceRotationQuatRef = gl.glGetUniformLocation(progID, "cameraDifferenceRotationQuat");
        diffCameraDifferenceRotationQuatRef = gl.glGetUniformLocation(progID, "diffcameraDifferenceRotationQuat");

        int blurKernelRef = gl.glGetUniformLocation(progID, "blurKernel");
        int offsetRef = gl.glGetUniformLocation(progID, "offset");

        bind(gl);
        gl.glUniform1fv(blurKernelRef, blurKernel.length, blurKernel, 0);
        gl.glUniform1fv(offsetRef, offsets.length, offsets, 0);
        setTextureUnit(gl, "vertexBuffer", GLTexture.Unit.ZERO);
        setTextureUnit(gl, "image", GLTexture.Unit.ONE);
        setTextureUnit(gl, "lut", GLTexture.Unit.TWO);
        setTextureUnit(gl, "diffImage", GLTexture.Unit.THREE);
        unbind(gl);
    }

    public static void dispose(GL2 gl) {
        ortho._dispose(gl);
        lati._dispose(gl);
        polar._dispose(gl);
        logpolar._dispose(gl);
    }

    public void bindMatrix(GL2 gl, float[] matrix) {
        gl.glUniformMatrix4fv(cameraTransformationInverseRef, 1, false, matrix, 0);
    }

    public void bindCameraDifferenceRotationQuat(GL2 gl, Quat quat) {
        gl.glUniform4fv(cameraDifferenceRotationQuatRef, 1, quat.getFloatArray(), 0);
    }

    public void bindDiffCameraDifferenceRotationQuat(GL2 gl, Quat quat) {
        gl.glUniform4fv(diffCameraDifferenceRotationQuatRef, 1, quat.getFloatArray(), 0);
    }

    public void bindRect(GL2 gl, double xOffset, double yOffset, double xScale, double yScale) {
        rect[0] = (float) xOffset;
        rect[1] = (float) yOffset;
        rect[2] = (float) xScale;
        rect[3] = (float) yScale;
        gl.glUniform4fv(rectRef, 1, rect, 0);
    }

    public void bindDiffRect(GL2 gl, double diffXOffset, double diffYOffset, double diffXScale, double diffYScale) {
        diffRect[0] = (float) diffXOffset;
        diffRect[1] = (float) diffYOffset;
        diffRect[2] = (float) diffXScale;
        diffRect[3] = (float) diffYScale;
        gl.glUniform4fv(diffRectRef, 1, diffRect, 0);
    }

    public void bindColor(GL2 gl, float red, float green, float blue, double alpha, double blend) {
        color[0] = (float) (red * alpha);
        color[1] = (float) (green * alpha);
        color[2] = (float) (blue * alpha);
        color[3] = (float) (alpha * blend); // http://amindforeverprogramming.blogspot.be/2013/07/why-alpha-premultiplied-colour-blending.html
        gl.glUniform4fv(colorRef, 1, color, 0);
    }

    public void bindBrightness(GL2 gl, double offset, double scale, double gamma) {
        bright[0] = (float) offset;
        bright[1] = (float) scale;
        bright[2] = (float) gamma;
        gl.glUniform3fv(brightRef, 1, bright, 0);
    }

    public void bindSharpen(GL2 gl, double weight, double pixelWidth, double pixelHeight) {
        sharpen[0] = (float) (pixelWidth);
        sharpen[1] = (float) (pixelHeight);
        sharpen[2] = (float) (-2 * weight); // used for mix
        gl.glUniform3fv(sharpenRef, 1, sharpen, 0);
    }

    public void bindEnhanced(GL2 gl, boolean _enhanced) {
        enhanced[0] = _enhanced ? 1 : 0;
        gl.glUniform1iv(enhancedRef, 1, enhanced, 0);
    }

    public void bindIsDiff(GL2 gl, int _isDiff) {
        isDiff[0] = _isDiff;
        gl.glUniform1iv(isDiffRef, 1, isDiff, 0);
    }

    public void bindViewport(GL2 gl, float offsetX, float offsetY, float width, float height) {
        viewportOffset[0] = offsetX;
        viewportOffset[1] = offsetY;
        gl.glUniform2fv(viewportOffsetRef, 1, viewportOffset, 0);
        viewport[0] = width;
        viewport[1] = height;
        viewport[2] = height / width;
        gl.glUniform3fv(viewportRef, 1, viewport, 0);
    }

    public void bindCutOffRadius(GL2 gl, double innerCutOffRadius, double outerCutOffRadius) {
        cutOffRadius[0] = (float) innerCutOffRadius;
        cutOffRadius[1] = (float) outerCutOffRadius;
        gl.glUniform2fv(cutOffRadiusRef, 1, cutOffRadius, 0);
    }

    public void bindCutOffValue(GL2 gl, double val) {
        cutOffValue[0] = (float) val;
        gl.glUniform1fv(cutOffValueRef, 1, cutOffValue, 0);
    }

    public void bindCutOffDirection(GL2 gl, double x, double y, double z) {
        cutOffDirection[0] = (float) x;
        cutOffDirection[1] = (float) y;
        cutOffDirection[2] = (float) z;
        gl.glUniform3fv(cutOffDirectionRef, 1, cutOffDirection, 0);
    }

    public void bindAngles(GL2 gl, Position viewpoint, double _crota) {
        hglt[0] = (float) viewpoint.lat;
        hgln[0] = (float) ((viewpoint.lon + 2. * Math.PI) % (2. * Math.PI));
        gl.glUniform1fv(hgltRef, 1, hglt, 0);
        gl.glUniform1fv(hglnRef, 1, hgln, 0);
        crota[0] = (float) _crota;
        gl.glUniform1fv(crotaRef, 1, crota, 0);
    }

    public void bindAnglesDiff(GL2 gl, Position viewpoint, double _crota) {
        hgltDiff[0] = (float) viewpoint.lat;
        hglnDiff[0] = (float) ((viewpoint.lon + 2. * Math.PI) % (2. * Math.PI));
        gl.glUniform1fv(hgltDiffRef, 1, hgltDiff, 0);
        gl.glUniform1fv(hglnDiffRef, 1, hglnDiff, 0);
        crotaDiff[0] = (float) _crota;
        gl.glUniform1fv(crotaDiffRef, 1, crotaDiff, 0);
    }

    public void bindPolarRadii(GL2 gl, double start, double stop) {
        polarRadii[0] = (float) start;
        polarRadii[1] = (float) stop;
        gl.glUniform2fv(polarRadiiRef, 1, polarRadii, 0);
    }

}
