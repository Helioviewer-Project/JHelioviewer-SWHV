package org.helioviewer.jhv.image;

import org.helioviewer.jhv.image.lut.LUT;

import org.json.JSONObject;

public final class ImageDisplaySettings {

    public enum DifferenceMode {
        None, Running, Base
    }

    public static final int MIN_DCROTA = -15;
    public static final int MAX_DCROTA = 15;
    public static final int MIN_DCRVAL = -180;
    public static final int MAX_DCRVAL = 180;
    public static final int MAX_MASK = 32;

    private float red = 1;
    private float green = 1;
    private float blue = 1;

    private double deltaCROTA;
    private double deltaCRVAL1;
    private double deltaCRVAL2;

    private double innerMask;
    private double outerMask = Double.POSITIVE_INFINITY;
    private double slitLeft;
    private double slitRight = 1;
    private double sectorCenter;
    private double sectorWidth;
    private double brightOffset;
    private double brightScale = 1;
    private double opacity = 1;
    private double blend = .5;
    private double sharpen;
    private double enhanced;
    // RHEF two-sided midtone control (Upsilon), AIA 171 defaults (Gilly & DeForest 2024, §3.2,
    // https://arxiv.org/html/2511.02798v1). The curve below the median (shadows, upsilonLow) and
    // above it (highlights, upsilonHigh) are shaped independently, so the two handles are
    // asymmetric by design. sunkit-image's rhef exposes the same split light/dark upsilon:
    // https://docs.sunpy.org/projects/sunkit-image/en/stable/api/sunkit_image.radial.rhef.html
    private double upsilonLow = .6;
    private double upsilonHigh = .4;
    private DifferenceMode differenceMode = DifferenceMode.None;

    private LUT lut = LUT.gray();
    private boolean invertLUT;

    public void setDeltaCROTA(double delta) {
        deltaCROTA = Math.clamp(delta, MIN_DCROTA, MAX_DCROTA);
    }

    public void setDeltaCRVAL1(double delta) {
        deltaCRVAL1 = Math.clamp(delta, MIN_DCRVAL, MAX_DCRVAL);
    }

    public void setDeltaCRVAL2(double delta) {
        deltaCRVAL2 = Math.clamp(delta, MIN_DCRVAL, MAX_DCRVAL);
    }

    public void setMask(double inner, double outer) {
        innerMask = Math.clamp(inner, 0, MAX_MASK);
        outerMask = Double.isFinite(outer) ? Math.clamp(outer, innerMask, MAX_MASK) : Double.POSITIVE_INFINITY;
    }

    public void setSlit(double left, double right) {
        slitLeft = Math.clamp(left, 0, 1);
        slitRight = Math.clamp(right, slitLeft, 1);
    }

    public void setSector(double center, double width) {
        sectorCenter = Math.clamp(center, -180, 180);
        sectorWidth = Math.clamp(width, 0, 360);
    }

    public void setBrightness(double offset, double scale) {
        brightOffset = Math.clamp(offset, -1, 2);
        brightScale = Math.clamp(scale, 0, 2 - brightOffset);
    }

    public double getDeltaCROTA() {
        return deltaCROTA;
    }

    public double getDeltaCRVAL1() {
        return deltaCRVAL1;
    }

    public double getDeltaCRVAL2() {
        return deltaCRVAL2;
    }

    public double getInnerMask() {
        return innerMask;
    }

    public double getOuterMask() {
        return outerMask;
    }

    public double getSlitLeft() {
        return slitLeft;
    }

    public double getSlitRight() {
        return slitRight;
    }

    public double getSectorCenter() {
        return sectorCenter;
    }

    public double getSectorWidth() {
        return sectorWidth;
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
        opacity = Math.clamp(_opacity, 0, 1);
    }

    public void setBlend(double _blend) {
        blend = Math.clamp(_blend, 0, 1);
    }

    public void setSharpen(double _sharpen) {
        sharpen = Math.clamp(_sharpen, -1, 1);
    }

    public void setLUT(LUT newLUT, boolean invert) {
        lut = newLUT == null ? LUT.gray() : newLUT;
        invertLUT = invert;
    }

    public void setEnhanced(double _enhanced) {
        enhanced = Math.clamp(_enhanced, 0, 3);
    }

    public void setUpsilon(double low, double high) {
        upsilonLow = Math.clamp(low, 0.05, 1);
        upsilonHigh = Math.clamp(high, 0.05, 1);
    }

    public double getUpsilonLow() {
        return upsilonLow;
    }

    public double getUpsilonHigh() {
        return upsilonHigh;
    }

    public void setDifferenceMode(DifferenceMode _mode) {
        differenceMode = _mode;
    }

    public DifferenceMode getDifferenceMode() {
        return differenceMode;
    }

    public double getSharpen() {
        return sharpen;
    }

    public double getEnhanced() {
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

    public float getRedScale() {
        return red;
    }

    public float getGreenScale() {
        return green;
    }

    public float getBlueScale() {
        return blue;
    }

    public LUT getLUT() {
        return lut;
    }

    public boolean getInvertLUT() {
        return invertLUT;
    }

    public void fromJson(JSONObject jo) {
        setSharpen(jo.optDouble("sharpen", sharpen));
        setOpacity(jo.optDouble("opacity", opacity));
        setBlend(jo.optDouble("blend", blend));
        setSlit(jo.optDouble("slitLeft", slitLeft), jo.optDouble("slitRight", slitRight));
        setSector(jo.optDouble("sectorCenter", 0), jo.optDouble("sectorWidth", 0));
        setMask(jo.optDouble("innerMask", innerMask), jo.optDouble("outerMask", Double.POSITIVE_INFINITY));
        setBrightness(jo.optDouble("brightOffset", brightOffset), jo.optDouble("brightScale", brightScale));
        setEnhanced(jo.optDouble("enhanced", enhanced));
        setUpsilon(jo.optDouble("upsilonLow", upsilonLow), jo.optDouble("upsilonHigh", upsilonHigh));
        String mode = jo.optString("differenceMode", differenceMode.toString());
        try {
            differenceMode = DifferenceMode.valueOf(mode);
        } catch (Exception ignore) {}
        JSONObject colorObject = jo.optJSONObject("color");
        if (colorObject != null) {
            red = colorObject.optBoolean("red", getRed()) ? 1 : 0;
            green = colorObject.optBoolean("green", getGreen()) ? 1 : 0;
            blue = colorObject.optBoolean("blue", getBlue()) ? 1 : 0;
        }
        invertLUT = jo.optBoolean("invert", invertLUT);
    }

    public JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("sharpen", sharpen);
        jo.put("opacity", opacity);
        jo.put("blend", blend);
        jo.put("slitLeft", slitLeft);
        jo.put("slitRight", slitRight);
        jo.put("sectorCenter", sectorCenter);
        jo.put("sectorWidth", sectorWidth);
        jo.put("innerMask", innerMask);
        jo.put("outerMask", Double.isFinite(outerMask) ? outerMask : JSONObject.NULL);
        jo.put("brightOffset", brightOffset);
        jo.put("brightScale", brightScale);
        jo.put("enhanced", enhanced);
        jo.put("upsilonLow", upsilonLow);
        jo.put("upsilonHigh", upsilonHigh);
        jo.put("differenceMode", differenceMode);

        JSONObject colorObject = new JSONObject();
        colorObject.put("red", getRed());
        colorObject.put("green", getGreen());
        colorObject.put("blue", getBlue());
        jo.put("color", colorObject);
        jo.put("invert", invertLUT);

        return jo;
    }
}
