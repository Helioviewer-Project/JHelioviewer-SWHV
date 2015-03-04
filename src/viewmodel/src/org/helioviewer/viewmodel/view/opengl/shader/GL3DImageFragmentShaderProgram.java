package org.helioviewer.viewmodel.view.opengl.shader;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.shaderfactory.ShaderFactory;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;

public class GL3DImageFragmentShaderProgram extends GLFragmentShaderProgram {

    private GLShaderBuilder builder;

    private final int cutOffRadiusRef = 6;
    private final int outerCutOffRadiusRef = 7;

    private GLShaderBuilder builder;
    private final int phiParamRef = 8;
    private final double[] phiParamFloat = new double[4];
    private final int thetaParamRef = 9;
    private final double[] thetaParamFloat = new double[4];
    private final int differencePhiParamRef = 10;
    private final double[] differencePhiParamFloat = new double[4];
    private final int differenceThetaParamRef = 11;
    private final double[] differenceThetaParamFloat = new double[4];
    private final int localShaderID = 0;
    private final double[] cutOffRadiusFloat = new double[4];
    private final double[] outerCutOffRadiusFloat = new double[4];

    public GL3DImageFragmentShaderProgram() {

    }

    /**
     * Binds (= activates it) the shader, if it is not active so far.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    @Override
    public final void bind(GL2 gl) {
        bind(gl, localShaderID, outerCutOffRadius, cutOffRadius);
    }

    private void bind(GL2 gl, int shader, double outerCutOffRadius, double cutOffRadius) {
        gl.glBindProgramARB(GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.getFragmentId());
        cutOffRadiusFloat[0] = cutOffRadius;
        outerCutOffRadiusFloat[0] = outerCutOffRadius;
        this.bindEnvVars(gl, this.cutOffRadiusRef, cutOffRadiusFloat);
        this.bindEnvVars(gl, this.outerCutOffRadiusRef, outerCutOffRadiusFloat);

        this.bindEnvVars(gl, this.phiParamRef, this.phiParamFloat);
        this.bindEnvVars(gl, this.thetaParamRef, this.thetaParamFloat);
        this.bindEnvVars(gl, this.differencePhiParamRef, this.differencePhiParamFloat);
        this.bindEnvVars(gl, this.differenceThetaParamRef, this.differenceThetaParamFloat);
>>>>>>> use shader from file:jhv-3d-wcs/src/jhv-3d/src/org/helioviewer/gl3d/shader/GL3DImageFragmentShaderProgram.java
    }

    public void setCutOffRadius(double cutOffRadius) {
        this.cutOffRadius = cutOffRadius;
    }

    public void setOuterCutOffRadius(double outerCutOffRadius) {
        this.outerCutOffRadius = outerCutOffRadius;
    }

    public void changeAngles(double theta, double phi) {
        thetaParamFloat[0] = theta;
        phiParamFloat[0] = phi;
    }

}
