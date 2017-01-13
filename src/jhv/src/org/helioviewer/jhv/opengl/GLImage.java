package org.helioviewer.jhv.opengl;

import java.nio.IntBuffer;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.viewmodel.imagedata.ColorMask;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

import com.jogamp.opengl.GL2;

public class GLImage {

    private GLTexture tex;
    private GLTexture lutTex;
    private GLTexture diffTex;

    private float brightness = 1f;
    private float contrast = 1f;
    private float opacity = 1f;
    private float sharpen = 0f;
    private ColorMask colorMask = new ColorMask(true, true, true);

    private LUT lut = gray;
    private LUT lastLut;

    private boolean invertLUT = false;
    private boolean lastInverted = false;

    private boolean lutChanged = true;

    private static final LUT gray = LUT.get("Gray");

    private boolean differenceMode = false;
    private boolean baseDifferenceMode = false;
    private boolean baseDifferenceNoRot = false;
    private boolean runningDifferenceNoRot = false;
    private float truncation = 1f - 0.8f;
    private boolean enhanced = false;

    public void streamImage(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData) {
        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
        if (!imageData.getUploaded()) {
            imageData.setUploaded(true);
            tex.copyImageData2D(gl, imageData);
        }

        ImageData prevFrame = baseDifferenceMode ? baseImageData : prevImageData;
        if (differenceMode && prevFrame != null) {
            diffTex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE2);
            diffTex.copyImageData2D(gl, prevFrame);
        }
    }

    public void applyFilters(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData, GLSLSolarShader shader) {
        applyRegion(imageData, prevImageData, baseImageData, shader);
        applyRunningDifference(gl, shader);

        shader.colorMask = colorMask;
        shader.setBrightness(brightness * imageData.getAutoBrightness() * imageData.getMetaData().getBrightnessFactor());
        shader.setContrast(contrast);
        shader.setAlpha(opacity);
        shader.setEnhanced(gl, enhanced);

        int w = imageData.getWidth();
        int h = imageData.getHeight();
        shader.setFactors(sharpen, 1f / w, 1f / h, 1f);

        applyLUT(gl);
        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
    }

    private void applyRegion(ImageData imageData, ImageData prevImageData, ImageData baseImageData, GLSLSolarShader shader) {
        Region r = imageData.getRegion();
        shader.changeRect(r.llx, r.lly, 1. / r.width, 1. / r.height);

        Region diffRegion = null;
        if (!baseDifferenceMode && prevImageData != null) {
            diffRegion = prevImageData.getRegion();
        } else if (baseDifferenceMode && baseImageData != null) {
            diffRegion = baseImageData.getRegion();
        }

        if (diffRegion != null) {
            shader.setDifferenceRect(diffRegion.llx, diffRegion.lly, 1. / diffRegion.width, 1. / diffRegion.height);
        }

        MetaData metadata = imageData.getMetaData();
        shader.setCutOffRadius(metadata.getInnerCutOffRadius(), metadata.getOuterCutOffRadius());
        if (!Displayer.getShowCorona())
            shader.setOuterCutOffRadius(1.);

        if (metadata.getCutOffValue() > 0) {
            Vec3 cdir = metadata.getCutOffDirection();
            shader.setCutOffDirection((float) cdir.x, (float) cdir.y, 0);
            shader.setCutOffValue((float) metadata.getCutOffValue());
        } else {
            shader.setCutOffValue(-1);
        }
    }

    private void applyRunningDifference(GL2 gl, GLSLSolarShader shader) {
        if (baseDifferenceMode || differenceMode) {
            if (baseDifferenceMode) {
                if (baseDifferenceNoRot) {
                    shader.setIsDifference(GLSLSolarShader.BASEDIFFERENCE_NO_ROT);
                } else {
                    shader.setIsDifference(GLSLSolarShader.BASEDIFFERENCE_ROT);
                }
            } else {
                if (runningDifferenceNoRot) {
                    shader.setIsDifference(GLSLSolarShader.RUNNINGDIFFERENCE_NO_ROT);
                } else {
                    shader.setIsDifference(GLSLSolarShader.RUNNINGDIFFERENCE_ROT);
                }
            }

            shader.setTruncationValue(truncation);
            diffTex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE2);
        } else {
            shader.setIsDifference(GLSLSolarShader.NODIFFERENCE);
        }
    }

    private void applyLUT(GL2 gl) {
        lutTex.bind(gl, GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE1);

        LUT currlut = differenceMode || baseDifferenceMode ? gray : lut;
        if (lutChanged || lastLut != currlut || invertLUT != lastInverted) {
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

            IntBuffer lutBuffer = IntBuffer.wrap(intLUT);
            lastLut = currlut;
            lastInverted = invertLUT;

            GLTexture.copyBuffer1D(gl, lutBuffer);
        }
        lutChanged = false;
    }

    public void init(GL2 gl) {
        tex = new GLTexture(gl);
        lutTex = new GLTexture(gl);
        diffTex = new GLTexture(gl);

        lutChanged = true;
    }

    public void dispose(GL2 gl) {
        if (tex != null)
            tex.delete(gl);
        if (lutTex != null)
            lutTex.delete(gl);
        if (diffTex != null)
            diffTex.delete(gl);
    }

    public void setBrightness(float _brightness) {
        brightness = _brightness;
    }

    public void setContrast(float _contrast) {
        contrast = _contrast;
    }

    public void setOpacity(float _opacity) {
        opacity = _opacity;
    }

    public void setSharpen(float _sharpen) {
        sharpen = _sharpen;
    }

    public void setColorMask(boolean redColormask, boolean greenColormask, boolean blueColormask) {
        colorMask = new ColorMask(redColormask, greenColormask, blueColormask);
    }

    public void setLUT(LUT newLUT, boolean invert) {
        if (lut == newLUT && invertLUT == invert) {
            return;
        }
        if (newLUT == null)
            newLUT = gray;

        lut = newLUT;
        invertLUT = invert;
        lutChanged = true;
    }

    public void setDifferenceMode(boolean _differenceMode) {
        differenceMode = _differenceMode;
    }

    public void setBaseDifferenceMode(boolean selected) {
        baseDifferenceMode = selected;
    }

    public void setBaseDifferenceNoRot(boolean _baseDifferenceNoRot) {
        baseDifferenceNoRot = _baseDifferenceNoRot;
    }

    public void setRunDiffNoRot(boolean _runningDifferenceNoRot) {
        runningDifferenceNoRot = _runningDifferenceNoRot;
    }

    public void setTruncation(float _truncation) {
        truncation = _truncation;
    }

    public void setEnhanced(boolean _enhanced) {
        enhanced = _enhanced;
    }

    public boolean getDifferenceMode() {
        return differenceMode;
    }

    public boolean getBaseDifferenceMode() {
        return baseDifferenceMode;
    }

}
