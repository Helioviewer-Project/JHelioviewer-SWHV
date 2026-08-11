package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.util.Set;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.metadata.DetectorMask;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.view.View;

import org.json.JSONObject;

public class GLImage {

    public enum DifferenceMode {
        None, Running, Base
    }

    public static final int MIN_DCROTA = -15;
    public static final int MAX_DCROTA = 15;
    public static final int MIN_DCRVAL = -180;
    public static final int MAX_DCRVAL = 180;
    public static final int MAX_MASK = 32;

    private GLTexture tex;
    private GLTexture lutTex;
    private GLTexture diffTex;
    private GLTexture maskTex;

    private float red = 1;
    private float green = 1;
    private float blue = 1;

    private double deltaCROTA = 0;
    private int deltaCRVAL1 = 0;
    private int deltaCRVAL2 = 0;

    private double innerMask = 0;
    private double outerMask = Double.POSITIVE_INFINITY;
    private double slitLeft = 0;
    private double slitRight = 1;
    private double sectorCenter;
    private double sectorWidth;
    private double brightOffset = 0;
    private double brightScale = 1;
    private double opacity = 1;
    private double blend = .5;
    private double sharpen = 0;
    private double enhanced = 0;
    // RHEF two-sided midtone control (Upsilon), AIA 171 defaults (Gilly & DeForest 2024, §3.2,
    // https://arxiv.org/html/2511.02798v1). The curve below the median (shadows, upsilonLow) and
    // above it (highlights, upsilonHigh) are shaped independently, so the two handles are
    // asymmetric by design. sunkit-image's rhef exposes the same split light/dark upsilon:
    // https://docs.sunpy.org/projects/sunkit-image/en/stable/api/sunkit_image.radial.rhef.html
    private double upsilonLow = .6;
    private double upsilonHigh = .4;
    private DifferenceMode diffMode = DifferenceMode.None;

    private LUT lut = LUT.gray();
    private LUT lastLut;

    private boolean invertLUT = false;
    private boolean lastInverted = false;
    private boolean lutChanged = true;
    private DetectorMask uploadedMask = DetectorMask.NONE;
    private View.ImageData uploadedImageData;

    public void streamImage(View.ImageData imageData, View.ImageData prevImageData, View.ImageData baseImageData) {
        if (uploadedImageData != imageData) {
            tex.bind();
            tex.copyImageBuffer(imageData.imageBuffer(), GL.LINEAR);
            uploadedImageData = imageData;
        }

        View.ImageData prevFrame = diffMode == DifferenceMode.Base ? baseImageData : prevImageData;
        if (diffMode != DifferenceMode.None && prevFrame != null) {
            diffTex.bind();
            diffTex.copyImageBuffer(prevFrame.imageBuffer(), GL.LINEAR);
        }
    }

    public void collectImageBuffers(Set<ImageBuffer> retained) {
        if (uploadedImageData != null)
            retained.add(uploadedImageData.imageBuffer());
    }

    private final float[] color = new float[4];

    public void applyFilters(boolean rhefActive) {
        MetaData metaData = uploadedImageData.metaData();
        float userSectorCenter = 0;
        float userSectorHalfWidth = 0;
        if (sectorWidth != 0) {
            userSectorCenter = (float) Math.toRadians(sectorCenter);
            userSectorHalfWidth = (float) Math.toRadians(sectorWidth / 2);
        }
        double metadataHalfWidth = metadataSectorHalfWidth(metaData);
        float metadataSectorCenter = (float) metadataSectorCenter(metaData, metadataHalfWidth);

        color[0] = (float) (opacity * red); // https://amindforeverprogramming.blogspot.com/2013/07/why-alpha-premultiplied-colour-blending.html
        color[1] = (float) (opacity * green);
        color[2] = (float) (opacity * blue);
        color[3] = (float) (opacity * blend);
        GLSLSolarShader.bindDisplay(color,
                1f / uploadedImageData.imageBuffer().width, 1f / uploadedImageData.imageBuffer().height, (float) (-2 * sharpen), diffMode.ordinal(),
                userSectorCenter, userSectorHalfWidth, metadataSectorCenter, (float) metadataHalfWidth,
                metaData.getCutOffX(), metaData.getCutOffY(), metaData.getCutOffValue(), metaData.getCalculateDepth() ? 1 : 0,
                // RHEF output is already a normalized rank in [0, 1]; the raw-DN response
                // factor must NOT rescale it (that pushes the uniform upper half past 1 and
                // clamps it to white). The user's Levels (brightOffset/brightScale) still
                // apply as a black/white-point control on the equalized output.
                (float) brightOffset, (float) (brightScale * (rhefActive ? 1 : metaData.getResponseFactor())),
                Math.max(metaData.getInnerRadius(), (float) innerMask),
                Math.min(Display.getShowCorona() ? metaData.getOuterRadius() : 1, (float) outerMask),
                (float) slitLeft, (float) slitRight,
                (float) enhanced,
                (float) (rhefActive ? upsilonLow : 1), (float) (rhefActive ? upsilonHigh : 1));

        applyLUT();
        applyMask(metaData.getDetectorMask());
        maskTex.bind();
        tex.bind();
        if (diffMode != DifferenceMode.None)
            diffTex.bind();
    }

