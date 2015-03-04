package org.helioviewer.jhv.internal_plugins.filter.difference;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.shaderfactory.ShaderFactory;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;

public class DifferenceShader extends GLFragmentShaderProgram {

    private static int ID = 0;
    int mode = -1;

    private final double[] truncationValueFloat = new double[4];
    private final int truncationValueRef = 0;

    private final double[] isDifferenceValueFloat = new double[4];
    private final int isDifferenceValueRef = 1;

    private GLShaderBuilder builder;

    public void setIsDifference(GL2 gl, float isDifference) {
        this.isDifferenceValueFloat[0] = isDifference;
    }

    public void setTruncationValue(GL2 gl, float truncationValue) {
        this.truncationValueFloat[0] = truncationValue;
    }

    @Override
    public void bind(GL2 gl) {
        gl.glBindProgramARB(GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.getFragmentId());
        this.bindEnvVars(gl, this.truncationValueRef, truncationValueFloat);
        this.bindEnvVars(gl, this.isDifferenceValueRef, isDifferenceValueFloat);
    }

}
