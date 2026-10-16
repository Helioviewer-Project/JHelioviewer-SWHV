package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.image.ImageGrid;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.wcs.ImageGeometry;
import org.helioviewer.jhv.wcs.WcsHeader;

public final class GLSLImageShader extends GLSLScreenShader {

    private static final String COMMON_FRAGMENT = "/glsl/imageCommon.frag";

    private static final GLSLImageShader ortho = new GLSLImageShader("/glsl/imageOrtho.frag");
    private static final GLSLImageShader hpc = new GLSLImageShader("/glsl/imageHpc.frag");
    private static final GLSLImageShader lati = new GLSLImageShader("/glsl/imageLati.frag");
    private static final GLSLImageShader radialWarp = new GLSLImageShader("/glsl/imageRadialWarp.frag");
    private static final GLSLImageShader rectWarp = new GLSLImageShader("/glsl/imageRectWarp.frag");
    private static final GLSLImageShader[] imagePrograms = {ortho, hpc, lati, radialWarp, rectWarp};

    private int pv0Ref;
    private int pv1Ref;

    private GLSLImageShader(String fragment) {
        super(COMMON_FRAGMENT, fragment);
    }

    private static final UniformBufferObject imageBuffer = new UniformBufferObject(UniformBlockLayout.IMAGE, GL.STREAM_DRAW);
    private static final UniformBufferObject displayBuffer = new UniformBufferObject(UniformBlockLayout.DISPLAY, GL.STREAM_DRAW);

    static void init() {
        try {
            imageBuffer.init();
            displayBuffer.init();
            imageBuffer.bind();
            displayBuffer.bind();
            for (GLSLImageShader program : imagePrograms)
                program._init();
        } catch (RuntimeException | Error e) {
            dispose();
            throw e;
        }
    }

    @Override
    protected void initUniforms(int id) {
        super.initUniforms(id);
        pv0Ref = requiredUniform(id, "pv0");
        pv1Ref = requiredUniform(id, "pv1");
        setupUniformBlock(id, UniformBlockLayout.IMAGE);
        setupUniformBlock(id, UniformBlockLayout.DISPLAY);
        setTextureUnit(id, "image", GLTexture.Unit.ZERO);
        setTextureUnit(id, "lut", GLTexture.Unit.ONE);
        setTextureUnit(id, "diffImage", GLTexture.Unit.TWO);
        setTextureUnit(id, "mask", GLTexture.Unit.THREE);
    }

    static void dispose() {
        for (GLSLImageShader program : imagePrograms)
            program._dispose();
        imageBuffer.dispose();
        displayBuffer.dispose();
    }

    public static void bindImages(
            ImageGeometry image0, float[] crval0,
            float observerDistance0, float deltaT0, Quat cameraDiff0, Quat sourceView0,
            ImageGeometry image1, float[] crval1,
            float observerDistance1, float deltaT1, Quat cameraDiff1, Quat sourceView1, Mat2 adjustment) {
        FloatBuffer values = imageBuffer.begin();
        putImage(values, image0, crval0, observerDistance0, deltaT0, cameraDiff0, sourceView0, adjustment);
        putImage(values, image1, crval1, observerDistance1, deltaT1, cameraDiff1, sourceView1, adjustment);

        imageBuffer.uploadIfChanged();
    }

    private static void putImage(FloatBuffer values, ImageGeometry image, float[] crval,
                                 float observerDistance, float deltaT, Quat cameraDiff, Quat sourceView, Mat2 adjustment) {
        putTransform(values, image.original.planeToTexture, image.original.referenceTexture, adjustment);
        ImageGrid grid = image.grid;
        double step = grid.step();
        // Original normalized pixels -> decoded normalized pixels: scale, then offset.
        values.put((float) (image.original.grid.width() / (step * grid.width())));
        values.put((float) (image.original.grid.height() / (step * grid.height())));
        values.put((float) ((.5 - grid.firstX() / step) / grid.width()));
        values.put((float) ((.5 - grid.firstY() / step) / grid.height()));
        WcsHeader wcs = image.wcs;
        values.put(crval).put((float) wcs.unitsPerRad).put(wcs.projection.ordinal());
        values.put((float) wcs.zpnUpperEta).put(observerDistance).put(deltaT).put(0);
        cameraDiff.setFloatBuffer(values);
        sourceView.setFloatBuffer(values);
    }

    // The user rotation acts on the plane, so it precedes the plane-to-texture matrix.
    private static void putTransform(FloatBuffer values, Mat2 matrix, Vec2 reference, Mat2 adjustment) {
        values.put((float) (matrix.m00 * adjustment.m00 + matrix.m01 * adjustment.m10));
        values.put((float) (matrix.m00 * adjustment.m01 + matrix.m01 * adjustment.m11));
        values.put((float) reference.x).put(0);
        values.put((float) (matrix.m10 * adjustment.m00 + matrix.m11 * adjustment.m10));
        values.put((float) (matrix.m10 * adjustment.m01 + matrix.m11 * adjustment.m11));
        values.put((float) reference.y).put(0);
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
        FloatBuffer values = displayBuffer.begin();
        values.put(color);
        values.put(shWidth).put(shHeight).put(shWeight).put(isDiff);
        values.put(bOffset).put(bScale).put(upsilonLow).put(upsilonHigh);
        values.put(userSectorCenter).put(userSectorHalfWidth).put(metadataSectorCenter).put(metadataSectorHalfWidth);
        values.put(cutOffX).put(cutOffY).put(cutOffVal).put(calculateDepth);
        values.put(innerRadius).put(outerRadius).put(slitLeft).put(slitRight);
        values.put(enhanced).put(0).put(0).put(0);
        displayBuffer.uploadIfChanged();
    }

    public static void render(MapMode mode, float[] pv0, float[] pv1) {
        GLSLImageShader shader = switch (mode) {
            case Orthographic -> ortho;
            case HPC -> hpc;
            case Latitudinal -> lati;
            case RadialWarp -> radialWarp;
            case RectWarp -> rectWarp;
        };
        shader.use();
        shader.bindPV(pv0, pv1);
        shader.draw();
    }

    private void bindPV(float[] pv0, float[] pv1) {
        GL.glUniform1fv(pv0Ref, pv0);
        GL.glUniform1fv(pv1Ref, pv1);
    }

}
