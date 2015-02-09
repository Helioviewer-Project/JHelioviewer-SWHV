package org.helioviewer.viewmodel.renderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.media.opengl.GL2;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;

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

    private final GL2 gl;
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
    public GLCommonRenderGraphics(GL2 _gl) {
        gl = _gl;
    }

    private int genTextureID(GL2 gl) {
        int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);

        return tmp[0];
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

            texID = genTextureID(gl);
            GLTextureHelper.moveBufferedImageToGLTexture(gl, image, texID.intValue());
            mapImageToTexture.put(image, texID);
        }

        gl.glBindTexture(GL2.GL_TEXTURE_2D, texID.intValue());
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

            texID = genTextureID(gl);
            GLTextureHelper.moveBufferedImageToGLTexture(gl, image, texID.intValue());
            mapStringToTexture.put(stringFontPair, texID);
        }

        gl.glBindTexture(GL2.GL_TEXTURE_2D, texID.intValue());
    }

    /**
     * Clears all saved image textures.
     * 
     * This might be useful to clean up after switching drawing modes.
     * 
     * @param gl
     *            valid gl object to use.
     */
    public static void clearImageTextureBuffer(GL2 gl) {
        for (Integer i : mapImageToTexture.values()) {
            gl.glDeleteTextures(1, new int[] { i }, 0);
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
    public static void clearStringTextureBuffer(GL2 gl) {
        for (Integer i : mapStringToTexture.values()) {
            gl.glDeleteTextures(1, new int[] { i }, 0);
        }
        mapStringToTexture.clear();
    }

    private class StringFontPair {
        public String string;
        public Font font;

        StringFontPair(String _string, Font _font) {
            string = _string;
            font = _font;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof StringFontPair) {
                StringFontPair other = (StringFontPair) o;
                if (font == null)
                    return string.equals(other.string) && other.font == null;

                return string.equals(other.string) && font.equals(other.font);
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (font == null)
                return string.hashCode();

            return 2 * string.hashCode() + font.hashCode();
        }
    }
}
