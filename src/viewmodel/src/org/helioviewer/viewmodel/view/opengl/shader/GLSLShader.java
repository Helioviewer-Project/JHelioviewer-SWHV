package org.helioviewer.viewmodel.view.opengl.shader;

import java.io.InputStream;

import javax.media.opengl.GL2;

import org.helioviewer.base.FileUtils;
import org.helioviewer.viewmodel.imagedata.ColorMask;

public class GLSLShader {

    private static final int MAX_TEXTURES = 10;
    private static int vertexID;
    private static int fragmentID;
    private static int progID;

    public static int truncationValueRef;
    public static int isDifferenceValueRef;
    public static int pixelSizeWeightingRef;
    public static int gammaParamRef;
    public static int contrastParamRef;
    public static int alphaParamRef;
    public static int cutOffRadiusRef;
    public static int outerCutOffRadiusRef;
    public static int phiRef;
    public static int thetaRef;

    public static final float[] truncationValueFloat = new float[4];
    public static final float[] isDifferenceValueFloat = new float[4];
    public static final float[] sharpenParamFloat = new float[4];
    public static final float[] gammaParamFloat = new float[4];
    public static final float[] contrastParamFloat = new float[4];
    public static final float[] alphaParamFloat = new float[4];
    public static final float[] phiParamFloat = new float[4];
    public static final float[] thetaParamFloat = new float[4];
    public static final float[] cutOffRadiusFloat = new float[4];
    public static final float[] outerCutOffRadiusFloat = new float[4];

    /* Vertex */
    public static int rectRef;
    public static int differenceThetaRef;
    public static int differencePhiRef;
    public static int differenceRectRef;
    public static final float[] rectVertex = new float[4];
    public static final float[] differenceThetaVertex = new float[4];
    public static final float[] differencePhiVertex = new float[4];
    public static final float[] differenceRectVertex = new float[4];
    public static ColorMask colorMask = new ColorMask();
    public static boolean init = false;

    public static void initShader(GL2 gl) {
        if (!init) {
            init = true;

            InputStream fragmentStream = FileUtils.getResourceInputStream("/data/fragment3d.glsl");
            String fragmentText = FileUtils.convertStreamToString(fragmentStream);
            InputStream vertexStream = FileUtils.getResourceInputStream("/data/vertex3d.glsl");
            String vertexText = FileUtils.convertStreamToString(vertexStream);
            ;
            attachVertexShader(gl, vertexText);
            attachFragmentShader(gl, fragmentText);

            initializeProgram(gl, true);
            truncationValueRef = gl.glGetUniformLocation(progID, "truncationValue");
            isDifferenceValueRef = gl.glGetUniformLocation(progID, "isdifference");
            pixelSizeWeightingRef = gl.glGetUniformLocation(progID, "pixelSizeWeighting");
            gammaParamRef = gl.glGetUniformLocation(progID, "gamma");
            contrastParamRef = gl.glGetUniformLocation(progID, "contrast");
            alphaParamRef = gl.glGetUniformLocation(progID, "alpha");
            cutOffRadiusRef = gl.glGetUniformLocation(progID, "cutOffRadius");
            outerCutOffRadiusRef = gl.glGetUniformLocation(progID, "outerCutOffRadius");
            phiRef = gl.glGetUniformLocation(progID, "phi");
            thetaRef = gl.glGetUniformLocation(progID, "theta");
            differencePhiRef = gl.glGetUniformLocation(progID, "differencephi");
            differenceThetaRef = gl.glGetUniformLocation(progID, "differencetheta");
            rectRef = gl.glGetUniformLocation(progID, "rect");
            differenceRectRef = gl.glGetUniformLocation(progID, "differenceRect");
            bind(gl);
            setTextureUnit(gl, "image", 0);
            setTextureUnit(gl, "lut", 1);
            setTextureUnit(gl, "differenceImage", 2);
            unbind(gl);
        }

    }

    public static void destroy(GL2 gl) {
        gl.glDeleteShader(vertexID);
        gl.glDeleteShader(fragmentID);

        gl.glDeleteProgram(progID);

    }

    public static void bind(GL2 gl) {
        gl.glUseProgram(progID);
    }

