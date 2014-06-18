package org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin;

import java.nio.IntBuffer;

import javax.media.opengl.GL;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.filter.AbstractFilter;
import org.helioviewer.viewmodel.filter.FrameFilter;
import org.helioviewer.viewmodel.filter.GLFragmentShaderFilter;
import org.helioviewer.viewmodel.filter.StandardFilter;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imageformat.SingleChannelImageFormat;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Short16ImageTransport;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLSingleChannelLookupFragmentShaderProgram;

/**
 * Filter for applying a color table to a single channel image.
 * 
 * <p>
 * If the input image is not a single channel image, the filter does nothing and
 * returns the input data.
 * 
 * <p>
 * This filter supports software rendering as well as rendering in OpenGL.
 * 
 * mostly rewritten
 * 
 * @author Helge Dietert
 */
public class SOHOLUTFilter extends AbstractFilter implements FrameFilter, StandardFilter, GLFragmentShaderFilter {
    // /////////////////////////
    // GENERAL //
    // /////////////////////////

    private SOHOLUTPanel panel;
    private IntBuffer buffer;

    /**
     * Used lut
     */
    private LUT lut;
    private boolean invertLUT = false;
    private boolean changed;

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This filter is a major filter.
     */
    @Override
    public boolean isMajorFilter() {
        return true;
    }

    /**
     * LUT is set to Gray as default table.
     */
    public SOHOLUTFilter() {
        lut = LUT.getStandardList().get("Gray");
    }

    /**
     * Constructor setting the color table.
     * 
     * @param startWithLut
     *            Color table to apply to the image
     */
    public SOHOLUTFilter(LUT startWithLut) {
        lut = startWithLut;
    }

    /**
     * Sets the corresponding SOHOLUT panel.
     * 
     * @param panel
     *            Corresponding panel.
     */
    void setPanel(SOHOLUTPanel panel) {
        this.panel = panel;
        panel.setValue(lut, invertLUT);
    }

    /**
     * Sets a new color table to use from now on.
     * 
     * @param newLUT
     *            New color table
     */
    void setLUT(LUT newLUT, boolean invert) {
        if (newLUT == null || (lut == newLUT && invertLUT == invert)) {
            return;
        }
        lut = newLUT;
        invertLUT = invert;
        this.changed = true;
        notifyAllListeners();
        Displayer.getSingletonInstance().display();
    }

    // /////////////////////////
    // STANDARD //
    // /////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageData apply(ImageData data) {
        // Ship over gray for performance as before
        if (data == null || !(data.getImageFormat() instanceof SingleChannelImageFormat) || (lut.getName() == "Gray" && !invertLUT)) {
            return data;
        }

        if (data.getImageTransport() instanceof Byte8ImageTransport) {
            byte[] pixelData = ((Byte8ImageTransport) data.getImageTransport()).getByte8PixelData();
            int[] resultPixelData = new int[pixelData.length];
            lut.lookup8(pixelData, resultPixelData, invertLUT);
            return new ARGBInt32ImageData(data, resultPixelData);
        } else if (data.getImageTransport() instanceof Short16ImageTransport) {
            short[] pixelData = ((Short16ImageTransport) data.getImageTransport()).getShort16PixelData();
            int[] resultPixelData = new int[pixelData.length];
            lut.lookup16(pixelData, resultPixelData, invertLUT);
            data = new ARGBInt32ImageData(data, resultPixelData);
            return data;
        }

        return null;
    }

    // /////////////////////////
    // OPENGL //
    // /////////////////////////
    private final GLSingleChannelLookupFragmentShaderProgram shader = new GLSingleChannelLookupFragmentShaderProgram();
    private int lookupTex = 0;
    private LUT lastLut = null;
    private boolean lastInverted = false;
    private JHVJP2View jp2View;

    /**
     * {@inheritDoc}
     */
    @Override
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
        shader.build(shaderBuilder);

        if (lastLut == null) {
            GLTextureHelper textureHelper = new GLTextureHelper();
            textureHelper.delTextureID(shaderBuilder.getGL(), lookupTex);
            // I think this may be wrong, but I just reused the openGL code
            lookupTex = textureHelper.genTextureID(shaderBuilder.getGL());
        }

        return shaderBuilder;
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, also updates the color table, if necessary.
     */
    @Override
    public void applyGL(GL gl) {
        shader.bind(gl);
        shader.activateLutTexture(gl);
        LUT currlut;
        // Note: The lookup table will always be power of two,
        // so we won't get any problems here.
        boolean b = (jp2View instanceof JHVJPXView);
        JHVJPXView jpxView = (JHVJPXView) jp2View;
        if (b && jpxView.getDifferenceMode()) {
            currlut = LUT.getStandardList().get("Gray");
        } else {
            currlut = lut;
        }

        gl.glBindTexture(GL.GL_TEXTURE_1D, lookupTex);
        if (lastLut != currlut || invertLUT != lastInverted) {
            int[] intLUT;

            if (invertLUT) {
                int[] sourceLUT = currlut.getLut8();
                intLUT = new int[sourceLUT.length];

                int offset = sourceLUT.length - 1;
                for (int i = 0; i < sourceLUT.length / 2; i++) {
                    intLUT[i] = sourceLUT[offset - i];
                    intLUT[offset - i] = sourceLUT[i];
                }
            } else {
                intLUT = currlut.getLut8();
            }

            buffer = IntBuffer.wrap(intLUT);
            lastLut = currlut;
            lastInverted = invertLUT;

            gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, 0);
            gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS, 0);
            gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, 0);
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 4);

            gl.glTexImage1D(GL.GL_TEXTURE_1D, 0, GL.GL_RGBA, buffer.limit(), 0, GL.GL_BGRA, GL.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
            gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
        }

    }

    @Override
    protected void finalize() {
        if (lookupTex != 0) {
            GLTextureHelper textureHelper = new GLTextureHelper();
            textureHelper.delTextureID(null, lookupTex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forceRefilter() {
        lastLut = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        String[] values = state.trim().split(" ");
        String tableString = values[0];
        String invertString = values[values.length - 1];
        for (int i = 1; i < values.length - 1; i++) {
            tableString += " " + values[i];
        }
        setLUT(LUT.getStandardList().get(tableString.replaceAll("ANGSTROM", Character.toString(LUT.angstrom))), Boolean.parseBoolean(invertString));
        panel.setValue(lut, invertLUT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return lut.getName().replaceAll(Character.toString(LUT.angstrom), "ANGSTROM") + " " + invertLUT;
    }

    @Override
    public void setJP2View(JHVJP2View jp2View) {
        this.jp2View = jp2View;
    }
}
