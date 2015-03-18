package org.helioviewer.viewmodel.renderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Hashtable;

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

    private final static GLCommonRenderGraphics instance = new GLCommonRenderGraphics();
    private GLCommonRenderGraphics() {}

    public static GLCommonRenderGraphics getSingletonInstance() {
        return instance;
    }

    private static Hashtable<BufferedImage, GLTextureHelper.GLTexture> mapImageToTexture = new Hashtable<BufferedImage, GLTextureHelper.GLTexture>();
    private static Hashtable<StringFontPair, GLTextureHelper.GLTexture> mapStringToTexture = new Hashtable<StringFontPair, GLTextureHelper.GLTexture>();

    private static BufferedImage stringSizeImage = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
    private static Graphics2D stringSizeGraphics = stringSizeImage.createGraphics();

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
    public void bindImage(GL2 gl, BufferedImage image) {
        GLTextureHelper.GLTexture tex = mapImageToTexture.get(image);
        if (tex == null) {
            if (mapImageToTexture.size() > 256) {
                mapImageToTexture.clear();
            }

            tex = new GLTextureHelper.GLTexture();
            GLTextureHelper.moveBufferedImageToGLTexture(gl, image, tex);
            mapImageToTexture.put(image, tex);
        }

        gl.glBindTexture(GL2.GL_TEXTURE_2D, tex.get(gl));
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
    public void bindString(GL2 gl, String string, Font font) {
        StringFontPair stringFontPair = new StringFontPair(string, font);

        GLTextureHelper.GLTexture tex = mapStringToTexture.get(stringFontPair);
        if (tex == null) {
            if (mapStringToTexture.size() > 256) {
                mapStringToTexture.clear();
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

            tex = new GLTextureHelper.GLTexture();
            GLTextureHelper.moveBufferedImageToGLTexture(gl, image, tex);
            mapStringToTexture.put(stringFontPair, tex);
        }

        gl.glBindTexture(GL2.GL_TEXTURE_2D, tex.get(gl));
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
                if (font == null) {
                    return string.equals(other.string) && other.font == null;
                }
                return string.equals(other.string) && font.equals(other.font);
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (font == null) {
                return string.hashCode();
            }
            return 2 * string.hashCode() + font.hashCode();
        }
    }

}
