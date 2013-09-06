package org.helioviewer.jhv.internal_plugins.filter.opacity;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;

import org.helioviewer.viewmodel.filter.AbstractFilter;
import org.helioviewer.viewmodel.filter.GLFragmentShaderFilter;
import org.helioviewer.viewmodel.filter.StandardFilter;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.JavaBufferedImageData;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLTextureCoordinate;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

/**
 * Filter for changing the opacity of an image.
 * 
 * <p>
 * The output of the filter always is an ARGB image, since that is currently the
 * only format supporting an alpha channel. Thus, this filter should be applied
 * as late as possible.
 * 
 * <p>
 * This filter supports software rendering as well as rendering in OpenGL.
 * 
 * @author Markus Langenberg
 * 
 */
public class DynamicOpacityFilter extends AbstractFilter implements StandardFilter, GLFragmentShaderFilter {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private float opacity;
    private OpacityShader shader = new OpacityShader();

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    public DynamicOpacityFilter(float initialOpacity) {
        opacity = initialOpacity;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This filter is a major filter.
     */
    public boolean isMajorFilter() {
        return true;
    }

    /**
     * Sets the opacity.
     * 
     * This function does not the slider, thus should only be called by the
     * slider itself. Otherwise, use {@link #setOpacityExternal(float)}.
     * 
     * @param newOpacity
     *            New opacity, value has to be within [0, 1]
     */
    public void setOpacity(float newOpacity) {
        if (opacity == newOpacity) {
            return;
        }

        opacity = newOpacity;
        Thread t = new Thread(new Runnable() {
            public void run() {
                notifyAllListeners();
            }
        }, "NotifyFilterListenersThread");
        t.start();
    }

    /**
     * {@inheritDoc}
     */
    public ImageData apply(ImageData data) {
        if (data == null) {
            return null;
        }

        if (opacity > 0.999f)
            return data;

        if (data instanceof JavaBufferedImageData) {
            BufferedImage source = ((JavaBufferedImageData) data).getBufferedImage();
            BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = target.createGraphics();
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, data.getWidth(), data.getHeight());
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, opacity));
            g.drawImage(source, 0, 0, null);
            g.dispose();

            return new ARGBInt32ImageData(data, target);
        }
        return null;
    }

    /**
     * Fragment shader setting the opacity.
     */
    private class OpacityShader extends GLFragmentShaderProgram {
        private GLTextureCoordinate alphaParam;

        /**
         * Sets the new alpha value.
         * 
         * @param gl
         *            Valid reference to the current gl object
         * @param alpha
         *            Alpha value
         */
        private void setAlpha(GL gl, float alpha) {
            if (alphaParam != null) {
                alphaParam.setValue(gl, alpha);
            }
        }

        /**
         * {@inheritDoc}
         */
        protected void buildImpl(GLShaderBuilder shaderBuilder) {
            try {
                alphaParam = shaderBuilder.addTexCoordParameter(1);
                String program = "\toutput.a = output.a * alpha;";
                program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
                program = program.replace("alpha", alphaParam.getIdentifier());
                shaderBuilder.addMainFragment(program);
            } catch (GLBuildShaderException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
        shader.build(shaderBuilder);
        return shaderBuilder;
    }

    /**
     * {@inheritDoc}
     */
    public void applyGL(GL gl) {
        shader.bind(gl);
        shader.setAlpha(gl, opacity);
    }

    /**
     * {@inheritDoc}
     */
    public void forceRefilter() {

    }

    /**
     * {@inheritDoc}
     */
    public void setState(String state) {
        setOpacity(Float.parseFloat(state));
        // panel.setValue(opacity);
    }

    /**
     * {@inheritDoc}
     */
    public String getState() {
        return Float.toString(opacity);
    }
}
