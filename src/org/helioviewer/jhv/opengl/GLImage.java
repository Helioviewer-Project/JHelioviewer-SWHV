package org.helioviewer.jhv.opengl;

import java.nio.IntBuffer;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class GLImage {

    public enum DifferenceMode {
        None, Running, RunningRotation, Base, BaseRotation
    }

    private GLTexture tex;
    private GLTexture lutTex;
    private GLTexture diffTex;

    private float red = 1;
    private float green = 1;
    private float blue = 1;

    private float brightOffset = 0;
    private float brightScale = 1;
    private float opacity = 1;
    private float sharpen = 0;
    private int enhanced = 0;
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

        shader.setBrightness(brightOffset, (float) (brightScale * imageData.getMetaData().getResponseFactor()), (float) imageData.getGamma());
        shader.setColor(red, green, blue, opacity);
        shader.setEnhanced(enhanced);

        shader.setIsDifference(diffMode.ordinal());
        if (diffMode != DifferenceMode.None)
            diffTex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE2);

        int w = imageData.getWidth();
        int h = imageData.getHeight();
        shader.setSharpen(sharpen, 1f / w, 1f / h, 1f);

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
        shader.setCROTA(metadata.getCROTA());
        shader.setCutOffRadius(metadata.getInnerCutOffRadius(), Displayer.getShowCorona() ? metadata.getOuterCutOffRadius() : 1);
        if (metadata.getCutOffValue() > 0) {
            Vec3 cdir = metadata.getCutOffDirection();
            shader.setCutOffDirection(cdir.x, cdir.y, 0);
            shader.setCutOffValue(metadata.getCutOffValue());
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

    public float getBrightOffset() {
        return brightOffset;
    }

    public float getBrightScale() {
        return brightScale;
    }

    public void setColor(float _red, float _green, float _blue) {
        red = _red;
        green = _green;
        blue = _blue;
    }

    public void setOpacity(float _opacity) {
        opacity = _opacity;
    }

    public void setSharpen(float _sharpen) {
        sharpen = _sharpen;
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

    public void setEnhanced(int _enhanced) {
        enhanced = _enhanced;
    }

    public void setDifferenceMode(DifferenceMode mode) {
        diffMode = mode;
    }

    public DifferenceMode getDifferenceMode() {
        return diffMode;
    }

    public float getSharpen() {
        return sharpen;
    }

    public int getEnhanced() {
        return enhanced;
    }

    public float getOpacity() {
        return opacity;
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
    public void fromJson(JSONObject json) {
        sharpen = (float) json.optDouble("sharpen", 0);
        opacity = (float) json.optDouble("opacity", 1);
        brightOffset = (float) json.optDouble("brightOffset", 0);
        brightScale = (float) json.optDouble("brightScale", 1);
        enhanced = json.optInt("enhanced", 1);
        String strdiffMode = json.optString("differenceMode", "None");
        try {
            diffMode = DifferenceMode.valueOf(strdiffMode);
        } catch (Exception e) {
            diffMode = DifferenceMode.None;
        }
        JSONObject colorObject = json.optJSONObject("color");
        if (colorObject != null) {
            red = (float) colorObject.optDouble("red", 1);
            green = (float) colorObject.optDouble("green", 1);
            blue = (float) colorObject.optDouble("blue", 1);
        }
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("sharpen", sharpen);
        json.put("opacity", opacity);
        json.put("brightOffset", brightOffset);
        json.put("brightScale", brightScale);
        json.put("enhanced", enhanced);
        json.put("differenceMode", diffMode.toString());
        JSONObject colorObject = new JSONObject();
        json.put("color", colorObject);
        colorObject.put("red", red);
        colorObject.put("green", green);
        colorObject.put("blue", blue);
        return json;
    }

}
