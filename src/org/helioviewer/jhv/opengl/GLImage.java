package org.helioviewer.jhv.opengl;

import java.nio.IntBuffer;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec3;
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

    private double brightOffset = 0;
    private double brightScale = 1;
    private double opacity = 1;
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

    public void applyFilters(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData, GLSLSolarShader shader, int numLayers) {
        applyRegion(gl, imageData, prevImageData, baseImageData, shader);

        shader.bindBrightness(gl, brightOffset, brightScale * imageData.getMetaData().getResponseFactor(), imageData.getGamma());
        shader.bindColor(gl, red, green, blue, opacity, numLayers);
        shader.bindEnhanced(gl, enhanced);
        shader.bindSharpen(gl, sharpen, 1. / imageData.getWidth(), 1. / imageData.getHeight(), 1);

        applyLUT(gl);
        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
    }

    private boolean isBaseDiff() {
        return diffMode == DifferenceMode.Base;
    }

    private void applyRegion(GL2 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData, GLSLSolarShader shader) {
        Region r = imageData.getRegion();
        shader.bindRect(gl, r.llx, r.lly, 1. / r.width, 1. / r.height);

        shader.bindIsDiff(gl, diffMode.ordinal());
        if (diffMode != DifferenceMode.None)
            diffTex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE2);

        Region diffRegion = null;
        if (prevImageData != null && !isBaseDiff()) {
            diffRegion = prevImageData.getRegion();
            shader.bindAnglesDiff(gl, prevImageData.getMetaData().getViewpointL(), prevImageData.getMetaData().getCROTA());
        } else if (baseImageData != null && isBaseDiff()) {
            diffRegion = baseImageData.getRegion();
            shader.bindAnglesDiff(gl, baseImageData.getMetaData().getViewpointL(), baseImageData.getMetaData().getCROTA());
        }
        if (diffRegion != null)
            shader.bindDiffRect(gl, diffRegion.llx, diffRegion.lly, 1. / diffRegion.width, 1. / diffRegion.height);

        MetaData metaData = imageData.getMetaData();
        shader.bindAngles(gl, metaData.getViewpointL(), metaData.getCROTA());
        shader.bindCutOffRadius(gl, metaData.getInnerCutOffRadius(), Displayer.getShowCorona() ? metaData.getOuterCutOffRadius() : 1);
        if (metaData.getCutOffValue() > 0) {
            Vec3 cdir = metaData.getCutOffDirection();
            shader.bindCutOffDirection(gl, cdir.x, cdir.y, 0);
            shader.bindCutOffValue(gl, metaData.getCutOffValue());
        } else
            shader.bindCutOffValue(gl, -1);
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

    public void setBrightness(double offset, double scale) {
        brightOffset = MathUtils.clip(offset, -1, 2);
        brightScale = MathUtils.clip(scale, 0, 2 - brightOffset);
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
