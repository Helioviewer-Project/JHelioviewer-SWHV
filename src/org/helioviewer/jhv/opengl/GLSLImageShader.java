package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.wcs.ImageGeometry;
import org.helioviewer.jhv.wcs.ImageGrid;
import org.helioviewer.jhv.wcs.WcsSolution;

public final class GLSLImageShader extends GLSLScreenShader {

    private static final String COMMON_FRAGMENT = "/glsl/imageCommon.frag";

    private static final GLSLImageShader ortho = new GLSLImageShader("/glsl/imageOrtho.frag");
    private static final GLSLImageShader hpc = new GLSLImageShader("/glsl/imageHpc.frag");
    private static final GLSLImageShader lati = new GLSLImageShader("/glsl/imageLati.frag");
    private static final GLSLImageShader radialWarp = new GLSLImageShader("/glsl/imageRadialWarp.frag");
    private static final GLSLImageShader rectWarp = new GLSLImageShader("/glsl/imageRectWarp.frag");
    private static final GLSLImageShader[] imagePrograms = {ortho, hpc, lati, radialWarp, rectWarp};

    // JHV Orthographic TAN display policy (ORTHOGRAPHIC_TAN in imageCommon.frag).
    private static final float ORTHOGRAPHIC_TAN = 6;

    private GLSLImageShader(String fragment) {
        super(COMMON_FRAGMENT, "/glsl/imageGeometry.frag", fragment);
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

    // Observer images are traced from their own viewpoint, maps from the display viewpoint.
    public static void render(MapMode mode, Quat displayRotation, Position display,
                              ImageGeometry image0, Position source0,
                              ImageGeometry image1, Position source1,
                              Mat2 adjustment, double longitudeOffset, double latitudeOffset, boolean differentialRotation) {
        FloatBuffer values = imageBuffer.begin();
        putImage(values, mode, displayRotation, display, image0, source0, adjustment, longitudeOffset, latitudeOffset, differentialRotation);
        putImage(values, mode, displayRotation, display, image1, source1, adjustment, longitudeOffset, latitudeOffset, differentialRotation);

        imageBuffer.uploadIfChanged();

        GLSLImageShader shader = switch (mode) {
            case Orthographic -> ortho;
            case HPC -> hpc;
            case Latitudinal -> lati;
            case RadialWarp -> radialWarp;
            case RectWarp -> rectWarp;
        };
        shader.use();
        shader.draw();
    }

    private static void putImage(FloatBuffer values, MapMode mode, Quat displayRotation, Position display, ImageGeometry image,
                                 Position source, Mat2 adjustment, double longitudeOffset, double latitudeOffset, boolean differentialRotation) {
        WcsSolution wcs = image.adjustedWcs(longitudeOffset, latitudeOffset);
        ImageGeometry original = image.original;
        int width = original.grid.width(), height = original.grid.height();
        boolean surface = wcs.projection.isSurfaceMap();
        Position tracing = image.tracingViewpoint(display, source);
        Quat solarToImage = surface ? displayRotation : source.toQuat();
        float observerDistance = (float) tracing.distance;
        float deltaT = differentialRotation ? (float) ((display.time.milli - tracing.time.milli) * 1e-9) : 0;
        // Orthographic TAN deliberately uses source XY as a small-angle plane.
        boolean orthographicTan = mode == MapMode.Orthographic && wcs.projection == WcsSolution.Projection.TAN;
        double offsetX = orthographicTan ? wcs.reference.x * wcs.unitsPerRad : wcs.offset.x;
        double offsetY = orthographicTan ? wcs.reference.y * wcs.unitsPerRad : wcs.offset.y;
        Mat2 matrix = original.planeToPixel;
        Vec2 reference = original.referencePixel;
        double a = (matrix.m00 * adjustment.m00 + matrix.m01 * adjustment.m10) / width;
        double b = (matrix.m00 * adjustment.m01 + matrix.m01 * adjustment.m11) / width;
        double c = (matrix.m10 * adjustment.m00 + matrix.m11 * adjustment.m10) / height;
        double d = (matrix.m10 * adjustment.m01 + matrix.m11 * adjustment.m11) / height;
        // Formal projections return radians; compose their unit conversion into the affine coefficients.
        double scale = orthographicTan ? 1 : wcs.unitsPerRad;
        float imageXFromPlaneX = (float) (a * scale);
        float imageXFromPlaneY = (float) (b * scale);
        float imageXOffset = (float) (reference.x / width - a * offsetX - b * offsetY);
        float imageYFromPlaneX = (float) (c * scale);
        float imageYFromPlaneY = (float) (d * scale);
        float imageYOffset = (float) (reference.y / height - c * offsetX - d * offsetY);
        float minSurfaceZ = surface ? Float.NEGATIVE_INFINITY : orthographicTan ? 0 : 1 / observerDistance;
        float projectionOriginZ = surface ? 0 : observerDistance;
        float projectionCode = orthographicTan ? ORTHOGRAPHIC_TAN : wcs.projection.ordinal();

        ImageGrid grid = image.grid;
        double step = grid.step();
        float texScaleX = (float) (width / (step * grid.width()));
        float texScaleY = (float) (height / (step * grid.height()));
        float texOffsetX = (float) ((.5 - grid.firstX() / step) / grid.width());
        float texOffsetY = (float) ((.5 - grid.firstY() / step) / grid.height());

        Vec3 toProjectionX = wcs.toProjectionX;
        Vec3 toProjectionY = wcs.toProjectionY;
        Vec3 toProjectionZ = wcs.toProjectionZ;
        if (surface) {
            toProjectionX = solarToImage.rotateVector(toProjectionX);
            toProjectionY = solarToImage.rotateVector(toProjectionY);
            toProjectionZ = solarToImage.rotateVector(toProjectionZ);
        }
        float projectionZSign = surface ? -1 : 1;
        Quat imageToView = Quat.rotateWithConjugate(displayRotation, solarToImage);

        // Column-major std140 mat3: each three-component column occupies four floats.
        values.put((float) toProjectionX.x).put((float) toProjectionY.x).put((float) toProjectionZ.x).put(0);
        values.put((float) toProjectionX.y).put((float) toProjectionY.y).put((float) toProjectionZ.y).put(0);
        values.put((float) (projectionZSign * toProjectionX.z)).put((float) (projectionZSign * toProjectionY.z))
                .put((float) (projectionZSign * toProjectionZ.z)).put(0);
        values.put(projectionCode).put(wcs.azpTanTilt).put(wcs.azpSecTilt).put(wcs.zpnMaxAngle);
        values.put(wcs.pv2);
        values.put((float) wcs.northPole.x).put((float) wcs.northPole.y);
        values.put((float) wcs.southPole.x).put((float) wcs.southPole.y);

        // Image mapping: affine columns and offset, then decoded texture mapping.
        values.put(imageXFromPlaneX).put(imageYFromPlaneX);
        values.put(imageXFromPlaneY).put(imageYFromPlaneY);
        values.put(imageXOffset).put(imageYOffset);
        values.put(texScaleX).put(texScaleY);
        values.put(texOffsetX).put(texOffsetY);

        // Tracing viewpoint and frame rotations.
        values.put(observerDistance).put(projectionOriginZ).put(minSurfaceZ).put(deltaT);
        imageToView.setFloatBuffer(values);
        solarToImage.setFloatBuffer(values);
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

}
