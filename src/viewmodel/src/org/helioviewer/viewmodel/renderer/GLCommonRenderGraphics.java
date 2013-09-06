package org.helioviewer.viewmodel.renderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLScalePowerOfTwoVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLTextureCoordinate;

/**
 * Class containing OpenGL specific render functions equal for both renderer
 * types.
 * 
 * This class should only be used by org.helioviewer.viewmodel
 * .renderer.physical.GLPhysicalRenderGraphics and
 * org.helioviewer.viewmodel.renderer.screen.GLScreenRenderGraphics
 * 
 * @author Markus Langenberg
 */
public class GLCommonRenderGraphics {

    private static GLScalePowerOfTwoVertexShaderProgram scalingShader;
    private static GLTextureCoordinate texCoord = new GLTextureHelper.GLMainTextureCoordinate();

    private GL gl;
    private static GLTextureHelper textureHelper = new GLTextureHelper();
    private static HashMap<BufferedImage, Integer> mapImageToTexture = new HashMap<BufferedImage, Integer>();
    private static HashMap<StringFontPair, Integer> mapStringToTexture = new HashMap<StringFontPair, Integer>();

    private static BufferedImage stringSizeImage = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
    private static Graphics2D stringSizeGraphics = stringSizeImage.createGraphics();

    /**
     * Default constructor.
     * 
     * <p>
     * The caller has to provide a gl object, which can be used by this
     * renderer.
     * 
     * @param _gl
     *            gl object, that should be used for drawing.
     */
    public GLCommonRenderGraphics(GL _gl) {
        gl = _gl;

        if (!GLTextureHelper.textureNonPowerOfTwoAvailable() && scalingShader == null) {
            scalingShader = new GLScalePowerOfTwoVertexShaderProgram();
            scalingShader.buildStandAlone(_gl);
        }
    }

    /**
     * Sets the standard texture coordinate.
     * 
     * This function has to be called instead of the pure OpenGL-function to be
     * able to use
     * {@link org.helioviewer.viewmodel.view.opengl.shader.GLScalePowerOfTwoVertexShaderProgram}
     * .
     * 
     * @param x
     *            X-coordinate to set
     * @param y
     *            Y-coordinate to set
     */
    public void setTexCoord(float x, float y) {
        texCoord.setValue(gl, x, y);
    }

    /**
     * Returns the display size of the given string with the given font.
     * 
     * @return Display size of the given string with the given font
     */
    public Vector2dInt getStringDisplaySize(String string, Font font) {
        if (font != null)
            stringSizeGraphics.setFont(font);

        FontMetrics metrics = stringSizeGraphics.getFontMetrics();

        return new Vector2dInt(metrics.stringWidth(string), metrics.getHeight());
    }

    /**
     * Binds the texture corresponding to the given image.
     * 
     * If the image never has been used before, a new texture is created. The
     * texture will be saved after rendering to use it again later. If more than
     * 256 textures have been generated, the buffer is cleared.
     * 
     * @param image
     *            Image corresponding to the texture to bind
     */
    public void bindImage(BufferedImage image) {
        Integer texID = mapImageToTexture.get(image);
        if (texID == null) {
            if (mapImageToTexture.size() > 256) {
                clearImageTextureBuffer(gl);
            }

            texID = textureHelper.genTextureID(gl);
            textureHelper.moveBufferedImageToGLTexture(gl, image, texID.intValue());
            mapImageToTexture.put(image, texID);
        }

        textureHelper.bindTexture(gl, texID.intValue());
    }

    /**
     * Binds a texture containing the given string with the given font.
     * 
     * If the string never has been used before, a new texture is created. The
     * texture will be saved after rendering to use it again later. If more than
     * 256 textures have been generated, the buffer is cleared.
     * 
     * @param textLayout
     *            String and font corresponding to the texture to bind
     */
    public void bindString(String string, Font font) {
        StringFontPair stringFontPair = new StringFontPair(string, font);
        Integer texID = mapStringToTexture.get(stringFontPair);
        if (texID == null) {
            if (mapStringToTexture.size() > 256) {
                clearStringTextureBuffer(gl);
            }

            if (font != null)
                stringSizeGraphics.setFont(font);

            FontMetrics metrics = stringSizeGraphics.getFontMetrics();

            BufferedImage image = new BufferedImage(metrics.stringWidth(string), metrics.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.WHITE);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setFont(font);
            graphics.drawString(string, 0, metrics.getAscent());

            texID = textureHelper.genTextureID(gl);
            textureHelper.moveBufferedImageToGLTexture(gl, image, texID.intValue());
            mapStringToTexture.put(stringFontPair, texID);
        }

        textureHelper.bindTexture(gl, texID.intValue());
    }

    /**
     * Binds the scaling shader.
     * 
     * This should be done before using a texture. After using the texture,
     * {@link #unbindScalingShader()} should be called.
     * 
     * <p>
     * This function only has an effect, if non-power-of-two- textures are
     * disabled.
     */
    public void bindScalingShader() {
        if (GLTextureHelper.textureNonPowerOfTwoAvailable())
            return;

        gl.glEnable(GL.GL_VERTEX_PROGRAM_ARB);
        scalingShader.bind(gl);
    }

    /**
     * Unbinds the scaling shader.
     * 
     * This is the corresponding call to {@link #bindScalingShader()}.
     */
    public void unbindScalingShader() {
        gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);
    }

    /**
     * Clears all saved image textures.
     * 
     * This might be useful to clean up after switching drawing modes.
     * 
     * @param gl
     *            valid gl object to use.
     */
    public static void clearImageTextureBuffer(GL gl) {
        for (Integer i : mapImageToTexture.values()) {
            textureHelper.delTextureID(gl, i);
        }
        mapImageToTexture.clear();
    }

    /**
     * Clears all saved text textures.
     * 
     * This might be useful to clean up after switching drawing modes.
     * 
     * @param gl
     *            valid gl object to use.
     */
    public static void clearStringTextureBuffer(GL gl) {
        for (Integer i : mapStringToTexture.values()) {
            textureHelper.delTextureID(gl, i);
        }
        mapStringToTexture.clear();
    }

    /**
     * Clears the shader shared by this render graphics objects.
     */
    public static void clearShader() {
        scalingShader = null;
    }

    private class StringFontPair {
        public String string;
        public Font font;

        StringFontPair(String _string, Font _font) {
            string = _string;
            font = _font;
        }

        public boolean equals(Object o) {
            if (o instanceof StringFontPair) {
                StringFontPair other = (StringFontPair) o;
                if (font == null)
                    return string.equals(other.string) && other.font == null;

                return string.equals(other.string) && font.equals(other.font);
            }
            return false;
        }

        public int hashCode() {
            if (font == null)
                return string.hashCode();

            return 2 * string.hashCode() + font.hashCode();
        }
    }
}
