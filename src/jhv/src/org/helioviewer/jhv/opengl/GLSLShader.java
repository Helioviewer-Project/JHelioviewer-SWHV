package org.helioviewer.jhv.opengl;

import java.io.InputStream;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.viewmodel.imagedata.ColorMask;

import com.jogamp.opengl.GL2;

public class GLSLShader {

    public static final int NODIFFERENCE = 0;
    public static final int RUNNINGDIFFERENCE_NO_ROT = 1;
    public static final int RUNNINGDIFFERENCE_ROT = 2;
    public static final int BASEDIFFERENCE_NO_ROT = 3;
    public static final int BASEDIFFERENCE_ROT = 4;

    private static int vertexID;
    private static int fragmentID;
    private static int progID;

    public static int truncationValueRef;
    public static int isDifferenceValueRef;
    public static int isDiscRef;

    public static int pixelSizeWeightingRef;
    public static int gammaParamRef;
    public static int contrastParamRef;
    public static int alphaParamRef;
    public static int cutOffRadiusRef;
    public static int outerCutOffRadiusRef;
    public static int cutOffDirectionRef;
    public static int cutOffValueRef;

    public static int rectRef;
    public static int differenceRectRef;
    public static int viewportRef;
    public static int viewportOffsetRef;

    private static int cameraTransformationInverseRef;
    private static int cameraDifferenceRotationQuatRef;
    private static int diffCameraDifferenceRotationQuatRef;

    public static final int[] isDifferenceValue = new int[1];
    public static final int[] isDiscValue = new int[1];

    public static final float[] sharpenParamFloat = new float[3];
    public static final float[] truncationValueFloat = new float[1];
    public static final float[] gammaParamFloat = new float[1];
    public static final float[] contrastParamFloat = new float[1];
    public static final float[] alphaParamFloat = new float[1];
    public static final float[] cutOffRadiusFloat = new float[1];
    public static final float[] outerCutOffRadiusFloat = new float[1];
    public static final float[] cutOffDirectionFloat = new float[3];
    public static final float[] cutOffValueFloat = new float[3];

    public static final float[] rectVertex = new float[4];
    public static final float[] differencerect = new float[4];
    public static final float[] viewport = new float[2];
    public static final float[] viewportOffset = new float[2];

    public static ColorMask colorMask = new ColorMask();

    public static void init(GL2 gl) {
        InputStream fragmentStream = FileUtils.getResourceInputStream("/data/fragment3d.glsl");
        String fragmentText = FileUtils.convertStreamToString(fragmentStream);
        InputStream vertexStream = FileUtils.getResourceInputStream("/data/vertex3d.glsl");
        String vertexText = FileUtils.convertStreamToString(vertexStream);

        attachVertexShader(gl, vertexText);
        attachFragmentShader(gl, fragmentText);

        initializeProgram(gl, true);
        truncationValueRef = gl.glGetUniformLocation(progID, "truncationValue");
        isDifferenceValueRef = gl.glGetUniformLocation(progID, "isdifference");
        isDiscRef = gl.glGetUniformLocation(progID, "isdisc");

        pixelSizeWeightingRef = gl.glGetUniformLocation(progID, "pixelSizeWeighting");
        gammaParamRef = gl.glGetUniformLocation(progID, "gamma");
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
        gl.glDeleteShader(vertexID);
        gl.glDeleteShader(fragmentID);
        gl.glDeleteProgram(progID);
    }

    public static void bind(GL2 gl) {
        gl.glUseProgram(progID);
    }

    public static void bindMatrix(GL2 gl, float[] matrix) {
        gl.glUniformMatrix4fv(cameraTransformationInverseRef, 1, false, matrix, 0);
    }

    public static void bindCameraDifferenceRotationQuat(GL2 gl, GL3DQuatd quat) {
        gl.glUniform4fv(cameraDifferenceRotationQuatRef, 1, quat.getFloatArray(), 0);
    }

    public static void bindDiffCameraDifferenceRotationQuat(GL2 gl, GL3DQuatd quat) {
        gl.glUniform4fv(diffCameraDifferenceRotationQuatRef, 1, quat.getFloatArray(), 0);
    }

