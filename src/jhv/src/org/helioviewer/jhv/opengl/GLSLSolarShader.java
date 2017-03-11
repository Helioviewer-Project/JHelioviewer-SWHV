package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.math.Quat;

import com.jogamp.opengl.GL2;

public class GLSLSolarShader extends GLSLShader {

    private static final float[] blurKernel;
    private static final float[] offset = new float[] { -2, -1, 0, 1, 2 };

    static {
        float[] v = new float[] { 0.06136f, 0.24477f, 0.38774f, 0.24477f, 0.06136f };
        blurKernel = new float[] {
            v[0] * v[0], v[0] * v[1], v[0] * v[2], v[0] * v[3], v[0] * v[4],
            v[1] * v[0], v[1] * v[1], v[1] * v[2], v[1] * v[3], v[1] * v[4],
            v[2] * v[0], v[2] * v[1], v[2] * v[2], v[2] * v[3], v[2] * v[4],
            v[3] * v[0], v[3] * v[1], v[3] * v[2], v[3] * v[3], v[3] * v[4],
            v[4] * v[0], v[4] * v[1], v[4] * v[2], v[4] * v[3], v[4] * v[4],
        };
    }

    public static final GLSLSolarShader ortho = new GLSLSolarShader("/data/vertex.glsl", "/data/fragmentortho.glsl");
    public static final GLSLSolarShader lati = new GLSLSolarShader("/data/vertex.glsl", "/data/fragmentlati.glsl");
    public static final GLSLSolarShader polar = new GLSLSolarShader("/data/vertex.glsl", "/data/fragmentpolar.glsl");
    public static final GLSLSolarShader logpolar = new GLSLSolarShader("/data/vertex.glsl", "/data/fragmentlogpolar.glsl");

    private int isDifferenceValueRef;
    private int isDiscRef;

    private int sharpenParamRef;
    private int hgltParamRef;
    private int hglnParamRef;

    private int brightParamRef;
    private int colorParamRef;
    private int cutOffRadiusRef;
    private int outerCutOffRadiusRef;
    private int cutOffDirectionRef;
    private int cutOffValueRef;
    private int polarRadiiRef;
    private int enhancedParamRef;

    private int rectRef;
    private int differenceRectRef;
    private int viewportRef;
    private int viewportOffsetRef;

    private int cameraTransformationInverseRef;
    private int cameraDifferenceRotationQuatRef;
    private int diffCameraDifferenceRotationQuatRef;

    private final int[] isDifferenceValue = new int[1];
    private final int[] isDiscValue = new int[1];

    private final float[] sharpenParamFloat = new float[3];
    private final float[] hgltParamFloat = new float[1];
    private final float[] hglnParamFloat = new float[1];
    private final float[] brightParamFloat = new float[2];
    private final float[] colorParamFloat = new float[4];
    private final float[] cutOffRadiusFloat = new float[1];
    private final float[] outerCutOffRadiusFloat = new float[1];
    private final float[] cutOffDirectionFloat = new float[3];
    private final float[] cutOffValueFloat = new float[3];
    private final float[] polarRadii = new float[2];
    private final int[] enhanced = new int[1];

    private final float[] rectVertex = new float[4];
    private final float[] differencerect = new float[4];
    private final float[] viewport = new float[2];

    private final float[] viewportOffset = new float[2];

