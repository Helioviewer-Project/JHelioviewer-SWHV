package org.helioviewer.jhv.opengl;

import java.nio.IntBuffer;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.metadata.MetaData;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class GLImage {

    public enum DifferenceMode {
        None, Running, Base
    }

    private GLTexture tex;
    private GLTexture lutTex;
    private GLTexture diffTex;

    private float red = 1;
    private float green = 1;
    private float blue = 1;

    private double slitLeft = 0;
    private double slitRight = 1;
    private double brightOffset = 0;
    private double brightScale = 1;
    private double opacity = 1;
    private double blend = .5;
    private double sharpen = 0;
    private boolean enhanced = false;
    private DifferenceMode diffMode = DifferenceMode.None;

    private LUT lut = gray;
    private LUT lastLut;

    private boolean invertLUT = false;
    private boolean lastInverted = false;
    private boolean lutChanged = true;

    private static final LUT gray = LUT.get("Gray");

    public void streamImage(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData) {
        if (!imageData.getUploaded()) {
            imageData.setUploaded(true);
            tex.bind(gl);
            tex.copyImageBuffer(gl, imageData.getImageBuffer());
        }

        ImageData prevFrame = isBaseDiff() ? baseImageData : prevImageData;
        if (diffMode != DifferenceMode.None && prevFrame != null) {
            diffTex.bind(gl);
            diffTex.copyImageBuffer(gl, prevFrame.getImageBuffer());
        }
    }

    public void applyFilters(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData, GLSLSolarShader shader) {
        applyRegion(gl, imageData, prevImageData, baseImageData, shader);

        shader.bindSlit(gl, slitLeft, slitRight);
        shader.bindBrightness(gl, brightOffset, brightScale * imageData.getMetaData().getResponseFactor(), 1);
        shader.bindColor(gl, red, green, blue, opacity, blend);
        shader.bindEnhanced(gl, enhanced);
        shader.bindSharpen(gl, sharpen, 1. / imageData.getImageBuffer().width, 1. / imageData.getImageBuffer().height);

        applyLUT(gl);
        tex.bind(gl);
        shader.bindIsDiff(gl, diffMode.ordinal());
        if (diffMode != DifferenceMode.None)
            diffTex.bind(gl);
    }

    private boolean isBaseDiff() {
        return diffMode == DifferenceMode.Base;
    }

    private static MetaData bindParams(GL2 gl, ImageData imageData, GLSLSolarShader shader) {
        Region r = imageData.getRegion();
        shader.bindRect(gl, r.llx, r.lly, 1. / r.width, 1. / r.height);
        MetaData metaData = imageData.getMetaData();
        shader.bindAngles(gl, (float) metaData.getViewpoint().lat, metaData.getCROTA(), metaData.getSCROTA(), metaData.getCCROTA());
        shader.bindAnglesGrid(gl, metaData.getViewpoint());
        return metaData;
    }

    private static void bindParamsDiff(GL2 gl, ImageData imageData, GLSLSolarShader shader) {
        Region r = imageData.getRegion();
        shader.bindDiffRect(gl, r.llx, r.lly, 1. / r.width, 1. / r.height);
        MetaData metaData = imageData.getMetaData();
        shader.bindAnglesDiff(gl, (float) metaData.getViewpoint().lat, metaData.getCROTA(), metaData.getSCROTA(), metaData.getCCROTA());
        shader.bindAnglesGridDiff(gl, metaData.getViewpoint());
    }

    private void applyRegion(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData, GLSLSolarShader shader) {
        if (prevImageData != null && !isBaseDiff()) {
            bindParamsDiff(gl, prevImageData, shader);
        } else if (baseImageData != null && isBaseDiff()) {
            bindParamsDiff(gl, baseImageData, shader);
        }

        MetaData metaData = bindParams(gl, imageData, shader);
        shader.bindCalculateDepth(gl, metaData.getCalculateDepth());
        shader.bindRadii(gl, metaData.getInnerRadius(), Display.getShowCorona() ? metaData.getOuterRadius() : 1);
        shader.bindSector(gl, metaData.getSector0(), metaData.getSector1());
        if (metaData.getCutOffValue() > 0) {
            shader.bindCutOffDirection(gl, metaData.getCutOffX(), metaData.getCutOffY());
            shader.bindCutOffValue(gl, metaData.getCutOffValue());
        } else
            shader.bindCutOffValue(gl, -1);
    }

    private void applyLUT(GL2 gl) {
        lutTex.bind(gl);

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
        tex = new GLTexture(gl, GL2.GL_TEXTURE_2D, GLTexture.Unit.ZERO);
        lutTex = new GLTexture(gl, GL2.GL_TEXTURE_1D, GLTexture.Unit.ONE);
        diffTex = new GLTexture(gl, GL2.GL_TEXTURE_2D, GLTexture.Unit.TWO);

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

    public void setSlit(double left, double right) {
        slitLeft = MathUtils.clip(left, 0, 1);
        slitRight = MathUtils.clip(right, slitLeft, 1);
    }

    public void setBrightness(double offset, double scale) {
        brightOffset = MathUtils.clip(offset, -1, 2);
        brightScale = MathUtils.clip(scale, 0, 2 - brightOffset);
    }

    public double getSlitLeft() {
        return slitLeft;
    }

    public double getSlitRight() {
        return slitRight;
    }

    public double getBrightOffset() {
        return brightOffset;
    }

    public double getBrightScale() {
        return brightScale;
    }

    public void setColor(float _red, float _green, float _blue) {
        red = _red;
        green = _green;
        blue = _blue;
    }

    public void setOpacity(double _opacity) {
        opacity = MathUtils.clip(_opacity, 0, 1);
    }

    public void setBlend(double _blend) {
        blend = MathUtils.clip(_blend, 0, 1);
    }

    public void setSharpen(double _sharpen) {
        sharpen = MathUtils.clip(_sharpen, -1, 1);
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

    public double getSharpen() {
        return sharpen;
    }

    public boolean getEnhanced() {
        return enhanced;
    }

    public double getOpacity() {
        return opacity;
    }

    public double getBlend() {
        return blend;
    }

    public boolean getRed() {
        return red != 0;
    }

    public boolean getGreen() {
        return green != 0;
    }

    public boolean getBlue() {
        return blue != 0;
    }

    public boolean getInvertLUT() {
        return invertLUT;
    }

    public void fromJson(JSONObject jo) {
        setSharpen(jo.optDouble("sharpen", sharpen));
        setOpacity(jo.optDouble("opacity", opacity));
        setBlend(jo.optDouble("blend", blend));
        setSlit(jo.optDouble("slitLeft", slitLeft), jo.optDouble("slitRight", slitRight));
        setBrightness(jo.optDouble("brightOffset", brightOffset), jo.optDouble("brightScale", brightScale));
        enhanced = jo.optBoolean("enhanced", false);
        String strDiffMode = jo.optString("differenceMode", diffMode.toString());
        try {
            diffMode = DifferenceMode.valueOf(strDiffMode);
        } catch (Exception ignore) {
        }
        JSONObject colorObject = jo.optJSONObject("color");
        if (colorObject != null) {
            red = colorObject.optBoolean("red", true) ? 1 : 0;
            green = colorObject.optBoolean("green", true) ? 1 : 0;
            blue = colorObject.optBoolean("blue", true) ? 1 : 0;
        }
        invertLUT = jo.optBoolean("invert", false);
    }

    public JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("sharpen", sharpen);
        jo.put("opacity", opacity);
        jo.put("blend", blend);
        jo.put("slitLeft", slitLeft);
        jo.put("slitRight", slitRight);
        jo.put("brightOffset", brightOffset);
        jo.put("brightScale", brightScale);
        jo.put("enhanced", enhanced);
        jo.put("differenceMode", diffMode);

        JSONObject colorObject = new JSONObject();
        colorObject.put("red", getRed());
        colorObject.put("green", getGreen());
        colorObject.put("blue", getBlue());
        jo.put("color", colorObject);
        jo.put("invert", invertLUT);

        return jo;
    }

}
