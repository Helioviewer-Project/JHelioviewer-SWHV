package org.helioviewer.jhv.internal_plugins.filter.sharpen;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.filter.GLFragmentShaderFilter;
import org.helioviewer.viewmodel.filter.GLImageSizeFilter;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

/**
 * Extension of SharpenFilter, also providing an OpenGL implementation.
 *
 * <p>
 * For further information about sharpening, see {@link SharpenFilter}.
 *
 * @author Markus Langenberg
 */
public class SharpenGLFilter extends SharpenFilter implements GLFragmentShaderFilter, GLImageSizeFilter {

    private final UnsharpMaskingShader shader = new UnsharpMaskingShader();
    private float pixelWidth, pixelHeight;

    /**
     * Fragment shader performing the unsharp mask algorithm.
     */
    private class UnsharpMaskingShader extends GLFragmentShaderProgram {
        private final int sharpenParamRef = 2;
        private final double[] sharpenParamFloat = new double[4];

        /**
         * Sets all necessary parameters: The size of a pixel and the weighting.
         *
         * @param gl
         *            Valid reference to the current gl object
         * @param weighting
         *            Weighting of the sharpening
         * @param pixelWidth
         *            Width of a pixel = 1/imageWidth
         * @param pixelHeight
         *            Height of a pixel = 1/imageHeight
         */
        public void setFactors(GL2 gl, float weighting, float pixelWidth, float pixelHeight) {
            sharpenParamFloat[0] = pixelWidth * span;
            sharpenParamFloat[1] = pixelHeight * span;
            sharpenParamFloat[2] = weighting;
        }

        @Override
        public void bind(GL2 gl) {
            super.bind(gl);
            this.bindEnvVars(gl, this.sharpenParamRef, sharpenParamFloat);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void buildImpl(GLShaderBuilder shaderBuilder) {

            try {
                shaderBuilder.addEnvParameter("float4 pixelSizeWeighting");

                String program = "\tfloat unsharpMaskingKernel[3][3] = {" + GLShaderBuilder.LINE_SEP + "\t\t{1, 2, 1}," + GLShaderBuilder.LINE_SEP + "\t\t{2, 4, 2}," + GLShaderBuilder.LINE_SEP + "\t\t{1, 2, 1}" + GLShaderBuilder.LINE_SEP + "\t};" + GLShaderBuilder.LINE_SEP + "\tfloat3 tmpConvolutionSum = float3(0, 0, 0);" + GLShaderBuilder.LINE_SEP + "\tfor(int i=0; i<3; i++)" + GLShaderBuilder.LINE_SEP + "\t{" + GLShaderBuilder.LINE_SEP + "\t\tfor(int j=0; j<3; j++)" + GLShaderBuilder.LINE_SEP + "\t\t{" + GLShaderBuilder.LINE_SEP + "\t\t\ttmpConvolutionSum += tex2D(source, texCoord.xy + float2(i-1, j-1) * pixelSizeWeighting.x).rgb" + GLShaderBuilder.LINE_SEP + "\t\t\t\t* unsharpMaskingKernel[i][j];" + GLShaderBuilder.LINE_SEP + "\t\t}" + GLShaderBuilder.LINE_SEP + "\t}" + GLShaderBuilder.LINE_SEP + "\ttmpConvolutionSum = (1 + pixelSizeWeighting.z) * output.rgb" + GLShaderBuilder.LINE_SEP + "\t\t- pixelSizeWeighting.z * tmpConvolutionSum / 16.0f;" + GLShaderBuilder.LINE_SEP + "\toutput.rgb = saturate(tmpConvolutionSum);";

                program = program.replaceAll("source", shaderBuilder.useStandardParameter("sampler2D", "TEXUNIT0"));
                program = program.replaceAll("texCoord", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
                program = program.replaceAll("output", shaderBuilder.useOutputValue("float4", "COLOR"));

                shaderBuilder.addMainFragment(program);

            } catch (GLBuildShaderException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
        shader.build(shaderBuilder);
        return shaderBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyGL(GL2 gl) {
        shader.setFactors(gl, weighting, pixelWidth, pixelHeight);
        shader.bind(gl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImageSize(int width, int height) {
        pixelWidth = 1.0f / width;
        pixelHeight = 1.0f / height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forceRefilter() {
    }
}