    public static void bindVars(GL2 gl) {
        GLSLShader.setUniform(gl, GLSLShader.rectRef, GLSLShader.rectVertex, 4);
        GLSLShader.setUniform(gl, GLSLShader.differenceRectRef, GLSLShader.differenceRectVertex, 4);
        GLSLShader.setUniform(gl, GLSLShader.differenceThetaRef, GLSLShader.differenceThetaVertex, 1);
        GLSLShader.setUniform(gl, GLSLShader.differencePhiRef, GLSLShader.differencePhiVertex, 1);
        GLSLShader.setUniform(gl, GLSLShader.cutOffRadiusRef, GLSLShader.cutOffRadiusFloat, 4);
        GLSLShader.setUniform(gl, GLSLShader.outerCutOffRadiusRef, GLSLShader.outerCutOffRadiusFloat, 4);
        GLSLShader.setUniform(gl, GLSLShader.phiRef, GLSLShader.phiParamFloat, 1);
        GLSLShader.setUniform(gl, GLSLShader.thetaRef, GLSLShader.thetaParamFloat, 1);
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

    public static void bindTexture(GL2 gl, int target, String texname, int texid, int texunit) {
        gl.glActiveTexture(GL2.GL_TEXTURE0 + texunit);
        gl.glBindTexture(target, texid);
        gl.glActiveTexture(GL2.GL_TEXTURE0);
    }

    public void bindTexture2D(GL2 gl, String texname, int texid, int texunit) {
        bindTexture(gl, GL2.GL_TEXTURE_2D, texname, texid, texunit);
    }

    public void bindTexture3D(GL2 gl, String texname, int texid, int texunit) {
        bindTexture(gl, GL2.GL_TEXTURE_3D, texname, texid, texunit);
    }

    public void bindTextureRECT(GL2 gl, String texname, int texid, int texunit) {
        bindTexture(gl, GL2.GL_TEXTURE_RECTANGLE, texname, texid, texunit);
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

    public static void changeDifferenceAngles(double theta, double phi) {
        differenceThetaVertex[0] = (float) theta;
        differencePhiVertex[0] = (float) phi;
    }

    public static void setDifferenceRect(double differenceXOffset, double differenceYOffset, double differenceXScale, double differenceYScale) {
        differenceRectVertex[0] = (float) differenceXOffset;
        differenceRectVertex[1] = (float) differenceYOffset;
        differenceRectVertex[2] = (float) differenceXScale;
        differenceRectVertex[3] = (float) differenceYScale;
    }

    public static void filter(GL2 gl) {
        //contrast
        GLSLShader.setUniform(gl, GLSLShader.contrastParamRef, GLSLShader.contrastParamFloat, 1);
        //channelmixer
        gl.glColorMask(colorMask.showRed(), colorMask.showGreen(), colorMask.showBlue(), true);
        //difference
        GLSLShader.setUniform(gl, GLSLShader.truncationValueRef, GLSLShader.truncationValueFloat, 1);
        GLSLShader.setUniform(gl, GLSLShader.isDifferenceValueRef, GLSLShader.isDifferenceValueFloat, 1);
        //gamma
        GLSLShader.setUniform(gl, GLSLShader.gammaParamRef, GLSLShader.gammaParamFloat, 1);
        //opacity
        GLSLShader.setUniform(gl, GLSLShader.alphaParamRef, GLSLShader.alphaParamFloat, 1);
        //sharpen
        GLSLShader.setUniform(gl, GLSLShader.pixelSizeWeightingRef, GLSLShader.sharpenParamFloat, 4);

    }

    public static void setCutOffRadius(double cutOffRadius, double outerCutOffRadius) {
        cutOffRadiusFloat[0] = (float) cutOffRadius;
        outerCutOffRadiusFloat[0] = (float) outerCutOffRadius;
    }

    public static void changeAngles(double theta, double phi) {
        thetaParamFloat[0] = (float) theta;
        phiParamFloat[0] = (float) -phi;
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

    public static void setIsDifference(float isDifference) {
        isDifferenceValueFloat[0] = isDifference;
    }

    public static void setTruncationValue(float truncationValue) {
        truncationValueFloat[0] = truncationValue;
    }
};
