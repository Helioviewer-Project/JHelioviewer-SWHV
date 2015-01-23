package org.helioviewer.viewmodel.view.opengl.shader;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

/**
 * Specialized implementation of GLFragmentShaderProgram for filters based on
 * lookup tables.
 *
 * <p>
 * To use this program, it is not necessary to derive another class from this
 * one. A one-dimensional texture is used as the lookup table. To set the lookup
 * table, call {@link #activateLutTexture(GL2)}. This binds the texture id used
 * for the lookup table. After that, the texture can be filled with the lookup
 * data. The rest is done by the class itself.
 *
 * @author Markus Langenberg
 */
public class GLSingleChannelLookupFragmentShaderProgram extends GLFragmentShaderProgram {

    private static int lutID = 0;
    private GLShaderBuilder builder;

    @Override
    public void bind(GL2 gl) {
        super.bind(gl);
        gl.glBindProgramARB(target, shaderID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        this.builder = shaderBuilder;
        try {
            String program = "\toutput.rgb = tex1D(lut, output.r).rgb;";

            program = program.replaceAll("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            shaderBuilder.getParameterList().add("uniform sampler1D lut" + lutID + " : TEXUNIT1");

            program = program.replaceAll("lut", "lut" + lutID);
            lutID = (lutID + 1) & 15;

            shaderBuilder.addMainFragment(program);

        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }
}
