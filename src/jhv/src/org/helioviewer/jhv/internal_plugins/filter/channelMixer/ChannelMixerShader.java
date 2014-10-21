package org.helioviewer.jhv.internal_plugins.filter.channelMixer;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.viewmodel.view.opengl.shader.GLTextureCoordinate;

public class ChannelMixerShader extends GLFragmentShaderProgram {
    private GLTextureCoordinate isDifference;

    private static int ID = 0;
    int mode = -1;
    private int channelMixerValueRef;
    private GLShaderBuilder builder;

    private double[] channelMixerValue;

    public void setChannelMixerValue(GL2 gl, float red, float green, float blue) {
        this.channelMixerValue[0] = red;
        this.channelMixerValue[1] = green;
        this.channelMixerValue[2] = blue;
        this.channelMixerValue[3] = 1.;
    }

    @Override
    public void bind(GL2 gl) {
        super.bind(gl);
        this.bindEnvVars(gl, this.channelMixerValueRef, channelMixerValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        this.builder = shaderBuilder;
        try {
            this.channelMixerValueRef = shaderBuilder.addEnvParameter("float4 channelMixerValue");
            this.channelMixerValue = this.builder.getEnvParameter(this.channelMixerValueRef);

            String program = "";
            program += "\toutput = output * channelMixerValue;" + GLShaderBuilder.LINE_SEP;
            //program += "\toutput.g = output.g * channelMixerValue.g;" + GLShaderBuilder.LINE_SEP;
            //program += "\toutput.b = output.b * channelMixerValue.b;" + GLShaderBuilder.LINE_SEP;

            program = program.replaceAll("output", shaderBuilder.useOutputValue("float4", "COLOR"));

            shaderBuilder.addMainFragment(program);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }

    public void activateDifferenceTexture(GL2 gl) {
    }
}
