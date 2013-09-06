package org.helioviewer.viewmodel.view.opengl.shader;

import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

/**
 * Minimal code needed to create a correct fragment shader.
 * 
 * <p>
 * In particular, the code block reads the sets the output color to the
 * corresponding pixel given by the standard input texture and texture
 * coordinate (GL_TEXTURE0). It does not use the color.
 * 
 * @author Markus Langenberg
 */
public class GLMinimalFragmentShaderProgram extends GLFragmentShaderProgram {

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String init = "tex2D(source, texCoord.xy)";
            init = init.replaceAll("texCoord", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            init = init.replaceAll("source", shaderBuilder.useStandardParameter("sampler2D", "TEXUNIT0"));
            shaderBuilder.useOutputValue("float4", "COLOR", init);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }
}