    public static void unbind(GL2 gl) {
        gl.glUseProgram(0);
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
        }
    }

    public static void setTextureUnit(GL2 gl, String texname, int texunit) {
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

    public static void attachVertexShader(GL2 gl, String vertexText) {
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

    public static void attachFragmentShader(GL2 gl, String fragmentText) {
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

    public static void initializeProgram(GL2 gl, boolean cleanUp) {
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

    public static void changeRect(double xOffset, double yOffset, double xScale, double yScale) {
        rectVertex[0] = (float) xOffset;
        rectVertex[1] = (float) yOffset;
        rectVertex[2] = (float) xScale;
        rectVertex[3] = (float) yScale;
    }

    public static void setDifferenceRect(double differenceXOffset, double differenceYOffset, double differenceXScale, double differenceYScale) {
        differencerect[0] = (float) differenceXOffset;
        differencerect[1] = (float) differenceYOffset;
        differencerect[2] = (float) differenceXScale;
        differencerect[3] = (float) differenceYScale;
    }

    public static void filter(GL2 gl) {
        gl.glColorMask(colorMask.showRed(), colorMask.showGreen(), colorMask.showBlue(), true);
        gl.glUniform1fv(GLSLShader.contrastParamRef, 1, GLSLShader.contrastParamFloat, 0);
        gl.glUniform1fv(GLSLShader.truncationValueRef, 1, GLSLShader.truncationValueFloat, 0);
        gl.glUniform1iv(GLSLShader.isDifferenceValueRef, 1, GLSLShader.isDifferenceValue, 0);
        gl.glUniform1fv(GLSLShader.gammaParamRef, 1, GLSLShader.gammaParamFloat, 0);
        gl.glUniform1fv(GLSLShader.alphaParamRef, 1, GLSLShader.alphaParamFloat, 0);
        gl.glUniform3fv(GLSLShader.pixelSizeWeightingRef, 1, GLSLShader.sharpenParamFloat, 0);
        gl.glUniform4fv(GLSLShader.rectRef, 1, GLSLShader.rectVertex, 0);
        gl.glUniform4fv(GLSLShader.differenceRectRef, 1, GLSLShader.differencerect, 0);
        gl.glUniform1fv(GLSLShader.cutOffRadiusRef, 1, GLSLShader.cutOffRadiusFloat, 0);
        gl.glUniform1fv(GLSLShader.outerCutOffRadiusRef, 1, GLSLShader.outerCutOffRadiusFloat, 0);
        gl.glUniform2fv(GLSLShader.viewportRef, 1, GLSLShader.viewport, 0);
        gl.glUniform2fv(GLSLShader.viewportOffsetRef, 1, GLSLShader.viewportOffset, 0);
        gl.glUniform3fv(GLSLShader.cutOffDirectionRef, 1, GLSLShader.cutOffDirectionFloat, 0);
        gl.glUniform1fv(GLSLShader.cutOffValueRef, 1, GLSLShader.cutOffValueFloat, 0);
    }

    public static void bindIsDisc(GL2 gl, int isDisc) {
        isDiscValue[0] = isDisc;
        gl.glUniform1iv(GLSLShader.isDiscRef, 1, GLSLShader.isDiscValue, 0);
    }

    public static void setCutOffRadius(double cutOffRadius, double outerCutOffRadius) {
        cutOffRadiusFloat[0] = (float) cutOffRadius;
        outerCutOffRadiusFloat[0] = (float) outerCutOffRadius;
    }

    public static void setOuterCutOffRadius(double cutOffRadius) {
        outerCutOffRadiusFloat[0] = (float) cutOffRadius;
    }

    public static void setAlpha(float alpha) {
        alphaParamFloat[0] = alpha;
    }

    public static void setContrast(float contrast) {
        contrastParamFloat[0] = contrast;
    }

    public static void setGamma(float gamma) {
        gammaParamFloat[0] = gamma;
    }

    public static void setFactors(float weighting, float pixelWidth, float pixelHeight, float span) {
        sharpenParamFloat[0] = pixelWidth * span;
        sharpenParamFloat[1] = pixelHeight * span;
        sharpenParamFloat[2] = weighting;
    }

    public static void setIsDifference(int isDifference) {
        isDifferenceValue[0] = isDifference;
    }

    public static void setTruncationValue(float truncationValue) {
        truncationValueFloat[0] = truncationValue;
    }

    public static void setViewport(float width, float height, float offsetX, float offsetY) {
        viewport[0] = width;
        viewport[1] = height;
        viewportOffset[0] = offsetX;
        viewportOffset[1] = offsetY;
    }

    public static void setCutOffValue(float val) {
        cutOffValueFloat[0] = val;
    }

    public static void setCutOffDirection(float x, float y, float z) {
        cutOffDirectionFloat[0] = x;
        cutOffDirectionFloat[1] = y;
        cutOffDirectionFloat[2] = z;
    }
}
