package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageDisplaySettings;
import org.helioviewer.jhv.image.ImageDisplaySettings.DifferenceMode;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.metadata.DetectorMask;
import org.helioviewer.jhv.metadata.MetaData;

public class GLSLImage {

    private final ImageDisplaySettings settings;

    private GLStreamingTexture2D tex;
    private GLStreamingTexture2D diffTex;
    private GLStreamingTexture2D maskTex;
    private GLTexture lutTex;

    private LUT lastLut;
    private boolean lastInverted;
    private DetectorMask uploadedMask = DetectorMask.NONE;
    private ImageBuffer uploadedImageBuffer;
    private ImageBuffer uploadedDiffBuffer;

    public GLSLImage(ImageDisplaySettings _settings) {
        settings = _settings;
    }

    public void streamImages(ImageBuffer imageBuffer, @Nullable ImageBuffer differenceBuffer) {
        if (uploadedImageBuffer != imageBuffer) {
            tex.upload(imageBuffer);
            uploadedImageBuffer = imageBuffer;
        }
        if (differenceBuffer != null && uploadedDiffBuffer != differenceBuffer) {
            diffTex.upload(differenceBuffer);
            uploadedDiffBuffer = differenceBuffer;
        }
    }

    private final float[] color = new float[4];

    public void applyFilters(ImageBuffer imageBuffer, MetaData metaData, boolean rhefActive) {
        color[0] = (float) (settings.getOpacity() * settings.getRedScale()); // premultiplied alpha
        color[1] = (float) (settings.getOpacity() * settings.getGreenScale());
        color[2] = (float) (settings.getOpacity() * settings.getBlueScale());
        color[3] = (float) (settings.getOpacity() * settings.getBlend());
        GLSLImageShader.bindDisplay(color,
                1f / imageBuffer.width, 1f / imageBuffer.height,
                (float) (-2 * settings.getSharpen()), settings.getDifferenceMode().ordinal(),
                // RHEF output is already a normalized rank in [0, 1]; the raw-DN response
                // factor must NOT rescale it (that pushes the uniform upper half past 1 and
                // clamps it to white). The user's Levels (brightOffset/brightScale) still
                // apply as a black/white-point control on the equalized output.
                (float) settings.getBrightOffset(), (float) (settings.getBrightScale() * (rhefActive ? 1 : metaData.getResponseFactor())),
                (float) (rhefActive ? settings.getUpsilonLow() : 1), (float) (rhefActive ? settings.getUpsilonHigh() : 1),
                settings.getSectorMaskCenter(), settings.getSectorMaskHalfWidth(), metaData.getSectorMaskCenter(), metaData.getSectorMaskHalfWidth(),
                metaData.getCutOffX(), metaData.getCutOffY(), metaData.getCutOffValue(), metaData.getCalculateDepth() ? 1 : 0,
                Math.max(metaData.getInnerRadius(), (float) settings.getInnerMask()),
                Math.min(Display.getShowCorona() ? metaData.getOuterRadius() : 1, (float) settings.getOuterMask()),
                (float) settings.getSlitLeft(), (float) settings.getSlitRight(),
                (float) settings.getEnhanced());

        applyLUT();
        applyMask(metaData.getDetectorMask());
        maskTex.bind();
        tex.bind();
        if (settings.getDifferenceMode() != DifferenceMode.None)
            diffTex.bind();
    }

    private void applyLUT() {
        lutTex.bind();
        LUT currlut = settings.getDifferenceMode() == DifferenceMode.None ? settings.getLUT() : LUT.gray();
        boolean inverted = settings.getInvertLUT();
        if (lastLut != currlut || inverted != lastInverted) {
            ByteBuffer lutBuffer = inverted ? currlut.rgbaInv() : currlut.rgba();
            lastLut = currlut;
            lastInverted = inverted;

            lutTex.upload2D(GLTexture.Format.RGBA8, lutBuffer.remaining() / 4, 1, GL.NEAREST, lutBuffer);
        }
    }

    public void init() {
        if (tex != null)
            return;
        try {
            tex = new GLStreamingTexture2D(GLTexture.Unit.ZERO, GL.LINEAR);
            lutTex = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.ONE);
            diffTex = new GLStreamingTexture2D(GLTexture.Unit.TWO, GL.LINEAR);
            maskTex = new GLStreamingTexture2D(GLTexture.Unit.THREE, GL.NEAREST);
            maskTex.upload(uploadedMask.getImageBuffer());
        } catch (RuntimeException | Error e) {
            dispose();
            throw e;
        }
    }

    public void dispose() {
        if (tex != null)
            tex.delete();
        if (lutTex != null)
            lutTex.delete();
        if (diffTex != null)
            diffTex.delete();
        if (maskTex != null)
            maskTex.delete();
        tex = null;
        lutTex = null;
        diffTex = null;
        maskTex = null;
        uploadedImageBuffer = null;
        uploadedDiffBuffer = null;
        uploadedMask = DetectorMask.NONE;
        lastLut = null;
    }

    private void applyMask(DetectorMask detectorMask) {
        if (uploadedMask == detectorMask)
            return;
        maskTex.upload(detectorMask.getImageBuffer());
        uploadedMask = detectorMask;
    }
}
