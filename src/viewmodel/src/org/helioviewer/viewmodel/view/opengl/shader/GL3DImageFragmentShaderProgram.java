package org.helioviewer.viewmodel.view.opengl.shader;

import javax.media.opengl.GL2;

public class GL3DImageFragmentShaderProgram extends GLFragmentShaderProgram {

    /**
     * Binds (= activates it) the shader, if it is not active so far.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    @Override
    public final void bind(GL2 gl) {
        gl.glBindProgramARB(GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.getFragmentId());
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.cutOffRadiusRef, ShaderFactory.cutOffRadiusFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.outerCutOffRadiusRef, ShaderFactory.outerCutOffRadiusFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.phiParamRef, ShaderFactory.phiParamFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.thetaParamRef, ShaderFactory.thetaParamFloat);
    }

    public void setCutOffRadius(double cutOffRadius, double outerCutOffRadius) {
        ShaderFactory.setCutOffRadius(cutOffRadius, outerCutOffRadius);

    }

    public void changeAngles(double theta, double phi) {
        ShaderFactory.changeAngles(theta, phi);
    }

}
