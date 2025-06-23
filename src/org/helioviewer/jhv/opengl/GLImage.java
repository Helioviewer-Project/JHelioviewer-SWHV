package org.helioviewer.jhv.opengl;

import java.nio.IntBuffer;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.metadata.MetaData;
import org.json.JSONObject;

import com.jogamp.opengl.GL3;

public class GLImage {

    public enum DifferenceMode {
        None, Running, Base
    }

    public static final int MIN_DCROTA = -15;
    public static final int MAX_DCROTA = 15;
    public static final int MIN_DCRVAL = -180;
    public static final int MAX_DCRVAL = 180;
    public static final int MAX_INNER = 5;

    private GLTexture tex;
    private GLTexture lutTex;
    private GLTexture diffTex;

    private float red = 1;
    private float green = 1;
    private float blue = 1;

    private double deltaCROTA = 0;
    private int deltaCRVAL1 = 0;
    private int deltaCRVAL2 = 0;

    private double innerMask = 0;
    private double slitLeft = 0;
    private double slitRight = 1;
    // private double sector0 = -Math.PI;
    // private double sector1 = Math.PI;
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

    public void streamImage(GL3 gl, ImageData imageData, ImageData prevImageData, ImageData baseImageData) {
        if (!imageData.getUploaded()) {
            imageData.setUploaded(true);
            tex.bind(gl);
            tex.copyImageBuffer(gl, imageData.getImageBuffer());
        }

        ImageData prevFrame = diffMode == DifferenceMode.Base ? baseImageData : prevImageData;
        if (diffMode != DifferenceMode.None && prevFrame != null) {
            diffTex.bind(gl);
            diffTex.copyImageBuffer(gl, prevFrame.getImageBuffer());
        }
    }

    private final float[] color = new float[4];

    public void applyFilters(GL3 gl, MetaData metaData, ImageData imageData) {
        // shader.bindSector(gl, -Math.max(Math.abs(metaData.getSector0()), Math.abs(sector0)), Math.max(metaData.getSector1(), sector1));

        color[0] = (float) (red * opacity); // https://amindforeverprogramming.blogspot.com/2013/07/why-alpha-premultiplied-colour-blending.html
        color[1] = (float) (green * opacity);
        color[2] = (float) (blue * opacity);
        color[3] = (float) (opacity * blend);
        GLSLSolarShader.bindDisplay(gl, color,
                1f / imageData.getImageBuffer().width, 1f / imageData.getImageBuffer().height, (float) (-2 * sharpen), diffMode.ordinal(),
                metaData.getSector0(), metaData.getSector1(), enhanced ? 1 : 0,
                metaData.getCutOffX(), metaData.getCutOffY(), metaData.getCutOffValue(), metaData.getCalculateDepth() ? 1 : 0,
                (float) brightOffset, (float) (brightScale * metaData.getResponseFactor()),
                Math.max(metaData.getInnerRadius(), (float) innerMask), Display.getShowCorona() ? metaData.getOuterRadius() : 1,
                (float) slitLeft, (float) slitRight);

        applyLUT(gl);
        tex.bind(gl);
        if (diffMode != DifferenceMode.None)
            diffTex.bind(gl);
    }

    private void applyLUT(GL3 gl) {
        lutTex.bind(gl);

        LUT currlut = diffMode == DifferenceMode.None ? lut : gray;
        if (lutChanged || lastLut != currlut || invertLUT != lastInverted) {
            int[] intLUT = invertLUT ? currlut.lut8Inv() : currlut.lut8();
            IntBuffer lutBuffer = IntBuffer.wrap(intLUT);
            lastLut = currlut;
            lastInverted = invertLUT;

            GLTexture.copyBuffer1D(gl, lutBuffer);
        }
        lutChanged = false;
    }

    public void init(GL3 gl) {
        tex = new GLTexture(gl, GL3.GL_TEXTURE_2D, GLTexture.Unit.ZERO);
        lutTex = new GLTexture(gl, GL3.GL_TEXTURE_1D, GLTexture.Unit.ONE);
        diffTex = new GLTexture(gl, GL3.GL_TEXTURE_2D, GLTexture.Unit.TWO);

        lutChanged = true;
    }

    public void dispose(GL3 gl) {
        if (tex != null)
            tex.delete(gl);
        if (lutTex != null)
            lutTex.delete(gl);
        if (diffTex != null)
            diffTex.delete(gl);
    }

    public void setDeltaCROTA(double delta) {
        deltaCROTA = MathUtils.clip(delta, MIN_DCROTA, MAX_DCROTA);
    }

    public void setDeltaCRVAL1(int delta) {
        deltaCRVAL1 = MathUtils.clip(delta, MIN_DCRVAL, MAX_DCRVAL);
    }

    public void setDeltaCRVAL2(int delta) {
        deltaCRVAL2 = MathUtils.clip(delta, MIN_DCRVAL, MAX_DCRVAL);
    }

    public void setInnerMask(double mask) {
        innerMask = MathUtils.clip(mask, 0, MAX_INNER);
    }

    public void setSlit(double left, double right) {
        slitLeft = MathUtils.clip(left, 0, 1);
        slitRight = MathUtils.clip(right, slitLeft, 1);
    }

    /*
        public void setSector(double left, double right) {
            sector0 = Math.toRadians(MathUtils.clip(left, -180, 0));
            sector1 = Math.toRadians(MathUtils.clip(right, 0, 180));
        }
    */
    public void setBrightness(double offset, double scale) {
        brightOffset = MathUtils.clip(offset, -1, 2);
        brightScale = MathUtils.clip(scale, 0, 2 - brightOffset);
    }

    public double getDeltaCROTA() {
        return deltaCROTA;
    }

    public int getDeltaCRVAL1() {
        return deltaCRVAL1;
    }

    public int getDeltaCRVAL2() {
        return deltaCRVAL2;
    }

    public double getInnerMask() {
        return innerMask;
    }

    public double getSlitLeft() {
        return slitLeft;
    }

    /*
        public double getSector0() {
            return Math.toDegrees(sector0);
        }

        public double getSector1() {
            return Math.toDegrees(sector1);
        }
    */
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
        // setSector(jo.optDouble("sector0", sector0), jo.optDouble("sector1", sector1));
        setInnerMask(jo.optDouble("innerMask", innerMask));
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
        // jo.put("sector0", getSector0());
        // jo.put("sector1", getSector1());
        jo.put("innerMask", innerMask);
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
