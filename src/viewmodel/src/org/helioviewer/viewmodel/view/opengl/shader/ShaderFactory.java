package org.helioviewer.viewmodel.view.opengl.shader;

import java.io.InputStream;

import javax.media.opengl.GL2;

import org.helioviewer.base.FileUtils;
import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.gui.states.ViewStateEnum;

public class ShaderFactory {
    private static boolean init = false;
    private static int fragment3dCGID = -1;
    private static int fragment2dCGID = -1;
    private static int vertex2dCGID = -1;
    private static int vertex3dCGID = -1;
    /* Fragment */
    public static final int truncationValueRef = 0;
    public static final int isDifferenceValueRef = 1;
    public static final int sharpenParamRef = 2;
    public static final int gammaParamRef = 3;
    public static final int contrastParamRef = 4;
    public static final int alphaParamRef = 5;
    public static final int cutOffRadiusRef = 6;
    public static final int outerCutOffRadiusRef = 7;
    public static final int phiParamRef = 8;
    public static final int thetaParamRef = 9;

    public static final double[] truncationValueFloat = new double[4];
    public static final double[] isDifferenceValueFloat = new double[4];
    public static final double[] sharpenParamFloat = new double[4];
    public static final double[] gammaParamFloat = new double[4];
    public static final double[] contrastParamFloat = new double[4];
    public static final double[] alphaParamFloat = new double[4];
    public static final double[] phiParamFloat = new double[4];
    public static final double[] thetaParamFloat = new double[4];
    public static final double[] cutOffRadiusFloat = new double[4];
    public static final double[] outerCutOffRadiusFloat = new double[4];

    /* Vertex */
    public static final int rectRef = 0;
    public static final int thetaRef = 1;
    public static final int phiRef = 2;
    public static final int differenceThetaRef = 3;
    public static final int differencePhiRef = 4;
    public static final int offsetRef = 5;
    public static final int differenceRectRef = 6;
    public static final double[] rectVertex = new double[4];
    public static final double[] thetaVertex = new double[4];
    public static final double[] phiVertex = new double[4];
    public static final double[] differenceThetaVertex = new double[4];
    public static final double[] differencePhiVertex = new double[4];
    public static final double[] offsetVertex = new double[4];
    public static final double[] differenceRectVertex = new double[4];

    public ShaderFactory() {
    }

    public static void setCutOffRadius(double cutOffRadius, double outerCutOffRadius) {
        cutOffRadiusFloat[0] = cutOffRadius;
        outerCutOffRadiusFloat[0] = outerCutOffRadius;

    }

    public static void changeAngles(double theta, double phi) {
        thetaParamFloat[0] = theta;
        phiParamFloat[0] = phi;
        thetaVertex[0] = theta;
        phiVertex[0] = phi;
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

    public static int getFragmentId() {
        if (StateController.getInstance().getCurrentState().getType() == ViewStateEnum.View3D) {
            return fragment3dCGID;
        } else {
            return fragment2dCGID;
        }
    }

    public static int getVertexId() {
        if (StateController.getInstance().getCurrentState().getType() == ViewStateEnum.View3D) {
            return vertex3dCGID;
        } else {
            return vertex2dCGID;
        }
    }

    public static void initShader(GL2 gl) {
        if (!init) {
            init = true;
            fragment3dCGID = genShaderID(gl);
            compileShader(gl, "/data/fragment3d.cg", fragment3dCGID, GL2.GL_FRAGMENT_PROGRAM_ARB);
            vertex3dCGID = genShaderID(gl);
            compileShader(gl, "/data/vertex3d.cg", vertex3dCGID, GL2.GL_VERTEX_PROGRAM_ARB);
            fragment2dCGID = genShaderID(gl);
            compileShader(gl, "/data/fragment2d.cg", fragment2dCGID, GL2.GL_FRAGMENT_PROGRAM_ARB);
            vertex2dCGID = genShaderID(gl);
            compileShader(gl, "/data/vertex2d.cg", vertex2dCGID, GL2.GL_VERTEX_PROGRAM_ARB);
        }
    }

    public static int genShaderID(GL2 gl) {
        int[] tmp = new int[1];
        gl.glGenProgramsARB(1, tmp, 0);
        int target = tmp[0];
        return target;
    }

    public static void compileShader(GL2 gl, String inputFile, int target, int type) {
        InputStream fragmentStream = FileUtils.getResourceInputStream(inputFile);
        String fragmentText = FileUtils.convertStreamToString(fragmentStream);

        GLShaderHelper shaderHelper = new GLShaderHelper();
        shaderHelper.compileProgram(gl, type, fragmentText, target);
    }

    public static void bindEnvVars(GL2 gl, int target, int id, double[] param) {
        gl.glProgramLocalParameter4dARB(target, id, param[0], param[1], param[2], param[3]);
    }

    public static void changeRect(double xOffset, double yOffset, double xScale, double yScale) {
        rectVertex[0] = xOffset;
        rectVertex[1] = yOffset;
        rectVertex[2] = xScale;
        rectVertex[3] = yScale;
    }

    public static void changeDifferenceAngles(double theta, double phi) {
        differenceThetaVertex[0] = theta;
        differencePhiVertex[0] = phi;
    }

    public static void setDifferenceRect(double differenceXOffset, double differenceYOffset, double differenceXScale, double differenceYScale) {
        differenceRectVertex[0] = differenceXOffset;
        differenceRectVertex[1] = differenceYOffset;
        differenceRectVertex[2] = differenceXScale;
        differenceRectVertex[3] = differenceYScale;
    }

    public static void bindVertexShader(GL2 gl) {
        gl.glBindProgramARB(GL2.GL_VERTEX_PROGRAM_ARB, ShaderFactory.getVertexId());
        ShaderFactory.bindEnvVars(gl, GL2.GL_VERTEX_PROGRAM_ARB, ShaderFactory.rectRef, ShaderFactory.rectVertex);
        ShaderFactory.bindEnvVars(gl, GL2.GL_VERTEX_PROGRAM_ARB, ShaderFactory.offsetRef, ShaderFactory.offsetVertex);
        ShaderFactory.bindEnvVars(gl, GL2.GL_VERTEX_PROGRAM_ARB, ShaderFactory.differenceRectRef, ShaderFactory.differenceRectVertex);
        ShaderFactory.bindEnvVars(gl, GL2.GL_VERTEX_PROGRAM_ARB, ShaderFactory.thetaRef, ShaderFactory.thetaVertex);
        ShaderFactory.bindEnvVars(gl, GL2.GL_VERTEX_PROGRAM_ARB, ShaderFactory.phiRef, ShaderFactory.phiVertex);
        ShaderFactory.bindEnvVars(gl, GL2.GL_VERTEX_PROGRAM_ARB, ShaderFactory.differenceThetaRef, ShaderFactory.differenceThetaVertex);
        ShaderFactory.bindEnvVars(gl, GL2.GL_VERTEX_PROGRAM_ARB, ShaderFactory.differencePhiRef, ShaderFactory.differencePhiVertex);
    }

    public static void bindFragmentShader(GL2 gl) {
        gl.glBindProgramARB(GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.getFragmentId());
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.cutOffRadiusRef, ShaderFactory.cutOffRadiusFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.outerCutOffRadiusRef, ShaderFactory.outerCutOffRadiusFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.phiParamRef, ShaderFactory.phiParamFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.thetaParamRef, ShaderFactory.thetaParamFloat);
    }
}
