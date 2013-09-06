package org.helioviewer.viewmodel.view.opengl.shader;

import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

/**
 * Minimal code needed to create a correct vertex shader.
 * 
 * <p>
 * In particular, the code block calculates the position by multiplying the
 * input value with combined projection-, viewport- and model-matrix. Also,
 * color and texture coordinate are forwarded to the fragment shader without any
 * changes.
 * 
 * @author Markus Langenberg
 */
public class GLMinimalVertexShaderProgram extends GLVertexShaderProgram {

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            shaderBuilder.useOutputValue("float4", "COLOR", shaderBuilder.useStandardParameter("float4", "COLOR"));
            shaderBuilder.useOutputValue("float4", "TEXCOORD0", "float4(" + shaderBuilder.useStandardParameter("float4", "TEXCOORD0") + ".xy, 0, 1)");

            String init = "mul(mvp, position)";
            init = init.replaceAll("position", shaderBuilder.useStandardParameter("float4", "POSITION"));
            init = init.replaceAll("mvp", shaderBuilder.useStandardParameter("uniform float4x4", "state.matrix.mvp"));
            shaderBuilder.useOutputValue("float4", "POSITION", init);

        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }
}
