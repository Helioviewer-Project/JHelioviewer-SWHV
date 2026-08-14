package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.wcs.WcsHeader;

public class GLSLSolarShader extends GLSLShader {

    public static final GLSLSolarShader sphere = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarSphere.frag", false);
    public static final GLSLSolarShader ortho = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarOrtho.frag", true);
    public static final GLSLSolarShader hpc = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarHpc.frag", true);
    public static final GLSLSolarShader lati = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLati.frag", true);
    public static final GLSLSolarShader radialWarp = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarRadialWarp.frag", true);
    public static final GLSLSolarShader rectWarp = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarRectWarp.frag", true);

    private final boolean hasCommon;

    private int pv0Ref;
    private int pv1Ref;

    private GLSLSolarShader(String vertex, String fragment, boolean _hasCommon) {
        super(vertex, fragment);
        hasCommon = _hasCommon;
    }

    private static GLBO imageBO;
    private static final int IMAGE_FLOATS = 48;
    private static final FloatBuffer imageBuf = BufferUtils.newFloatBuffer(IMAGE_FLOATS);
    private static final int IMAGE_SIZE = IMAGE_FLOATS * Float.BYTES;

    private static GLBO screenBO;
    private static final int SCREEN_FLOATS = 24;
    private static final FloatBuffer screenBuf = BufferUtils.newFloatBuffer(SCREEN_FLOATS);
    private static final int SCREEN_SIZE = SCREEN_FLOATS * Float.BYTES;

    private static GLBO displayBO;
    private static final int DISPLAY_FLOATS = 28;
    private static final FloatBuffer displayBuf = BufferUtils.newFloatBuffer(DISPLAY_FLOATS);
    private static final int DISPLAY_SIZE = DISPLAY_FLOATS * Float.BYTES;

    public static void init() {
        imageBO = new GLBO(GL.UNIFORM_BUFFER, GL.STREAM_DRAW);
        screenBO = new GLBO(GL.UNIFORM_BUFFER, GL.STREAM_DRAW);
        displayBO = new GLBO(GL.UNIFORM_BUFFER, GL.STREAM_DRAW);

        sphere._init(sphere.hasCommon);
        ortho._init(ortho.hasCommon);
        hpc._init(hpc.hasCommon);
        lati._init(lati.hasCommon);
        radialWarp._init(radialWarp.hasCommon);
        rectWarp._init(rectWarp.hasCommon);
    }

    private static void setupCommonBlocks(int programID) {
        setupUBO(programID, "ImageBlock", imageBO.getID(), UBO.IMAGE);
        setupUBO(programID, "ScreenBlock", screenBO.getID(), UBO.SOLAR_SCREEN);
        setupUBO(programID, "DisplayBlock", displayBO.getID(), UBO.DISPLAY);
    }

    @Override
    protected void initUniforms(int id) {
        pv0Ref = GL.glGetUniformLocation(id, "pv0");
        pv1Ref = GL.glGetUniformLocation(id, "pv1");

        setupCommonBlocks(id);

        if (hasCommon) {
            setTextureUnit(id, "image", GLTexture.Unit.ZERO);
            setTextureUnit(id, "lut", GLTexture.Unit.ONE);
            setTextureUnit(id, "diffImage", GLTexture.Unit.TWO);
            setTextureUnit(id, "mask", GLTexture.Unit.THREE);
        }
    }

    public static void dispose() {
        sphere._dispose();
        ortho._dispose();
        hpc._dispose();
        lati._dispose();
        radialWarp._dispose();
        rectWarp._dispose();
        imageBO.delete();
        screenBO.delete();
        displayBO.delete();
    }

    public static void bindImages(
            Region r0, Mat2 planeToImage0, float[] crval0, WcsHeader wcs0,
            float observerDistance0, float deltaT0, Quat cameraDiff0, Quat sourceView0,
            Region r1, Mat2 planeToImage1, float[] crval1, WcsHeader wcs1,
            float observerDistance1, float deltaT1, Quat cameraDiff1, Quat sourceView1) {
        putImage(r0, planeToImage0, crval0, wcs0, observerDistance0, deltaT0, cameraDiff0, sourceView0);
        putImage(r1, planeToImage1, crval1, wcs1, observerDistance1, deltaT1, cameraDiff1, sourceView1);

        imageBuf.flip();
        imageBO.setBufferDataIfChanged(IMAGE_SIZE, imageBuf);
    }

    private static void putImage(Region r, Mat2 planeToImage, float[] crval, WcsHeader wcs,
                                 float observerDistance, float deltaT, Quat cameraDiff, Quat sourceView) {
        imageBuf.put(r.glslArray);
        planeToImage.setFloatBuffer(imageBuf);
        imageBuf.put(crval).put((float) wcs.unitsPerRad).put(wcs.projection.ordinal());
        imageBuf.put((float) wcs.zpnUpperEta).put(observerDistance).put(deltaT).put(0);
        cameraDiff.setFloatBuffer(imageBuf);
        sourceView.setFloatBuffer(imageBuf);
    }

    public static void bindScreen(MapView mv, Viewport vp) {
        MapScale scale = mv.scale(vp);
        FloatBuffer inv = Transform.getInverse();
        screenBuf.put(inv);
        inv.flip();
        screenBuf.put((float) scale.toMapX(0)).put((float) scale.toMapX(1));
        screenBuf.put((float) scale.toMapY(0)).put((float) scale.toMapY(1));
        screenBuf.put((float) mv.latiLongitudeOrigin()).put((float) mv.latiLatitudeOrigin());
        screenBuf.put((float) (1 / vp.aspect));
        screenBuf.put((float) scale.warpLambda());

        screenBuf.flip();
        screenBO.setBufferData(SCREEN_SIZE, screenBuf); // always changes
    }

    static void bindDisplay(float[] color,
                            float shWidth, float shHeight, float shWeight, int isDiff,
                            float bOffset, float bScale,
                            float upsilonLow, float upsilonHigh,
                            float userSectorCenter, float userSectorHalfWidth, float metadataSectorCenter, float metadataSectorHalfWidth,
                            float cutOffX, float cutOffY, float cutOffVal, int calculateDepth,
                            float innerRadius, float outerRadius,
                            float slitLeft, float slitRight,
                            float enhanced) {
        displayBuf.put(color);
        displayBuf.put(shWidth).put(shHeight).put(shWeight).put(isDiff);
        displayBuf.put(bOffset).put(bScale).put(upsilonLow).put(upsilonHigh);
        displayBuf.put(userSectorCenter).put(userSectorHalfWidth).put(metadataSectorCenter).put(metadataSectorHalfWidth);
        displayBuf.put(cutOffX).put(cutOffY).put(cutOffVal).put(calculateDepth);
        displayBuf.put(innerRadius).put(outerRadius).put(slitLeft).put(slitRight);
        displayBuf.put(enhanced).put(0).put(0).put(0);

        displayBuf.flip();
        displayBO.setBufferDataIfChanged(DISPLAY_SIZE, displayBuf);
    }

    public void bindPV(float[] pv0, float[] pv1) {
        GL.glUniform1fv(pv0Ref, pv0);
        GL.glUniform1fv(pv1Ref, pv1);
    }
}
