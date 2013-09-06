package org.helioviewer.viewmodel.view.opengl.shader;

import org.helioviewer.viewmodel.view.opengl.GLView;

/**
 * View to build fragment shaders.
 * 
 * <p>
 * To build the final fragment shader, many different code blocks are merged. To
 * ensure, that this happens, every view, which wants to participate in the
 * progress of building the shader has to implement this interface. While
 * building the shader, it is called and can add its own code. Apart from that,
 * it has to call the next GLFragmentShaderView as well, so that every view gets
 * the chance to append its piece of code as well.
 * 
 * @author Markus Langenberg
 */
public interface GLFragmentShaderView extends GLView {

    /**
     * Appends its piece of code to the existing one.
     * 
     * Therefore, a shader builder is passed to the function, so it can append
     * its own code. Apart from that, this function has to call the next
     * GLFragmentShaderView as well, if there is one, to ensure that all views
     * append their code to the final shader.
     * 
     * <p>
     * In most cases, this call is redirected to
     * {@link GLFragmentShaderProgram#build(GLShaderBuilder)}.
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
