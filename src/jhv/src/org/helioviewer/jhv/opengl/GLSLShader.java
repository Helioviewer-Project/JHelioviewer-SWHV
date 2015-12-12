package org.helioviewer.jhv.opengl;

import java.io.InputStream;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.viewmodel.imagedata.ColorMask;

import com.jogamp.opengl.GL2;

public class GLSLShader {
    public static GLSLShader ortho = new GLSLShader("/data/vertexortho.glsl", "/data/fragmentortho.glsl");
    public static GLSLShader lati = new GLSLShader("/data/vertexlati.glsl", "/data/fragmentlati.glsl");
    public static GLSLShader polar = new GLSLShader("/data/vertexpolar.glsl", "/data/fragmentpolar.glsl");

    public static final int NODIFFERENCE = 0;
    public static final int RUNNINGDIFFERENCE_NO_ROT = 1;
    public static final int RUNNINGDIFFERENCE_ROT = 2;
    public static final int BASEDIFFERENCE_NO_ROT = 3;
    public static final int BASEDIFFERENCE_ROT = 4;

    private int vertexID;
    private int fragmentID;
    private int progID;

    private int truncationValueRef;
    private int isDifferenceValueRef;
    private int isDiscRef;

    private int pixelSizeWeightingRef;
    private int gammaParamRef;
    private int hgltParamRef;
    private int hglnParamRef;

    private int contrastParamRef;
    private int alphaParamRef;
    private int cutOffRadiusRef;
    private int outerCutOffRadiusRef;
    private int cutOffDirectionRef;
    private int cutOffValueRef;

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
    private final float[] truncationValueFloat = new float[1];
    private final float[] hgltParamFloat = new float[1];
    private final float[] hglnParamFloat = new float[1];
    private final float[] gammaParamFloat = new float[1];
    private final float[] contrastParamFloat = new float[1];
    private final float[] alphaParamFloat = new float[1];
    private final float[] cutOffRadiusFloat = new float[1];
    private final float[] outerCutOffRadiusFloat = new float[1];
    private final float[] cutOffDirectionFloat = new float[3];
    private final float[] cutOffValueFloat = new float[3];

    private final float[] rectVertex = new float[4];
    private final float[] differencerect = new float[4];
    private final float[] viewport = new float[2];

    private final float[] viewportOffset = new float[2];

    private final String vertex;
    private final String fragment;

    public ColorMask colorMask = new ColorMask();

    public GLSLShader(String vertex, String fragment) {
        this.vertex = vertex;
        this.fragment = fragment;
    }

    public static void init(GL2 gl) {
        ortho._init(gl);
        lati._init(gl);
        polar._init(gl);
    }

    private void _init(GL2 gl) {
        InputStream fragmentCommonStream = FileUtils.getResourceInputStream("/data/fragmentcommon.glsl");
        String fragmentCommonText = FileUtils.convertStreamToString(fragmentCommonStream);
        InputStream fragmentStream = FileUtils.getResourceInputStream(fragment);
        String fragmentText = FileUtils.convertStreamToString(fragmentStream);
        fragmentText = fragmentCommonText + fragmentText;
        InputStream vertexStream = FileUtils.getResourceInputStream(vertex);
        String vertexText = FileUtils.convertStreamToString(vertexStream);
        attachVertexShader(gl, vertexText);
        attachFragmentShader(gl, fragmentText);

        initializeProgram(gl, true);
        truncationValueRef = gl.glGetUniformLocation(progID, "truncationValue");
        isDifferenceValueRef = gl.glGetUniformLocation(progID, "isdifference");
        isDiscRef = gl.glGetUniformLocation(progID, "isdisc");

        pixelSizeWeightingRef = gl.glGetUniformLocation(progID, "pixelSizeWeighting");
        gammaParamRef = gl.glGetUniformLocation(progID, "gamma");
        hgltParamRef = gl.glGetUniformLocation(progID, "hglt");
        hglnParamRef = gl.glGetUniformLocation(progID, "hgln");

        contrastParamRef = gl.glGetUniformLocation(progID, "contrast");
        alphaParamRef = gl.glGetUniformLocation(progID, "alpha");
        cutOffRadiusRef = gl.glGetUniformLocation(progID, "cutOffRadius");
        outerCutOffRadiusRef = gl.glGetUniformLocation(progID, "outerCutOffRadius");
        cutOffDirectionRef = gl.glGetUniformLocation(progID, "cutOffDirection");
        cutOffValueRef = gl.glGetUniformLocation(progID, "cutOffValue");

        rectRef = gl.glGetUniformLocation(progID, "rect");
        differenceRectRef = gl.glGetUniformLocation(progID, "differencerect");
        viewportRef = gl.glGetUniformLocation(progID, "viewport");
        viewportOffsetRef = gl.glGetUniformLocation(progID, "viewportOffset");

        cameraTransformationInverseRef = gl.glGetUniformLocation(progID, "cameraTransformationInverse");
        cameraDifferenceRotationQuatRef = gl.glGetUniformLocation(progID, "cameraDifferenceRotationQuat");
        diffCameraDifferenceRotationQuatRef = gl.glGetUniformLocation(progID, "diffcameraDifferenceRotationQuat");
        int unsharpMaskingKernelRef = gl.glGetUniformLocation(progID, "unsharpMaskingKernel");

        bind(gl);
        gl.glUniform1fv(unsharpMaskingKernelRef, 9, new float[] { 1f, 2f, 1f, 2f, 4f, 2f, 1f, 2f, 1f }, 0);

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

    }

    private void _dispose(GL2 gl) {
        gl.glDeleteShader(vertexID);
        gl.glDeleteShader(fragmentID);
        gl.glDeleteProgram(progID);
    }

