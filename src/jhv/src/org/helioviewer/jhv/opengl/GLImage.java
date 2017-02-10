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

    public enum DifferenceMode {
        None, Running, RunningRotation, Base, BaseRotation
    }

    private GLTexture tex;
    private GLTexture lutTex;
    private GLTexture diffTex;

    private float brightOffset = 0;
    private float brightScale = 1;
    private float opacity = 1;
    private float sharpen = 0;
    private boolean enhanced = false;
    private ColorMask colorMask = new ColorMask(true, true, true);
    private DifferenceMode diffMode = DifferenceMode.None;

    private LUT lut = gray;
    private LUT lastLut;

    private boolean invertLUT = false;
    private boolean lastInverted = false;
    private boolean lutChanged = true;

    private static final LUT gray = LUT.get("Gray");

    public void streamImage(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData) {
        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
        if (!imageData.getUploaded()) {
            imageData.setUploaded(true);
            tex.copyImageData2D(gl, imageData);
        }

        ImageData prevFrame = isBaseDiff() ? baseImageData : prevImageData;
        if (diffMode != DifferenceMode.None && prevFrame != null) {
            diffTex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE2);
            diffTex.copyImageData2D(gl, prevFrame);
        }
    }

    public void applyFilters(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData, GLSLSolarShader shader) {
        applyRegion(imageData, prevImageData, baseImageData, shader);

        shader.colorMask = colorMask;
        shader.setBrightness(brightOffset, (float) (brightScale * imageData.getMetaData().getResponseFactor()));
        shader.setAlpha(opacity);
        shader.setEnhanced(gl, enhanced);
        shader.setIsDifference(diffMode.ordinal());

        int w = imageData.getWidth();
        int h = imageData.getHeight();
        shader.setFactors(sharpen, 1f / w, 1f / h, 1f);

        applyLUT(gl);
        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
    }

    private boolean isBaseDiff() {
        return diffMode == DifferenceMode.Base || diffMode == DifferenceMode.BaseRotation;
    }

    private boolean isRunningDiff() {
        return diffMode == DifferenceMode.Running || diffMode == DifferenceMode.RunningRotation;
    }

    private void applyRegion(ImageData imageData, ImageData prevImageData, ImageData baseImageData, GLSLSolarShader shader) {
        Region r = imageData.getRegion();
        shader.changeRect(r.llx, r.lly, 1. / r.width, 1. / r.height);

        Region diffRegion = null;
        if (prevImageData != null && !isBaseDiff()) {
            diffRegion = prevImageData.getRegion();
        } else if (baseImageData != null && isBaseDiff()) {
            diffRegion = baseImageData.getRegion();
        }

        if (diffRegion != null) {
            shader.setDifferenceRect(diffRegion.llx, diffRegion.lly, 1. / diffRegion.width, 1. / diffRegion.height);
        }

        MetaData metadata = imageData.getMetaData();
        shader.setCutOffRadius(metadata.getInnerCutOffRadius(), metadata.getOuterCutOffRadius());
        if (!Displayer.getShowCorona())
            shader.setOuterCutOffRadius(1);

        if (metadata.getCutOffValue() > 0) {
            Vec3 cdir = metadata.getCutOffDirection();
            shader.setCutOffDirection((float) cdir.x, (float) cdir.y, 0);
            shader.setCutOffValue((float) metadata.getCutOffValue());
        } else {
            shader.setCutOffValue(-1);
        }
    }

    private void applyLUT(GL2 gl) {
        lutTex.bind(gl, GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE1);

        LUT currlut = diffMode == DifferenceMode.None ? lut : gray;
        if (lutChanged || lastLut != currlut || invertLUT != lastInverted) {
            int[] intLUT = invertLUT ? currlut.getLut8Inv() : currlut.getLut8();
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

    public void setBrightness(float offset, float scale) {
        brightOffset = offset;
        brightScale = scale;
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

    public void setEnhanced(boolean _enhanced) {
        enhanced = _enhanced;
    }

    public void setDifferenceMode(DifferenceMode mode) {
        diffMode = mode;
    }

    public DifferenceMode getDifferenceMode() {
        return diffMode;
    }

}
