package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;

import com.jogamp.opengl.GL3;

public class GLSLSolarShader extends GLSLShader {

    public static final GLSLSolarShader sphere = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarSphere.frag", false);
    public static final GLSLSolarShader ortho = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarOrtho.frag", true);
    public static final GLSLSolarShader lati = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLati.frag", true);
    public static final GLSLSolarShader polar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarPolar.frag", true);
    public static final GLSLSolarShader logpolar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLogPolar.frag", true);

    private final boolean hasCommon;

    private int gridRef;

    private final int[] intArr = new int[1];
    private final float[] floatArr = new float[8];

    private GLSLSolarShader(String vertex, String fragment, boolean _hasCommon) {
        super(vertex, fragment);
        hasCommon = _hasCommon;
    }

    private static GLBO screenBO;
    private static final FloatBuffer screenBuf = BufferUtils.newFloatBuffer(16 + 4 + 4);
    private static final int SCREEN_SIZE = screenBuf.capacity() * 4;

    private static GLBO wcsBO;
    private static final FloatBuffer wcsBuf = BufferUtils.newFloatBuffer(2 * (4 + 4 + 4 + 4));
    private static final int WCS_SIZE = wcsBuf.capacity() * 4;

    private static GLBO displayBO;
    private static final FloatBuffer displayBuf = BufferUtils.newFloatBuffer(4 + 4 + 4 + 4 + 4 + 4);
    private static final int DISPLAY_SIZE = displayBuf.capacity() * 4;

    public static void init(GL3 gl) {
        screenBO = new GLBO(gl, GL3.GL_UNIFORM_BUFFER, GL3.GL_STREAM_DRAW);
        wcsBO = new GLBO(gl, GL3.GL_UNIFORM_BUFFER, GL3.GL_STREAM_DRAW);
        displayBO = new GLBO(gl, GL3.GL_UNIFORM_BUFFER, GL3.GL_STREAM_DRAW);

        sphere._init(gl, sphere.hasCommon);
        ortho._init(gl, ortho.hasCommon);
        lati._init(gl, lati.hasCommon);
        polar._init(gl, polar.hasCommon);
        logpolar._init(gl, logpolar.hasCommon);
    }

    private static void setupUBO(GL3 gl, int programID, String blockName, int uboID, int binding) {
        int blockIndex = gl.glGetUniformBlockIndex(programID, blockName);
        gl.glUniformBlockBinding(programID, blockIndex, binding);
        gl.glBindBufferBase(GL3.GL_UNIFORM_BUFFER, binding, uboID);
    }

    @Override
    protected void initUniforms(GL3 gl, int id) {
        gridRef = gl.glGetUniformLocation(id, "grid");

        setupUBO(gl, id, "ScreenBlock", screenBO.getID(), 0);
        setupUBO(gl, id, "WCSBlock", wcsBO.getID(), 1);
        setupUBO(gl, id, "DisplayBlock", displayBO.getID(), 2);

        if (hasCommon) {
            setTextureUnit(gl, id, "image", GLTexture.Unit.ZERO);
            setTextureUnit(gl, id, "lut", GLTexture.Unit.ONE);
            setTextureUnit(gl, id, "diffImage", GLTexture.Unit.TWO);
        }
    }

    public static void dispose(GL3 gl) {
        sphere._dispose(gl);
        ortho._dispose(gl);
        lati._dispose(gl);
        polar._dispose(gl);
        logpolar._dispose(gl);
        screenBO.delete(gl);
        wcsBO.delete(gl);
        displayBO.delete(gl);
    }

    public static void bindScreen(GL3 gl, Viewport vp) {
        FloatBuffer inv = Transform.getInverse();
        screenBuf.put(inv);
        inv.flip();
        screenBuf.put(vp.glslArray).put((float) (1 / vp.aspect));
        screenBuf.put((float) Display.mode.scale.getYstart()).put((float) Display.mode.scale.getYstop());

        screenBO.setBufferData(gl, SCREEN_SIZE, SCREEN_SIZE, screenBuf.flip());
    }

    public void bindWCS(GL3 gl,
                        Quat cameraDiff0, Region r0, Quat crota0, float[] crval0, float deltaT0,
                        Quat cameraDiff1, Region r1, Quat crota1, float[] crval1, float deltaT1) {
        cameraDiff0.setFloatBuffer(wcsBuf);
        wcsBuf.put(r0.glslArray);
        crota0.setFloatBuffer(wcsBuf);
        wcsBuf.put(crval0);
        wcsBuf.put(deltaT0).put(0);

        cameraDiff1.setFloatBuffer(wcsBuf);
        wcsBuf.put(r1.glslArray);
        crota1.setFloatBuffer(wcsBuf);
        wcsBuf.put(crval1);
        wcsBuf.put(deltaT1).put(0);

        wcsBO.setBufferData(gl, WCS_SIZE, WCS_SIZE, wcsBuf.flip());
    }

    public void bindDisplay(GL3 gl,
                            float red, float green, float blue, float alpha,
                            float shWidth, float shHeight, float shWeight, int isDiff,
                            float sector0, float sector1, int enhanced,
                            float cutOffX, float cutOffY, float cutOffVal, int calculateDepth,
                            float bOffset, float bScale,
                            float innerRadius, float outerRadius,
                            float slitLeft, float slitRight) {
        displayBuf.put(red).put(green).put(blue).put(alpha);
        displayBuf.put(shWidth).put(shHeight).put(shWeight).put(isDiff);
        displayBuf.put(sector0).put(sector1).put(/*sector0 + 2 * Math.PI == sector1*/ sector0 == sector1 ? 0 : 1).put(enhanced);
        displayBuf.put(cutOffX).put(cutOffY).put(cutOffVal).put(calculateDepth);
        displayBuf.put(bOffset).put(bScale);
        displayBuf.put(innerRadius).put(outerRadius).put(slitLeft).put(slitRight);

        displayBO.setBufferData(gl, DISPLAY_SIZE, DISPLAY_SIZE, displayBuf.flip());
    }

    public void bindAnglesLatiGrid(GL3 gl, double lon, double lat, double hglt, double dlon, double dlat, double dhglt) {
        floatArr[0] = (float) lon;
        floatArr[1] = (float) lat;
        floatArr[2] = (float) hglt;
        floatArr[3] = (float) dlon;
        floatArr[4] = (float) dlat;
        floatArr[5] = (float) dhglt;
        gl.glUniform3fv(gridRef, 2, floatArr, 0);
    }

}