    public void bind(GL2 gl) {
        gl.glUseProgram(progID);
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

    public void unbind(GL2 gl) {
        gl.glUseProgram(0);
    }

    public void setUniform(GL2 gl, int id, float[] val, int count) {
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
        }
    }

    public void setTextureUnit(GL2 gl, String texname, int texunit) {
        int[] params = new int[] { 0 };
        gl.glGetProgramiv(progID, GL2.GL_LINK_STATUS, params, 0);
        if (params[0] != 1) {
            System.err.println("Error: setTextureUnit needs program to be linked.");
        }
        int id = gl.glGetUniformLocation(progID, texname);
        if (id == -1) {
            System.err.println("Warning: Invalid texture " + texname);
            return;
        }
        gl.glUniform1i(id, texunit);
    }

    public void attachVertexShader(GL2 gl, String vertexText) {
        int iID = gl.glCreateShader(GL2.GL_VERTEX_SHADER);

        String[] akProgramText = new String[1];
        akProgramText[0] = vertexText;

        int[] params = new int[] { 0 };

        int[] aiLength = new int[1];
        aiLength[0] = akProgramText[0].length();
        int iCount = 1;

        gl.glShaderSource(iID, iCount, akProgramText, aiLength, 0);

        gl.glCompileShader(iID);

        gl.glGetShaderiv(iID, GL2.GL_COMPILE_STATUS, params, 0);

        if (params[0] != 1) {
            System.err.println("compile status: " + params[0]);
            gl.glGetShaderiv(iID, GL2.GL_INFO_LOG_LENGTH, params, 0);
            System.err.println("log length: " + params[0]);
            byte[] abInfoLog = new byte[params[0]];
            gl.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);
            System.err.println(new String(abInfoLog));
            System.exit(-1);
        }
        vertexID = iID;
    }

    public void attachFragmentShader(GL2 gl, String fragmentText) {
        int iID = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);

        String[] akProgramText = new String[1];
        akProgramText[0] = fragmentText;

        int[] params = new int[] { 0 };

        int[] aiLength = new int[1];
        aiLength[0] = akProgramText[0].length();
        int iCount = 1;

        gl.glShaderSource(iID, iCount, akProgramText, aiLength, 0);

        gl.glCompileShader(iID);

        gl.glGetShaderiv(iID, GL2.GL_COMPILE_STATUS, params, 0);

        if (params[0] != 1) {
            System.err.println("compile status: " + params[0]);
            gl.glGetShaderiv(iID, GL2.GL_INFO_LOG_LENGTH, params, 0);
            System.err.println("log length: " + params[0]);
            byte[] abInfoLog = new byte[params[0]];
            gl.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);
            System.err.println(new String(abInfoLog));
            System.exit(-1);
        }
        fragmentID = iID;

    }

    public void initializeProgram(GL2 gl, boolean cleanUp) {
        progID = gl.glCreateProgram();
        gl.glAttachShader(progID, vertexID);
        gl.glAttachShader(progID, fragmentID);

        gl.glBindAttribLocation(progID, 0, "position");
        gl.glBindAttribLocation(progID, 1, "vertexUV");
        gl.glLinkProgram(progID);
        int[] params = new int[] { 0 };
        gl.glGetProgramiv(progID, GL2.GL_LINK_STATUS, params, 0);
        if (params[0] != 1) {
            System.err.println("link status: " + params[0]);
            gl.glGetProgramiv(progID, GL2.GL_INFO_LOG_LENGTH, params, 0);
            System.err.println("log length: " + params[0]);
            byte[] abInfoLog = new byte[params[0]];
            gl.glGetProgramInfoLog(progID, params[0], params, 0, abInfoLog, 0);
            System.err.println(new String(abInfoLog));
        }

        gl.glValidateProgram(progID);

        if (cleanUp) {
            gl.glDetachShader(progID, vertexID);
            gl.glDeleteShader(vertexID);
            gl.glDetachShader(progID, fragmentID);
            gl.glDeleteShader(fragmentID);
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
        gl.glColorMask(colorMask.showRed(), colorMask.showGreen(), colorMask.showBlue(), true);
        gl.glUniform1fv(contrastParamRef, 1, contrastParamFloat, 0);
        gl.glUniform1fv(truncationValueRef, 1, truncationValueFloat, 0);
        gl.glUniform1iv(isDifferenceValueRef, 1, isDifferenceValue, 0);
        gl.glUniform1fv(hgltParamRef, 1, hgltParamFloat, 0);
        gl.glUniform1fv(hglnParamRef, 1, hglnParamFloat, 0);

        gl.glUniform1fv(gammaParamRef, 1, gammaParamFloat, 0);
        gl.glUniform1fv(alphaParamRef, 1, alphaParamFloat, 0);
        gl.glUniform3fv(pixelSizeWeightingRef, 1, sharpenParamFloat, 0);
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

    public void setAlpha(float alpha) {
        alphaParamFloat[0] = alpha;
    }

    public void setContrast(float contrast) {
        contrastParamFloat[0] = contrast;
    }

    public void setGamma(float gamma) {
        gammaParamFloat[0] = gamma;
    }

    public void setFactors(float weighting, float pixelWidth, float pixelHeight, float span) {
        sharpenParamFloat[0] = pixelWidth * span;
        sharpenParamFloat[1] = pixelHeight * span;
        sharpenParamFloat[2] = weighting;
    }

    public void setIsDifference(int isDifference) {
        isDifferenceValue[0] = isDifference;
    }

    public void setTruncationValue(float truncationValue) {
        truncationValueFloat[0] = truncationValue;
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
}
