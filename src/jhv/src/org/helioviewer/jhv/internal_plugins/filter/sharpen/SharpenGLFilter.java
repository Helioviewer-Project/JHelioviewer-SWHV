package org.helioviewer.jhv.internal_plugins.filter.sharpen;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.filter.GLImageSizeFilter;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.ShaderFactory;

/**
 * Extension of SharpenFilter, also providing an OpenGL implementation.
 *
 * <p>
 * For further information about sharpening, see {@link SharpenFilter}.
 *
 * @author Markus Langenberg
 */
public class SharpenGLFilter extends SharpenFilter implements GLImageSizeFilter {

    private final UnsharpMaskingShader shader = new UnsharpMaskingShader();
    private float pixelWidth, pixelHeight;

    /**
     * Fragment shader performing the unsharp mask algorithm.
     */
    private class UnsharpMaskingShader extends GLFragmentShaderProgram {

        @Override
        public void bind(GL2 gl) {
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyGL(GL2 gl) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImageSize(int width, int height) {
        pixelWidth = 1.0f / width;
        pixelHeight = 1.0f / height;
        ShaderFactory.setFactors(weighting, pixelWidth, pixelHeight, span);
    }

}
