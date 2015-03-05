package org.helioviewer.jhv.internal_plugins.filter.difference;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.ShaderFactory;

public class DifferenceShader extends GLFragmentShaderProgram {

    private static int ID = 0;
    int mode = -1;

    private GLShaderBuilder builder;

    public void setIsDifference(GL2 gl, float isDifference) {
        ShaderFactory.setIsDifference(isDifference);
    }

    public void setTruncationValue(GL2 gl, float truncationValue) {
        ShaderFactory.setTruncationValue(truncationValue);
    }

    @Override
    public void bind(GL2 gl) {
        gl.glBindProgramARB(GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.getFragmentId());
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.truncationValueRef, ShaderFactory.truncationValueFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.isDifferenceValueRef, ShaderFactory.isDifferenceValueFloat);
    }

}