    public GLSLSolarShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL2 gl) {
        ortho._init(gl);
        lati._init(gl);
        polar._init(gl);
        logpolar._init(gl);
    }

    @Override
    protected void _after_init(GL2 gl) {
        isDifferenceValueRef = gl.glGetUniformLocation(progID, "isdifference");
        isDiscRef = gl.glGetUniformLocation(progID, "isdisc");

        hgltParamRef = gl.glGetUniformLocation(progID, "hglt");
        hglnParamRef = gl.glGetUniformLocation(progID, "hgln");
        polarRadiiRef = gl.glGetUniformLocation(progID, "polarRadii");

        sharpenParamRef = gl.glGetUniformLocation(progID, "sharpenParam");
        brightParamRef = gl.glGetUniformLocation(progID, "brightness");
        colorParamRef = gl.glGetUniformLocation(progID, "colorParam");
        cutOffRadiusRef = gl.glGetUniformLocation(progID, "cutOffRadius");
        outerCutOffRadiusRef = gl.glGetUniformLocation(progID, "outerCutOffRadius");
        cutOffDirectionRef = gl.glGetUniformLocation(progID, "cutOffDirection");
        cutOffValueRef = gl.glGetUniformLocation(progID, "cutOffValue");
        enhancedParamRef = gl.glGetUniformLocation(progID, "enhanced");

        rectRef = gl.glGetUniformLocation(progID, "rect");
        differenceRectRef = gl.glGetUniformLocation(progID, "differencerect");
        viewportRef = gl.glGetUniformLocation(progID, "viewport");
        viewportOffsetRef = gl.glGetUniformLocation(progID, "viewportOffset");

        cameraTransformationInverseRef = gl.glGetUniformLocation(progID, "cameraTransformationInverse");
        cameraDifferenceRotationQuatRef = gl.glGetUniformLocation(progID, "cameraDifferenceRotationQuat");
        diffCameraDifferenceRotationQuatRef = gl.glGetUniformLocation(progID, "diffcameraDifferenceRotationQuat");

        int blurKernelRef = gl.glGetUniformLocation(progID, "blurKernel");
        int offsetRef = gl.glGetUniformLocation(progID, "offset");

        bind(gl);
        gl.glUniform1fv(blurKernelRef, blurKernel.length, blurKernel, 0);
        gl.glUniform1fv(offsetRef, offset.length, offset, 0);

        setTextureUnit(gl, "image", 0);
        setTextureUnit(gl, "lut", 1);
        setTextureUnit(gl, "differenceImage", 2);
        unbind(gl);
        setCutOffValue(-1f);
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

    public static void setUniform(GL2 gl, int id, float[] val, int count) {
        switch (count) {
        case 1:
            gl.glUniform1fv(id, 1, val, 0);
            break;
        case 2:
            gl.glUniform2fv(id, 1, val, 0);
            break;
        case 3:
            gl.glUniform3fv(id, 1, val, 0);
            break;
        case 4:
            gl.glUniform4fv(id, 1, val, 0);
            break;
        default:
            break;
        }
    }

    public void changeRect(double xOffset, double yOffset, double xScale, double yScale) {
        rectVertex[0] = (float) xOffset;
        rectVertex[1] = (float) yOffset;
        rectVertex[2] = (float) xScale;
        rectVertex[3] = (float) yScale;
    }

    public void setDifferenceRect(double differenceXOffset, double differenceYOffset, double differenceXScale, double differenceYScale) {
        differencerect[0] = (float) differenceXOffset;
        differencerect[1] = (float) differenceYOffset;
        differencerect[2] = (float) differenceXScale;
        differencerect[3] = (float) differenceYScale;
    }

    public void filter(GL2 gl) {
        gl.glUniform1iv(isDifferenceValueRef, 1, isDifferenceValue, 0);
        gl.glUniform1fv(hgltParamRef, 1, hgltParamFloat, 0);
        gl.glUniform1fv(hglnParamRef, 1, hglnParamFloat, 0);

        gl.glUniform2fv(brightParamRef, 1, brightParamFloat, 0);
        gl.glUniform4fv(colorParamRef, 1, colorParamFloat, 0);
        gl.glUniform3fv(sharpenParamRef, 1, sharpenParamFloat, 0);
        gl.glUniform4fv(rectRef, 1, rectVertex, 0);
        gl.glUniform4fv(differenceRectRef, 1, differencerect, 0);
        gl.glUniform1fv(cutOffRadiusRef, 1, cutOffRadiusFloat, 0);
        gl.glUniform1fv(outerCutOffRadiusRef, 1, outerCutOffRadiusFloat, 0);
        gl.glUniform2fv(viewportRef, 1, viewport, 0);
        gl.glUniform2fv(viewportOffsetRef, 1, viewportOffset, 0);
        gl.glUniform3fv(cutOffDirectionRef, 1, cutOffDirectionFloat, 0);
        gl.glUniform1fv(cutOffValueRef, 1, cutOffValueFloat, 0);
    }

    public void bindIsDisc(GL2 gl, int isDisc) {
        isDiscValue[0] = isDisc;
        gl.glUniform1iv(isDiscRef, 1, isDiscValue, 0);
    }

    public void setCutOffRadius(double cutOffRadius, double outerCutOffRadius) {
        cutOffRadiusFloat[0] = (float) cutOffRadius;
        outerCutOffRadiusFloat[0] = (float) outerCutOffRadius;
    }

    public void setOuterCutOffRadius(double cutOffRadius) {
        outerCutOffRadiusFloat[0] = (float) cutOffRadius;
    }

    public void setColor(float red, float green, float blue, float alpha) {
        colorParamFloat[0] = red;
        colorParamFloat[1] = green;
        colorParamFloat[2] = blue;
        colorParamFloat[3] = alpha;
    }

    public void setBrightness(float offset, float scale) {
        brightParamFloat[0] = offset;
        brightParamFloat[1] = scale;
    }

    public void setSharpen(float weighting, float pixelWidth, float pixelHeight, float span) {
        sharpenParamFloat[0] = pixelWidth * span;
        sharpenParamFloat[1] = pixelHeight * span;
        sharpenParamFloat[2] = -weighting; // used for mix
    }

    public void setIsDifference(int isDifference) {
        isDifferenceValue[0] = isDifference;
    }

    public void setViewport(float offsetX, float offsetY, float width, float height) {
        viewportOffset[0] = offsetX;
        viewportOffset[1] = offsetY;
        viewport[0] = width;
        viewport[1] = height;
    }

    public void setCutOffValue(float val) {
        cutOffValueFloat[0] = val;
    }

    public void setCutOffDirection(float x, float y, float z) {
        cutOffDirectionFloat[0] = x;
        cutOffDirectionFloat[1] = y;
        cutOffDirectionFloat[2] = z;
    }

    public void bindAngles(GL2 gl, Position.L viewpointL) {
        hgltParamFloat[0] = (float) viewpointL.lat;
        hglnParamFloat[0] = (float) ((viewpointL.lon + 2. * Math.PI) % (2. * Math.PI));
        gl.glUniform1fv(hgltParamRef, 1, hgltParamFloat, 0);
        gl.glUniform1fv(hglnParamRef, 1, hglnParamFloat, 0);
    }

    public void setPolarRadii(GL2 gl, double start, double stop) {
        polarRadii[0] = (float) start;
        polarRadii[1] = (float) stop;
        gl.glUniform2fv(polarRadiiRef, 1, polarRadii, 0);
    }

    public void setEnhanced(GL2 gl, boolean _enhanced) {
        if (_enhanced) {
            enhanced[0] = 1;
            gl.glUniform1iv(enhancedParamRef, 1, enhanced, 0);
        } else {
            enhanced[0] = 0;
            gl.glUniform1iv(enhancedParamRef, 1, enhanced, 0);
        }
    }

}