    private void applyLUT() {
        lutTex.bind();

        LUT currlut = diffMode == DifferenceMode.None ? lut : LUT.gray();
        if (lutChanged || lastLut != currlut || invertLUT != lastInverted) {
            ByteBuffer lutBuffer = invertLUT ? currlut.rgbaInv() : currlut.rgba();
            lastLut = currlut;
            lastInverted = invertLUT;

            GLTexture.copyByteImage(lutBuffer.remaining() / 4, 1, GL.NEAREST, lutBuffer);
        }
        lutChanged = false;
    }

    public void init() {
        tex = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.ZERO);
        lutTex = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.ONE);
        diffTex = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.TWO);
        maskTex = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.THREE);
        // Texture objects were recreated, so their corresponding upload bookkeeping must start fresh.
        uploadedImageData = null;
        lutChanged = true;
        uploadedMask = DetectorMask.NONE;

        // Keep diffImage and mask samplers backed by a complete texture from startup to avoid macOS driver warnings.
        diffTex.bind();
        ByteBuffer emptyDiffTexture = BufferUtils.newByteBuffer(4).put(new byte[]{0, 0, 0, (byte) 0xFF}).flip();
        GLTexture.copyByteImage(1, 1, GL.LINEAR, emptyDiffTexture);

        maskTex.bind();
        maskTex.copyImageBuffer(uploadedMask.getImageBuffer(), GL.NEAREST);
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
    }

    private void applyMask(DetectorMask detectorMask) {
        if (uploadedMask == detectorMask)
            return;
        maskTex.bind();
        maskTex.copyImageBuffer(detectorMask.getImageBuffer(), GL.NEAREST);
        uploadedMask = detectorMask;
    }

    public void setDeltaCROTA(double delta) {
        deltaCROTA = Math.clamp(delta, MIN_DCROTA, MAX_DCROTA);
    }

    public void setDeltaCRVAL1(int delta) {
        deltaCRVAL1 = Math.clamp(delta, MIN_DCRVAL, MAX_DCRVAL);
    }

    public void setDeltaCRVAL2(int delta) {
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

    public int getDeltaCRVAL1() {
        return deltaCRVAL1;
    }

    public int getDeltaCRVAL2() {
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

    public double getSectorCenter() {
        return sectorCenter;
    }

    public double getSectorWidth() {
        return sectorWidth;
    }

    private static double metadataSectorCenter(MetaData metaData, double halfWidth) {
        if (halfWidth == 0)
            return 0;
        double center = metaData.getSector1() + halfWidth;
        return (center + 3 * Math.PI) % (2 * Math.PI) - Math.PI;
    }

    private static double metadataSectorHalfWidth(MetaData metaData) {
        float start = metaData.getSector0();
        float end = metaData.getSector1();
        return start == end ? 0 : Math.PI + (start - end) / 2;
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
        opacity = Math.clamp(_opacity, 0, 1);
    }

    public void setBlend(double _blend) {
        blend = Math.clamp(_blend, 0, 1);
    }

    public void setSharpen(double _sharpen) {
        sharpen = Math.clamp(_sharpen, -1, 1);
    }

    public void setLUT(LUT newLUT, boolean invert) {
        if (lut == newLUT && invertLUT == invert) {
            return;
        }
        if (newLUT == null)
            newLUT = LUT.gray();

        lut = newLUT;
        invertLUT = invert;
        lutChanged = true;
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

    public void setDifferenceMode(DifferenceMode mode) {
        diffMode = mode;
    }

    public DifferenceMode getDifferenceMode() {
        return diffMode;
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
        String strDiffMode = jo.optString("differenceMode", diffMode.toString());
        try {
            diffMode = DifferenceMode.valueOf(strDiffMode);
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
