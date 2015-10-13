package org.helioviewer.jhv.opengl;

import java.nio.IntBuffer;

import org.helioviewer.base.Region;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.viewmodel.imagedata.ColorMask;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.MetaData;

import com.jogamp.opengl.GL2;

public class GLImage {

    private GLTexture tex;
    private GLTexture lutTex;
    private GLTexture diffTex;

    private float contrast = 0f;
    private float gamma = 1f;
    private float opacity = 1f;
    private float sharpen = 0f;
    private ColorMask colorMask = new ColorMask(true, true, true);

    private LUT lut = gray;
    private LUT lastLut;

    private boolean invertLUT = false;
    private boolean lastInverted = false;
    private IntBuffer lutBuffer;

    private boolean lutChanged = true;

    private static final LUT gray = LUT.getStandardList().get("Gray");

    private boolean differenceMode = false;
    private boolean baseDifferenceMode = false;
    private boolean baseDifferenceNoRot = false;
    private boolean runningDifferenceNoRot = false;
    private float truncation = 1f - 0.8f;

    public GLImage(LUT newLUT) {
        setLUT(newLUT, false);
    }

    public void applyFilters(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData) {
        copyScreenToTexture(gl, imageData, prevImageData, baseImageData);
        applyRunningDifferenceGL(gl, imageData, prevImageData, baseImageData);

        GLSLShader.colorMask = colorMask;
        GLSLShader.setContrast(contrast);
        GLSLShader.setGamma(gamma);
        GLSLShader.setAlpha(opacity);

        int w = imageData.getWidth();
        int h = imageData.getHeight();
        GLSLShader.setFactors(sharpen, 1f / w, 1f / h, 1f);
        applyGLLUT(gl);

        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);

        if (imageData.getUploaded() == false) {
            imageData.setUploaded(true);
            tex.copyImageData2D(gl, imageData);
        }
    }

    private void copyScreenToTexture(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData) {
        Region region = imageData.getRegion();

        double xOffset = region.getLowerLeftCorner().x;
        double yOffset = region.getLowerLeftCorner().y;
        double xScale = 1. / region.getWidth();
        double yScale = 1. / region.getHeight();

        GLSLShader.changeRect(xOffset, yOffset, xScale, yScale);

        boolean diffMode = false;
        Region diffRegion = null;

        if (!baseDifferenceMode && prevImageData != null) {
            diffMode = true;
            diffRegion = prevImageData.getRegion();
        } else if (baseDifferenceMode && baseImageData != null) {
            diffMode = true;
            diffRegion = baseImageData.getRegion();
        }

        if (diffMode) {
            double diffXOffset = diffRegion.getLowerLeftCorner().x;
            double diffYOffset = diffRegion.getLowerLeftCorner().y;
            double diffXScale = 1. / diffRegion.getWidth();
            double diffYScale = 1. / diffRegion.getHeight();

            GLSLShader.setDifferenceRect(diffXOffset, diffYOffset, diffXScale, diffYScale);
        }

        MetaData metadata = imageData.getMetaData();
        GLSLShader.setCutOffRadius(metadata.getInnerCutOffRadius(), metadata.getOuterCutOffRadius());
        if (metadata.getCutOffValue() > 0) {
            GL3DVec3d cdir = metadata.getCutOffDirection();
            GLSLShader.setCutOffDirection((float) cdir.x, (float) cdir.y, 0f);
            GLSLShader.setCutOffValue(metadata.getCutOffValue());
        } else {
            GLSLShader.setCutOffValue(-1f);
        }
    }

    private void applyRunningDifferenceGL(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData) {
        if (baseDifferenceMode || differenceMode) {
            if (baseDifferenceMode) {
                if (baseDifferenceNoRot) {
                    GLSLShader.setIsDifference(GLSLShader.BASEDIFFERENCE_NO_ROT);
                } else {
                    GLSLShader.setIsDifference(GLSLShader.BASEDIFFERENCE_ROT);
                }
            } else {
                if (runningDifferenceNoRot) {
                    GLSLShader.setIsDifference(GLSLShader.RUNNINGDIFFERENCE_NO_ROT);
                } else {
                    GLSLShader.setIsDifference(GLSLShader.RUNNINGDIFFERENCE_ROT);
                }
            }

            ImageData prevFrame = imageData;
            if (!baseDifferenceMode) {
                prevFrame = prevImageData;
            } else {
                prevFrame = baseImageData;
            }
            if (differenceMode && prevFrame != null) {
                GLSLShader.setTruncationValue(truncation);
                diffTex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE2);
                diffTex.copyImageData2D(gl, prevFrame);
            }
        } else {
            GLSLShader.setIsDifference(GLSLShader.NODIFFERENCE);
        }
    }

    private void applyGLLUT(GL2 gl) {
        LUT currlut;

        if (differenceMode || baseDifferenceMode) {
            currlut = gray;
        } else {
            currlut = lut;
        }

        lutTex.bind(gl, GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE1);

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

            lutBuffer = IntBuffer.wrap(intLUT);
            lastLut = currlut;
            lastInverted = invertLUT;

            lutTex.copyBuffer1D(gl, lutBuffer);
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
        tex.delete(gl);
        lutTex.delete(gl);
        diffTex.delete(gl);
    }

    public void setContrast(float contrast) {
        this.contrast = contrast;
    }

    public void setGamma(float gamma) {
        this.gamma = gamma;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public void setSharpen(float sharpen) {
        this.sharpen = sharpen;
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

    public void setDifferenceMode(boolean differenceMode) {
        this.differenceMode = differenceMode;
    }

    public boolean getDifferenceMode() {
        return this.differenceMode;
    }

    public void setBaseDifferenceMode(boolean selected) {
        this.baseDifferenceMode = selected;
    }

    public boolean getBaseDifferenceMode() {
        return baseDifferenceMode;
    }

    public void setBaseDifferenceNoRot(boolean baseDifferenceNoRot) {
        this.baseDifferenceNoRot = baseDifferenceNoRot;
    }

    public boolean getBaseDifferenceNoRot() {
        return baseDifferenceNoRot;
    }

    public void setRunDiffNoRot(boolean runningDifferenceNoRot) {
        this.runningDifferenceNoRot = runningDifferenceNoRot;
    }

    public boolean getRunDiffNoRot() {
        return runningDifferenceNoRot;
    }

    public void setTruncation(float truncation) {
        this.truncation = truncation;
    }

    public float getTruncation() {
        return truncation;
    }

    public float getOpacity() {
        return opacity;
    }

    public float getContrast() {
        return contrast;
    }

    public float getSharpen() {
        return sharpen;
    }

    public float getGamma() {
        return gamma;
    }

    public ColorMask getColorMask() {
        return colorMask;
    }

    public LUT getLUT() {
        return lut;
    }

    public boolean getInvertLUT() {
        return invertLUT;
    }

}
