package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL33;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.wcs.WcsHeader;

public class GLSLSolarShader extends GLSLShader {

    public static final GLSLSolarShader sphere = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarSphere.frag", false);
    public static final GLSLSolarShader ortho = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarOrtho.frag", true);
    public static final GLSLSolarShader hpc = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarHpc.frag", true);
    public static final GLSLSolarShader lati = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLati.frag", true);
    public static final GLSLSolarShader polar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarPolar.frag", true);
    public static final GLSLSolarShader logpolar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLogPolar.frag", true);

    private final boolean hasCommon;

    private int pv0Ref;
    private int pv1Ref;
    private int latiGridRef;
    private static final float[] latiGridBuf = new float[6];

    private GLSLSolarShader(String vertex, String fragment, boolean _hasCommon) {
        super(vertex, fragment);
        hasCommon = _hasCommon;
    }

    private static GLBO wcsBO;
    private static final FloatBuffer wcsBuf = BufferUtils.newFloatBuffer(2 * (4 + 4 + 4 + 4));
    private static final int WCS_SIZE = wcsBuf.capacity() * 4;

    private static GLBO projectionBO;
    private static final FloatBuffer projectionBuf = BufferUtils.newFloatBuffer(2 * (4 + 4 + 4));
    private static final int PROJECTION_SIZE = projectionBuf.capacity() * 4;

    private static GLBO screenBO;
    private static final FloatBuffer screenBuf = BufferUtils.newFloatBuffer(16 + 4 + 4 + 4);
    private static final int SCREEN_SIZE = screenBuf.capacity() * 4;

    private static GLBO displayBO;
    private static final FloatBuffer displayBuf = BufferUtils.newFloatBuffer(4 + 4 + 4 + 4 + 4 + 4);
    private static final int DISPLAY_SIZE = displayBuf.capacity() * 4;

    public static void init() {
        wcsBO = new GLBO(GL33.GL_UNIFORM_BUFFER, GL33.GL_STREAM_DRAW);
        projectionBO = new GLBO(GL33.GL_UNIFORM_BUFFER, GL33.GL_STREAM_DRAW);
        screenBO = new GLBO(GL33.GL_UNIFORM_BUFFER, GL33.GL_STREAM_DRAW);
        displayBO = new GLBO(GL33.GL_UNIFORM_BUFFER, GL33.GL_STREAM_DRAW);

        sphere._init(sphere.hasCommon);
        ortho._init(ortho.hasCommon);
        hpc._init(hpc.hasCommon);
        lati._init(lati.hasCommon);
        polar._init(polar.hasCommon);
        logpolar._init(logpolar.hasCommon);
    }

    private static void setupUBO(int programID, String blockName, int uboID, int binding) {
        int blockIndex = GL33.glGetUniformBlockIndex(programID, blockName);
        if (blockIndex < 0)
            return;
        GL33.glUniformBlockBinding(programID, blockIndex, binding);
        GL33.glBindBufferBase(GL33.GL_UNIFORM_BUFFER, binding, uboID);
    }

    static void setupCommonBlocks(int programID) {
        setupUBO(programID, "WCSBlock", wcsBO.getID(), 0);
        setupUBO(programID, "ProjectionBlock", projectionBO.getID(), 1);
        setupUBO(programID, "ScreenBlock", screenBO.getID(), 2);
        setupUBO(programID, "DisplayBlock", displayBO.getID(), 3);
    }

    @Override
    protected void initUniforms(int id) {
        pv0Ref = GL33.glGetUniformLocation(id, "pv0");
        pv1Ref = GL33.glGetUniformLocation(id, "pv1");
        latiGridRef = GL33.glGetUniformLocation(id, "latiGrid");

        setupCommonBlocks(id);

        if (hasCommon) {
            setTextureUnit(id, "image", GLTexture.Unit.ZERO);
            setTextureUnit(id, "lut", GLTexture.Unit.ONE);
            setTextureUnit(id, "diffImage", GLTexture.Unit.TWO);
        }
    }

    public static void dispose() {
        sphere._dispose();
        ortho._dispose();
        hpc._dispose();
        lati._dispose();
        polar._dispose();
        logpolar._dispose();
        wcsBO.delete();
        projectionBO.delete();
        screenBO.delete();
        displayBO.delete();
    }

