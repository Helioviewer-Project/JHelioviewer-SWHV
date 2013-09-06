package org.helioviewer.viewmodel.filter;

import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;

/**
 * Filter which is implemented using a fragment shader.
 * 
 * <p>
 * This filter may implement its own fragment shader. By implementing this
 * interface, the filter ensures that it is included in the process of building
 * the final fragment shader.
 * 
 * <p>
 * For further information about how to build shaders, see
 * {@link org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder} as well
 * as the Cg User Manual.
 * 
 * @author Markus Langenberg
 * 
 */
public interface GLFragmentShaderFilter extends GLFilter {

    /**
     * Appends its piece of code to the existing one.
     * 
     * Therefore, a shader builder is passed to the function, so it can append
     * its own code.
     * 
     * <p>
     * In most cases, this call is redirected to
     * {@link org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram#build(GLShaderBuilder)}.
     * 
     * <p>
     * The return value allows to switch the GLShaderBuilder during the process
     * of traversing through the view chain.
     * 
     * @param shaderBuilder
     *            ShaderBuilder to append customized code
     * @return GLShaderBuilder, which shall be used to continue. Usually, should
     *         be equal to the input parameter.
     */
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder);

}