    public static void bindWCS(
                               Quat cameraDiff0, Region r0, Quat crota0, float[] crval0, float deltaT0,
                               Quat cameraDiff1, Region r1, Quat crota1, float[] crval1, float deltaT1) {
        cameraDiff0.setFloatBuffer(wcsBuf);
        wcsBuf.put(r0.glslArray);
        crota0.setFloatBuffer(wcsBuf);
        wcsBuf.put(crval0).put(deltaT0).put(0);

        cameraDiff1.setFloatBuffer(wcsBuf);
        wcsBuf.put(r1.glslArray);
        crota1.setFloatBuffer(wcsBuf);
        wcsBuf.put(crval1).put(deltaT1).put(0);

        wcsBO.setBufferData(WCS_SIZE, WCS_SIZE, wcsBuf.flip());
    }

    public static void bindProjection(
                                      WcsHeader.Projection projection0, float planeUnitsPerRad0, float observerDistance0,
                                      Quat sourceView0, Quat displayMap0,
                                      WcsHeader.Projection projection1, float planeUnitsPerRad1, float observerDistance1,
                                      Quat sourceView1, Quat displayMap1) {
        projectionBuf.put(projection0.ordinal()).put(planeUnitsPerRad0).put(observerDistance0).put(0);
        sourceView0.setFloatBuffer(projectionBuf);
        displayMap0.setFloatBuffer(projectionBuf);

        projectionBuf.put(projection1.ordinal()).put(planeUnitsPerRad1).put(observerDistance1).put(0);
        sourceView1.setFloatBuffer(projectionBuf);
        displayMap1.setFloatBuffer(projectionBuf);

        projectionBO.setBufferData(PROJECTION_SIZE, PROJECTION_SIZE, projectionBuf.flip());
    }

    public void bindLatiGrid(float[] latiGrid0, float[] latiGrid1) {
        latiGridBuf[0] = latiGrid0[0];
        latiGridBuf[1] = latiGrid0[1];
        latiGridBuf[2] = latiGrid0[2];
        latiGridBuf[3] = latiGrid1[0];
        latiGridBuf[4] = latiGrid1[1];
        latiGridBuf[5] = latiGrid1[2];
        GL33.glUniform3fv(latiGridRef, latiGridBuf);
    }

    static void bindScreen(Viewport vp) {
        GridScale scale = Display.mode.scale;
        FloatBuffer inv = Transform.getInverse();
        screenBuf.put(inv);
        inv.flip();
        screenBuf.put(vp.glslArray).put((float) (1 / vp.aspect));
        screenBuf.put((float) scale.getInterpolatedXValue(0));
        screenBuf.put((float) scale.getInterpolatedXValue(1));
        screenBuf.put((float) scale.getYstart()).put((float) scale.getYstop());

        screenBO.setBufferData(SCREEN_SIZE, SCREEN_SIZE, screenBuf.flip());
    }

    static void bindDisplay(float[] color,
                            float shWidth, float shHeight, float shWeight, int isDiff,
                            float sector0, float sector1, float enhanced,
                            float cutOffX, float cutOffY, float cutOffVal, int calculateDepth,
                            float bOffset, float bScale,
                            float innerRadius, float outerRadius,
                            float slitLeft, float slitRight) {
        displayBuf.put(color);
        displayBuf.put(shWidth).put(shHeight).put(shWeight).put(isDiff);
        displayBuf.put(sector0).put(sector1).put(/*sector0 + 2 * Math.PI == sector1*/ sector0 == sector1 ? 0 : 1).put(enhanced);
        displayBuf.put(cutOffX).put(cutOffY).put(cutOffVal).put(calculateDepth);
        displayBuf.put(bOffset).put(bScale);
        displayBuf.put(innerRadius).put(outerRadius).put(slitLeft).put(slitRight);

        displayBO.setBufferData(DISPLAY_SIZE, DISPLAY_SIZE, displayBuf.flip());
    }

    public void bindPV(float[] pv0, float[] pv1) {
        GL33.glUniform1fv(pv0Ref, pv0);
        GL33.glUniform1fv(pv1Ref, pv1);
    }

}
