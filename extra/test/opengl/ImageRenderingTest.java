package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageDisplaySettings;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.Frames;
import org.helioviewer.jhv.layers.ImageData;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.source.FITSSource;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.wcs.ImageGeometry;
import org.helioviewer.jhv.wcs.ImageGrid;
import org.helioviewer.jhv.wcs.ObserverGeometry;
import org.helioviewer.jhv.wcs.WcsSolution;

import org.json.JSONArray;
import org.json.JSONObject;

import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;

public final class ImageRenderingTest {
    private static final int TEXTURE_SIZE = 64;
    private static final JHVTime TIME = new JHVTime("2026-01-15T12:00:00");

    public static void main(String[] args) throws Exception {
        if (args.length > 1)
            throw new IllegalArgumentException("usage: ImageRenderingTest [output-directory]");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);
        Platform.init();
        Directories.createPersistentDirs();
        Log.init();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        Position viewpoint = Sun.getEarth(TIME);
        rayIntersections(viewpoint.time);
        DisplayController.setRenderRequestHandler(_ -> {});
        DisplayController.setViewpointUpdate(_ -> viewpoint, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
        AngleRenderer renderer = AngleRenderer.pbuffer(512, 512);
        GLStreamingTexture2D texture = new GLStreamingTexture2D(GLTexture.Unit.ZERO, GL.LINEAR);
        GLStreamingTexture2D difference = new GLStreamingTexture2D(GLTexture.Unit.TWO, GL.LINEAR);
        GLTexture lut = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.ONE);
        GLTexture mask = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.THREE);
        try {
            short[] samples = new short[TEXTURE_SIZE * TEXTURE_SIZE];
            short[] previous = new short[samples.length];
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                for (int x = 0; x < TEXTURE_SIZE; x++) {
                    float value = 0.15f + 0.65f * x / (TEXTURE_SIZE - 1) + ((x / 4 + y / 4) % 2) * 0.15f;
                    samples[y * TEXTURE_SIZE + x] = Float.floatToFloat16(value);
                    previous[y * TEXTURE_SIZE + x] = Float.floatToFloat16(0.4f);
                }
            }
            ImageBuffer image = ImageBuffer.fromShorts(TEXTURE_SIZE, TEXTURE_SIZE, samples, ImageFilter.NONE);
            ImageBuffer prior = ImageBuffer.fromShorts(TEXTURE_SIZE, TEXTURE_SIZE, previous, ImageFilter.NONE);
            ImageBuffer odd = ImageBuffer.fromBytes(13, 11, new byte[13 * 11]);
            texture.upload(odd);
            lut.upload2D(GLTexture.Format.RGBA8, 256, 1, GL.NEAREST, LUT.gray().rgba());
            texture.upload(odd);
            if (GLException.checkErrors("ImageRenderingTest Gray8 13x11"))
                throw new AssertionError("Odd-width grayscale upload failed");
            // Re-upload to exercise the streaming PBO path, not just initial allocation.
            texture.upload(prior);
            texture.upload(image);
            difference.upload(prior);
            ByteBuffer white = BufferUtils.newByteBuffer(1);
            white.put(0, (byte) 255);
            mask.upload2D(GLTexture.Format.R8, 1, 1, GL.NEAREST, white);
            float[] pv = new float[6];
            WcsSolution wcs = new WcsSolution(WcsSolution.Projection.TAN, pv, 215);
            ImageGeometry geometry = new ImageGeometry(TEXTURE_SIZE, TEXTURE_SIZE, wcs,
                    new Mat2(4.0 / TEXTURE_SIZE, 0, 0, -4.0 / TEXTURE_SIZE), new Vec2(TEXTURE_SIZE / 2.0, TEXTURE_SIZE / 2.0));
            for (int size : new int[]{128, 257, 512}) {
                GLRenderer.reshape(size, size);
                Viewport vp = Display.getViewport(0);
                GL.glViewport(0, 0, size, size);
                for (MapMode mode : MapMode.values()) {
                    MapScale scale = switch (mode) {
                        case Orthographic -> MapScale.ortho;
                        case HPC -> MapScale.hpc(0.6, 0.6);
                        case Latitudinal -> MapScale.lati;
                        case RadialWarp, RectWarp -> MapScale.boxCoxRadial(3, 0);
                    };
                    MapView view = MapView.create(Display.getCamera(), viewpoint, mode, GridType.Viewpoint, new MapScale[]{scale});
                    if (mode == MapMode.Orthographic)
                        Transform.ortho(1, 4, 0, 0, Quat.ZERO);
                    else
                        Transform.ortho2D(1, 1, 0, 0);
                    GLSLScreenShader.setView(view, vp);
                    for (String effect : new String[]{"gray", "inverted", "levels", "sharpen", "difference", "gamma", "mask", "blend"}) {
                        lut.upload2D(GLTexture.Format.RGBA8, 256, 1, GL.NEAREST,
                                effect.equals("inverted") ? LUT.gray().rgbaInv() : LUT.gray().rgba());
                        mask.bind();
                        difference.bind();
                        texture.bind();
                        boolean blend = effect.equals("blend");
                        GL.glClearColor(blend ? 0.2f : 0, blend ? 0.1f : 0, blend ? 0.3f : 0, 1);
                        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
                        float opacity = blend ? 0.5f : 1;
                        int diff = effect.equals("difference") ? 1 : 0;
                        GLSLImageShader.bindDisplay(new float[]{opacity, opacity, opacity, opacity},
                                1f / TEXTURE_SIZE, 1f / TEXTURE_SIZE, effect.equals("sharpen") ? -1 : 0, diff,
                                effect.equals("levels") ? -0.2f : 0, effect.equals("levels") ? 1.5f : 1,
                                effect.equals("gamma") ? 0.5f : 1, 1, 0, 0, 0, 0, 0, 0, Float.POSITIVE_INFINITY, 0,
                                effect.equals("mask") ? 0.5f : 0, 100, 0, 1, 0);
                        GLSLImageShader.render(mode, Quat.ZERO, centered(215), geometry, centered(215),
                                geometry, centered(215), Mat2.IDENTITY, 0, 0, false);
                        ByteBuffer pixels = BufferUtils.newByteBuffer(size * size * 4);
                        GL.glReadPixels(0, 0, size, size, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
                        GLException.checkErrors("ImageRenderingTest " + mode + " " + effect);
                        BufferedImage rendered = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                        int min = 255, max = 0;
                        for (int y = 0; y < size; y++) {
                            for (int x = 0; x < size; x++) {
                                int offset = 4 * (y * size + x);
                                int r = pixels.get(offset) & 255;
                                min = Math.min(min, r);
                                max = Math.max(max, r);
                                rendered.setRGB(x, size - y - 1, (r << 16) | ((pixels.get(offset + 1) & 255) << 8) | (pixels.get(offset + 2) & 255));
                            }
                        }
                        if (max - min < 20)
                            throw new AssertionError("Empty/flat image: " + mode + " " + effect);
                        if (args.length == 1) {
                            Path directory = Path.of(args[0]);
                            Files.createDirectories(directory);
                            if (!ImageIO.write(rendered, "png", directory.resolve(mode + "-" + size + "-" + effect + ".png").toFile()))
                                throw new AssertionError("PNG writer unavailable");
                        }
                    }
                }
            }
            sampling(texture, difference, lut, mask, viewpoint);
            fitsReference(texture, difference, lut, mask, viewpoint);
            layerPose(viewpoint.time);
            layerScales(renderer, viewpoint.time);
            layerFit(renderer, viewpoint.time);
        } finally {
            mask.delete();
            lut.delete();
            difference.delete();
            texture.delete();
            renderer.destroy();
        }
        System.out.println("ImageRenderingTest passed");
    }

    private static void layerPose(JHVTime time) throws Exception {
        GLRenderer.reshape(257, 193);
        Viewport vp = Display.getViewport(0);
        GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        // Distinct metadata distances/orientations must not define a surface map's display pose.
        for (String[] projections : new String[][]{{"CAR", "CEA"}, {"CEA", "CAR"}, {"TAN", "CEA"}, {"CAR", "TAN"}}) {
            FitsMetaData current = poseMetadata(projections[0], 215, -.1, .15, "2026-01-15T12:00:00");
            FitsMetaData previous = poseMetadata(projections[1], 4, .2, -.1, "2026-01-15T11:59:00");
            DecodedImage image = ramp(current.getGeometry(), 3, 5, 1);
            DecodedImage prior = ramp(previous.getGeometry(), 3, 5, 2);
            ImageLayer layer = ImageLayer.create();
            try {
                layer.init();
                layer.getDisplaySettings().setBlend(1);
                layer.handleData(new ImageData(prior, new Frames.Frame(previous.getViewpoint().time,
                        previous, Frames.EMPTY_METAXML, null, 0), previous.getViewpoint()));
                layer.handleData(new ImageData(image, new Frames.Frame(current.getViewpoint().time,
                        current, Frames.EMPTY_METAXML, null, 1), current.getViewpoint()));
                layer.setVisible(0);
                for (double distance : new double[]{10, 215}) {
                    Position displayed = new Position(time, distance, .35, .2);
                    DisplayController.setViewpointUpdate(_ -> displayed, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
                    for (double drag : new double[]{0, .6}) {
                        for (MapMode mode : MapMode.values()) {
                            Display.mode = mode;
                            double width = mode == MapMode.Orthographic ? 2.4 : 1;
                            DisplayController.cameraFromJson(new JSONObject()
                                    .put("fov", 2 * Math.atan(width / (2 * distance)))
                                    .put("dragRotation", Quat.createAxisY(drag).toJson()));
                            double halfAngle = Math.toDegrees(Math.asin(1 / distance)) * 1.25;
                            MapScale scale = switch (mode) {
                                case Orthographic -> MapScale.ortho;
                                case HPC -> MapScale.hpc(halfAngle, halfAngle);
                                case Latitudinal -> MapScale.lati;
                                case RadialWarp, RectWarp -> MapScale.boxCoxRadial(1.25, 0);
                            };
                            MapView view = MapView.create(Display.getCamera(), displayed, mode, GridType.Viewpoint, new MapScale[]{scale});
                            if (mode == MapMode.Orthographic)
                                Transform.ortho(vp.aspect, view.cameraWidth(vp), 0, 0, view.viewRotation());
                            else
                                Transform.ortho2D(vp.aspect, view.cameraWidth(vp), 0, 0);
                            GLSLScreenShader.setView(view, vp);
                            for (boolean diff : new boolean[]{false, true}) {
                                layer.getDisplaySettings().setDifferenceMode(diff
                                        ? ImageDisplaySettings.DifferenceMode.Running
                                        : ImageDisplaySettings.DifferenceMode.None);
                                layer.prerender();
                                GL.glClearColor(0, 0, 0, 1);
                                GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
                                layer.render(view, vp);
                                ByteBuffer pixels = BufferUtils.newByteBuffer(vp.width * vp.height * 4);
                                GL.glReadPixels(0, 0, vp.width, vp.height, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
                                // RadialWarp discards its exact origin; keep CEA probes inside decoded sample centers.
                                for (int[] offset : new int[][]{{mode == MapMode.RadialWarp ? 12 : 0, 0}, {28, 14}, {-30, -18},
                                        {100, mode == MapMode.Latitudinal ? 50 : 80}}) {
                                    int x = vp.width / 2 + offset[0], y = vp.height / 2 + offset[1];
                                    Vec3 ray = poseRay(mode, offset[0], offset[1], vp, displayed);
                                    Vec3 world = poseWorld(mode, offset[0], offset[1], vp, displayed, drag);
                                    // Integer mouse coordinates differ by half a pixel from fragment sample centers.
                                    Vec3 expectedPick = poseWorld(mode, offset[0] - .5, offset[1] + .5, vp, displayed, drag);
                                    Vec3 picked = view.mouseToSurface(vp, vp.x + x, vp.yAWT + vp.height - 1 - y);
                                    if ((picked == null) != (expectedPick == null) || (picked != null
                                            && Math.hypot(Math.hypot(picked.x - expectedPick.x, picked.y - expectedPick.y), picked.z - expectedPick.z) > 2e-11))
                                        throw new AssertionError("Independent layer picking: " + mode + " D=" + distance + " drag=" + drag);
                                    if (!diff && projections[0].equals("TAN") && world == null)
                                        continue; // Observer off-limb plane geometry is outside this sphere-pose check.
                                    double value = poseValue(current, world, ray, mode);
                                    double other = poseValue(previous, world, ray, mode);
                                    boolean valid = Double.isFinite(value) && (!diff || Double.isFinite(other));
                                    int expected = valid ? (int) Math.floor(256 * (diff ? .5 + 2.5 * (value - other) : value)) : 0;
                                    int actual = pixels.get(4 * (y * vp.width + x)) & 255;
                                    if ((!valid && actual != 0) || (valid && Math.abs(actual - Math.clamp(expected, 0, 255)) > 2))
                                        throw new AssertionError("Layer pose " + projections[0] + "/" + projections[1] + " " + mode
                                                + " D=" + distance + " drag=" + drag + " diff=" + diff + " at " + x + "," + y + ": " + actual + " vs " + expected);
                                }
                            }
                        }
                    }
                }
            } finally {
                Layers.remove(layer);
                layer.dispose();
                prior.release();
                image.release();
            }
        }
        if (GLException.checkErrors("ImageRenderingTest layer pose"))
            throw new AssertionError("Layer pose rendering failed");
        System.out.println("PASS: actual layer pose bindings, independent surface/observer pixels and displayed-view picking in all modes");
    }

    private static void layerScales(AngleRenderer renderer, JHVTime time) throws Exception {
        // Keep the real renderer and scale construction, without grid/text overlays on the pixel probes.
        for (Layer layer : Layers.getLayers())
            layer.setEnabled(false);
        for (String projection : new String[]{"CAR", "CEA"}) {
            FitsMetaData surfaceMeta = poseMetadata(projection, 215, -.1, .15, "2026-01-15T12:00:00");
            FitsMetaData observerMeta = poseMetadata("TAN", 4, .2, -.1, "2026-01-15T11:59:00");
            DecodedImage surfaceImage = ramp(surfaceMeta.getGeometry(), 3, 5, 1);
            DecodedImage observerImage = ramp(observerMeta.getGeometry(), 3, 5, 2);
            ImageLayer surface = ImageLayer.create(), observer = ImageLayer.create();
            try {
                surface.getDisplaySettings().setBlend(1);
                observer.getDisplaySettings().setBlend(1);
                surface.handleData(new ImageData(surfaceImage, new Frames.Frame(surfaceMeta.getViewpoint().time,
                        surfaceMeta, Frames.EMPTY_METAXML, null, 0), surfaceMeta.getViewpoint()));
                observer.handleData(new ImageData(observerImage, new Frames.Frame(observerMeta.getViewpoint().time,
                        observerMeta, Frames.EMPTY_METAXML, null, 0), observerMeta.getViewpoint()));
                surface.setEnabled(true);
                for (boolean mixed : new boolean[]{false, true}) {
                    observer.setEnabled(mixed);
                    for (int[] size : new int[][]{{257, 193}, {193, 257}}) {
                        GLRenderer.reshape(size[0], size[1]);
                        Viewport vp = Display.getViewport(0);
                        for (double distance : new double[]{2, 10, 215}) {
                            Position displayed = new Position(time, distance, .35, .2);
                            DisplayController.setViewpointUpdate(_ -> displayed, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
                            Display.resetViewportZoom();
                            DisplayController.cameraFromJson(new JSONObject().put("fov", 2 * Math.atan(1 / (2 * distance)))
                                    .put("dragRotation", Quat.ZERO.toJson()));
                            // A unit-sphere tangent point satisfies v·(observer-v)=0.
                            double tangentZ = 1 / distance, tangentX = Math.sqrt(1 - tangentZ * tangentZ);
                            double limbRadius = tangentX / (1 - tangentZ / distance);
                            double surfaceAngle = Math.toDegrees(Math.atan2(tangentX, distance - tangentZ));
                            // The declared TAN image covers plane coordinates [-.5,.5] on each axis.
                            double observerAngle = Math.toDegrees(Math.atan(.5));
                            double halfAngle = mixed ? Math.max(surfaceAngle, observerAngle) : surfaceAngle;
                            double halfHeight = Math.max(halfAngle, halfAngle / vp.aspect);
                            Display.mode = MapMode.HPC;
                            renderer.render(displayed, false);
                            MapView view = GLRenderer.getMapView();
                            scaleNear(view.scale(vp).toMapY(1), halfHeight, "HPC vertical scale");
                            scaleNear(view.scale(vp).toMapX(1), halfHeight * vp.aspect, "HPC horizontal scale");
                            Layers.setActiveImageLayer(surface);
                            DisplayController.zoomOneToOne();
                            scaleNear(view.cameraWidth(vp), vp.height * surfaceAngle / (128 * halfHeight), "surface one-to-one zoom");
                            if (mixed) {
                                Layers.setActiveImageLayer(observer);
                                DisplayController.zoomOneToOne();
                                scaleNear(view.cameraWidth(vp), vp.height * observerAngle / (128 * halfHeight), "observer one-to-one zoom");
                            }
                            for (MapMode mode : new MapMode[]{MapMode.RadialWarp, MapMode.RectWarp}) {
                                Display.mode = mode;
                                // The observer's fixed source distance is 4, independent of the display distance.
                                double outer = mixed ? Math.max(limbRadius, 4 * Math.sqrt(.5)) : limbRadius;
                                renderer.render(displayed, false);
                                view = GLRenderer.getMapView();
                                scaleNear(view.scale(vp).toMapY(1), outer, mode + " outer scale");
                                if (!mixed && distance == 2) {
                                    // A visible point with z=.75 projects outside radius 1 at D=2.
                                    double planeRadius = 2 * Math.sqrt(1 - .75 * .75) / (2 - .75);
                                    double t = 1 / outer + (1 - 1 / outer) * Math.log(planeRadius) / Math.log(outer);
                                    int x, y;
                                    if (mode == MapMode.RectWarp) {
                                        x = (int) Math.round(.125 * vp.width - .5);
                                        y = (int) Math.round(t * vp.height - .5);
                                    } else {
                                        x = (int) Math.round(vp.width / 2. - .5 * t / Math.sqrt(2) * vp.height / 1.1 - .5);
                                        y = (int) Math.round(vp.height / 2. + .5 * t / Math.sqrt(2) * vp.height / 1.1 - .5);
                                    }
                                    double theta, unitRadius;
                                    if (mode == MapMode.RectWarp) {
                                        theta = 2 * Math.PI * (x + .5) / vp.width;
                                        unitRadius = (y + .5) / vp.height;
                                    } else {
                                        double px = 1.1 * (x + .5 - vp.width / 2.) / vp.height;
                                        double py = 1.1 * (y + .5 - vp.height / 2.) / vp.height;
                                        theta = Math.atan2(-px, py);
                                        unitRadius = 2 * Math.hypot(px, py);
                                    }
                                    double radius = Math.pow(outer, (unitRadius - 1 / outer) / (1 - 1 / outer));
                                    if (!(radius > 1 && radius < limbRadius))
                                        throw new AssertionError("Annulus probe missed the visible surface: " + mode);
                                    Vec3 ray = new Vec3(-radius * Math.sin(theta), radius * Math.cos(theta), -distance);
                                    double length = Math.sqrt(radius * radius + distance * distance);
                                    double impact = distance * radius / length;
                                    double travel = distance * distance / length - Math.sqrt(1 - impact * impact);
                                    Vec3 point = new Vec3(travel * ray.x / length, travel * ray.y / length, distance + travel * ray.z / length);
                                    Vec3 world = rotateY(rotateX(point, -displayed.lat), -displayed.lon);
                                    int expected = (int) Math.floor(256 * poseValue(surfaceMeta, world, ray, mode));
                                    ByteBuffer pixel = BufferUtils.newByteBuffer(4);
                                    GL.glReadPixels(x, y, 1, 1, GL.RGBA, GL.UNSIGNED_BYTE, pixel);
                                    if (Math.abs((pixel.get(0) & 255) - expected) > 2)
                                        throw new AssertionError("Visible surface annulus: " + projection + " " + mode + ": " + (pixel.get(0) & 255) + " vs " + expected);
                                }
                            }
                        }
                    }
                }
            } finally {
                Layers.remove(observer);
                Layers.remove(surface);
                observer.dispose();
                surface.dispose();
                observerImage.release();
                surfaceImage.release();
            }
        }
        if (GLException.checkErrors("ImageRenderingTest layer scales"))
            throw new AssertionError("Layer scale rendering failed");
        System.out.println("PASS: tangent-based actual HPC/warp scales, mixed source distances, one-to-one zoom and rendered surface annulus");
    }

    private static void layerFit(AngleRenderer renderer, JHVTime time) throws Exception {
        for (Layer layer : Layers.getLayers())
            layer.setEnabled(false);
        Display.mode = MapMode.Orthographic;
        for (String projection : new String[]{"CAR", "CEA"}) {
            for (double coverage : new double[]{1, .25}) {
                FitsMetaData surfaceMeta = poseMetadata(projection, 215, -.1, .15, "2026-01-15T12:00:00", coverage);
                for (double observerSpan : new double[]{.2, 1}) {
                    FitsMetaData observerMeta = poseMetadata("TAN", 4, .2, -.1, "2026-01-15T11:59:00", observerSpan);
                    DecodedImage surfaceImage = ramp(surfaceMeta.getGeometry(), 3, 5, 1);
                    DecodedImage observerImage = ramp(observerMeta.getGeometry(), 3, 5, 2);
                    ImageLayer surface = ImageLayer.create(), observer = ImageLayer.create();
                    try {
                        surface.getDisplaySettings().setBlend(1);
                        observer.getDisplaySettings().setBlend(1);
                        surface.handleData(new ImageData(surfaceImage, new Frames.Frame(surfaceMeta.getViewpoint().time,
                                surfaceMeta, Frames.EMPTY_METAXML, null, 0), surfaceMeta.getViewpoint()));
                        observer.handleData(new ImageData(observerImage, new Frames.Frame(observerMeta.getViewpoint().time,
                                observerMeta, Frames.EMPTY_METAXML, null, 0), observerMeta.getViewpoint()));
                        for (int enabled = 0; enabled < 4; enabled++) {
                            surface.setEnabled((enabled & 1) != 0);
                            observer.setEnabled((enabled & 2) != 0);
                            // Unit sphere: y spans [-1,1], regardless of the map's angular coverage.
                            // Observer plane: declared angular span / angular solar radius at source D=4.
                            double height = Math.max(surface.isEnabled() ? 2 : 0,
                                    observer.isEnabled() ? observerSpan / Math.atan(1. / 4) : 0);
                            for (int[] size : new int[][]{{257, 193}, {193, 257}}) {
                                GLRenderer.reshape(size[0], size[1]);
                                Viewport vp = Display.getViewport(0);
                                for (double distance : new double[]{10, 215}) {
                                    Position displayed = new Position(time, distance, .35, .2);
                                    DisplayController.setViewpointUpdate(_ -> displayed, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
                                    for (boolean reset : new boolean[]{false, true}) {
                                        DisplayController.cameraFromJson(new JSONObject().put("fov", .2)
                                                .put("translation", new JSONArray(new double[]{.15, -.1}))
                                                .put("dragRotation", Quat.createAxisY(.6).toJson()));
                                        vp.zoom = .7;
                                        renderer.render(displayed, false);
                                        if (reset)
                                            DisplayController.resetCamera();
                                        else
                                            DisplayController.zoomFit();
                                        renderer.render(displayed, false);
                                        MapView view = GLRenderer.getMapView();
                                        String context = projection + " coverage=" + coverage + " enabled=" + enabled + " reset=" + reset;
                                        double expectedHeight = height == 0 ? 2 * distance * Math.tan(Math.toRadians(.5)) : height;
                                        scaleNear(vp.zoom, reset ? .7 : 1, context + " viewport zoom");
                                        scaleNear(view.cameraWidth(vp) / vp.zoom, expectedHeight, context + " camera height");
                                        scaleNear(view.cameraTranslationX(), reset ? 0 : .15, context + " translation X");
                                        scaleNear(view.cameraTranslationY(), reset ? 0 : -.1, context + " translation Y");
                                        scaleNear(view.dragRotation().y, reset ? 0 : Math.sin(.3), context + " drag rotation");
                                        if (coverage == 1 && enabled == 1) {
                                            // Read actual fitted pixels inside/outside the unit sphere.
                                            double width = 2 * (reset ? .7 : 1), tx = reset ? 0 : .15, ty = reset ? 0 : -.1;
                                            for (Vec2 probe : new Vec2[]{new Vec2(0, .5), new Vec2(1.05, 0)}) {
                                                int x = (int) Math.round(vp.width / 2. + (probe.x + tx) * vp.height / width - .5);
                                                int y = (int) Math.round(vp.height / 2. + (probe.y + ty) * vp.height / width - .5);
                                                if (x < 0 || x >= vp.width || y < 0 || y >= vp.height)
                                                    continue;
                                                double px = (x + .5 - vp.width / 2.) * width / vp.height - tx;
                                                double py = (y + .5 - vp.height / 2.) * width / vp.height - ty;
                                                double radius2 = px * px + py * py;
                                                int expected = 0;
                                                if (radius2 < 1) {
                                                    Vec3 point = new Vec3(px, py, Math.sqrt(1 - radius2));
                                                    Vec3 world = rotateY(rotateX(rotateY(point, reset ? 0 : -.6), -displayed.lat), -displayed.lon);
                                                    expected = (int) Math.floor(256 * poseValue(surfaceMeta, world, Vec3.ZERO, MapMode.Orthographic));
                                                }
                                                ByteBuffer pixel = BufferUtils.newByteBuffer(4);
                                                GL.glReadPixels(x, y, 1, 1, GL.RGBA, GL.UNSIGNED_BYTE, pixel);
                                                if (Math.abs((pixel.get(0) & 255) - expected) > 2)
                                                    throw new AssertionError(context + " fitted surface pixel: " + (pixel.get(0) & 255) + " vs " + expected);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        Layers.remove(observer);
                        Layers.remove(surface);
                        observer.dispose();
                        surface.dispose();
                        observerImage.release();
                        surfaceImage.release();
                    }
                }
            }
        }
        if (GLException.checkErrors("ImageRenderingTest layer fit"))
            throw new AssertionError("Layer fit rendering failed");
        System.out.println("PASS: actual zoom-fit/reset with full/partial CAR/CEA, mixed observer heights, disabled/empty layers and fitted surface pixels");
    }

    private static void scaleNear(double actual, double expected, String context) {
        if (!(Math.abs(actual - expected) < 2e-12))
            throw new AssertionError(context + ": " + actual + " vs " + expected);
    }

    private static Vec3 poseRay(MapMode mode, double x, double y, Viewport vp, Position displayed) {
        double mx = .5 + x / vp.width, my = .5 + y / vp.height;
        return switch (mode) {
            case HPC -> {
                double halfAngle = Math.asin(1 / displayed.distance) * 1.25;
                double lon = halfAngle * (2 * mx - 1), lat = halfAngle * (2 * my - 1);
                yield new Vec3(Math.sin(lon) * Math.cos(lat), Math.sin(lat), -Math.cos(lon) * Math.cos(lat));
            }
            case RadialWarp -> new Vec3(2.5 * 1.1 * x / vp.height, 2.5 * 1.1 * y / vp.height, -displayed.distance);
            case RectWarp -> new Vec3(-1.25 * my * Math.sin(2 * Math.PI * mx), 1.25 * my * Math.cos(2 * Math.PI * mx), -displayed.distance);
            default -> Vec3.ZERO;
        };
    }

    private static Vec3 poseWorld(MapMode mode, double x, double y, Viewport vp, Position displayed, double drag) {
        Vec3 point;
        if (mode == MapMode.Latitudinal) {
            double lon = 2 * Math.PI * x / vp.width - displayed.lon;
            double lat = Math.PI * y / vp.height + displayed.lat;
            return Math.abs(lat) > Math.PI / 2 ? null
                    : new Vec3(Math.sin(lon) * Math.cos(lat), Math.sin(lat), Math.cos(lon) * Math.cos(lat));
        }
        if (mode == MapMode.Orthographic) {
            double vx = 2.4 * x / vp.height, vy = 2.4 * y / vp.height;
            if (vx * vx + vy * vy > 1)
                return null;
            point = new Vec3(vx, vy, Math.sqrt(1 - vx * vx - vy * vy));
        } else {
            Vec3 ray = poseRay(mode, x, y, vp, displayed);
            // Independent closest-approach construction of the near sphere hit.
            double length = Math.sqrt(ray.x * ray.x + ray.y * ray.y + ray.z * ray.z);
            double impact = displayed.distance * Math.hypot(ray.x, ray.y) / length;
            if (impact > 1)
                return null;
            double travel = -displayed.distance * ray.z / length - Math.sqrt(1 - impact * impact);
            point = new Vec3(travel * ray.x / length, travel * ray.y / length, displayed.distance + travel * ray.z / length);
        }
        // Explicit axis rotations, independent of Position.toQuat() and shader quaternion helpers.
        return rotateY(rotateX(rotateY(point, mode == MapMode.Orthographic ? -drag : 0), -displayed.lat), -displayed.lon);
    }

    private static FitsMetaData poseMetadata(String projection, double distance, double longitude, double latitude, String date) throws Exception {
        return poseMetadata(projection, distance, longitude, latitude, date, 1);
    }

    private static FitsMetaData poseMetadata(String projection, double distance, double longitude, double latitude, String date, double coverage) throws Exception {
        int width = 256, height = 128;
        double spanX = projection.equals("TAN") ? Math.toDegrees(1) : 360;
        double spanY = projection.equals("CAR") ? 180 : Math.toDegrees(projection.equals("CEA") ? 4 : 1);
        String axes = projection.equals("TAN") ? "HPL" : "CRL";
        String xml = "<meta><DATE-OBS>" + date + "</DATE-OBS><NAXIS1>" + width + "</NAXIS1><NAXIS2>" + height
                + "</NAXIS2><CTYPE1>" + axes + "N-" + projection + "</CTYPE1><CTYPE2>" + axes + "T-" + projection
                + "</CTYPE2><CUNIT1>deg</CUNIT1><CUNIT2>deg</CUNIT2><CRPIX1>" + (width + 1) / 2.
                + "</CRPIX1><CRPIX2>" + (height + 1) / 2. + "</CRPIX2><CDELT1>" + coverage * spanX / width
                + "</CDELT1><CDELT2>" + coverage * spanY / height + "</CDELT2><PV2_1>0.5</PV2_1><DSUN_OBS>"
                + distance * Sun.RadiusMeter + "</DSUN_OBS><CRLN_OBS>" + Math.toDegrees(-longitude)
                + "</CRLN_OBS><HGLT_OBS>" + Math.toDegrees(latitude) + "</HGLT_OBS></meta>";
        return new FitsMetaData(new XMLMetaDataContainer(xml), URI.create("test:layer-pose"));
    }

    private static double poseValue(FitsMetaData metadata, Vec3 world, Vec3 ray, MapMode mode) {
        if (world == null)
            return Double.NaN;
        double u, v;
        if (metadata.getGeometry().wcs.projection.isSurfaceMap()) {
            double longitude = Math.atan2(world.x, world.z);
            u = .5 + longitude / (2 * Math.PI);
            v = .5 - (metadata.getGeometry().wcs.projection == WcsSolution.Projection.CAR
                    ? Math.asin(world.y) / Math.PI : world.y / 2);
        } else {
            Position source = metadata.getViewpoint();
            Vec3 sourcePoint = rotateX(rotateY(world, source.lon), source.lat);
            if (mode == MapMode.Orthographic) {
                u = .5 + sourcePoint.x / metadata.getGeometry().wcs.unitsPerRad;
                v = .5 - sourcePoint.y / metadata.getGeometry().wcs.unitsPerRad;
            } else if (mode == MapMode.Latitudinal) {
                if (sourcePoint.z < 1 / source.distance)
                    return Double.NaN;
                u = .5 + sourcePoint.x / (source.distance - sourcePoint.z);
                v = .5 - sourcePoint.y / (source.distance - sourcePoint.z);
            } else {
                double denominator = mode == MapMode.HPC ? -ray.z : source.distance;
                u = .5 + ray.x / denominator;
                v = .5 - ray.y / denominator;
            }
        }
        return u < 0 || u > 1 || v < 0 || v > 1 ? Double.NaN : .1 + .35 * u + .3 * v;
    }

    private static Vec3 rotateX(Vec3 point, double angle) {
        return new Vec3(point.x, Math.cos(angle) * point.y - Math.sin(angle) * point.z,
                Math.sin(angle) * point.y + Math.cos(angle) * point.z);
    }

    private static Vec3 rotateY(Vec3 point, double angle) {
        return new Vec3(Math.cos(angle) * point.x + Math.sin(angle) * point.z, point.y,
                -Math.sin(angle) * point.x + Math.cos(angle) * point.z);
    }

    private static void rayIntersections(JHVTime time) {
        // Known sphere points give a geometric reference independent of the quadratic formula.
        // Include a distant observer to check that accuracy does not degrade with distance.
        for (double distance : new double[]{10, 215, 1e8}) {
            for (double longitude : new double[]{0, .7}) {
                Position observer = new Position(time, distance, longitude, .2);
                for (Vec2 xy : new Vec2[]{Vec2.ZERO, new Vec2(.3, .4), new Vec2(-.8, -.2), new Vec2(.99, 0)}) {
                    Vec3 view = new Vec3(xy.x, xy.y, Math.sqrt(1 - xy.x * xy.x - xy.y * xy.y));
                    double zeta = distance - view.z;
                    double phi = Math.atan2(view.x, zeta);
                    double theta = Math.atan2(view.y, Math.hypot(view.x, zeta));
                    Vec3 expected = observer.toQuat().rotateInverseVector(view);
                    Vec3 actual = ObserverGeometry.helioprojectiveToWorld(observer, phi, theta);
                    if (actual == null || !(Math.hypot(Math.hypot(actual.x - expected.x, actual.y - expected.y), actual.z - expected.z) <= 2e-12))
                        throw new AssertionError("Wrong ray/sphere intersection: distance " + distance + ", point " + view + ": " + actual);
                    for (double scale : new double[]{.01, 1, 100}) {
                        Vec3 ray = new Vec3(scale * view.x, scale * view.y, -scale * zeta);
                        Vec3 direct = ObserverGeometry.observerRayToWorld(observer, ray);
                        if (direct == null || !(Math.hypot(Math.hypot(direct.x - expected.x, direct.y - expected.y), direct.z - expected.z) <= 2e-12))
                            throw new AssertionError("Wrong directed ray intersection: distance " + distance + ", scale " + scale + ": " + direct);
                        if (ObserverGeometry.observerRayToWorld(observer, new Vec3(-ray.x, -ray.y, -ray.z)) != null)
                            throw new AssertionError("Reversed ray hit the sphere: distance " + distance);
                    }
                }
                for (double backward : new double[]{Math.toRadians(178), Math.PI, -Math.PI}) {
                    if (ObserverGeometry.helioprojectiveToWorld(observer, backward, 0) != null)
                        throw new AssertionError("Backward ray hit the sphere: distance " + distance);
                }
                double offDisk = Math.asin(1 / distance) * 1.001;
                if (ObserverGeometry.helioprojectiveToWorld(observer, offDisk, 0) != null ||
                        ObserverGeometry.helioprojectiveToWorld(observer, 0, offDisk) != null)
                    throw new AssertionError("Off-disk ray hit the sphere: distance " + distance);
                double miss = distance / Math.sqrt(distance * distance - 1) * 1.001;
                if (ObserverGeometry.observerRayToWorld(observer, new Vec3(miss, 0, -distance)) != null ||
                        ObserverGeometry.observerRayToWorld(observer, new Vec3(0, miss, -distance)) != null)
                    throw new AssertionError("Off-disk planar ray hit the sphere: distance " + distance);
            }
        }
        System.out.println("PASS: Java ray/sphere intersections preserve surface points, viewpoint rotation and forward/off-disk/backward classification");
    }

    private static void fitsReference(GLStreamingTexture2D texture, GLStreamingTexture2D difference,
                                      GLTexture lut, GLTexture mask, Position viewpoint) throws Exception {
        byte[][] pixels = new byte[96][128];
        int[] counts = new int[256];
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 128; x++) {
                // FITS centers are (x+1,y+1); the reference is (64.25,24.25).
                int value = 32 + (int) Math.hypot(x + 1 - 64.25, y + 1 - 24.25);
                pixels[y][x] = (byte) value;
                counts[value]++;
            }
        }
        Path path = Files.createTempFile("jhv-offcenter-", ".fits");
        try {
            try (Fits fits = new Fits()) {
                ImageHDU hdu = (ImageHDU) Fits.makeHDU(pixels);
                Header header = hdu.getHeader();
                header.addValue("DATE-OBS", "2026-01-15T12:00:00", null);
                header.addValue("CRPIX1", 64.25, null);
                header.addValue("CRPIX2", 24.25, null);
                header.addValue("CDELT1", 100, null);
                header.addValue("CDELT2", 100, null);
                fits.addHDU(hdu);
                fits.write(path.toFile());
            }
            FITSSource source = new FITSSource(path.toFile());
            FitsMetaData metadata = new FitsMetaData(new XMLMetaDataContainer(source.xml(0)), path.toUri());
            if (metadata.getOuterRadius() != Float.POSITIVE_INFINITY)
                throw new AssertionError("Unmasked FITS image must have an unlimited outer radius");
            ImageGeometry geometry = metadata.getGeometry();
            DecodedImage image = new DecodedImage(source.decode(0, 0, ImageFilter.NONE, null, null), geometry);
            try {
                ByteBuffer decoded = (ByteBuffer) image.imageBuffer().buffer;
                for (int y = 0; y < 96; y++)
                    for (int x = 0; x < 128; x++)
                        if (decoded.get(y * 128 + x) != pixels[95 - y][x])
                            throw new AssertionError("FITS buffer row order changed");
                int size = 257, center = size / 2;
                GLRenderer.reshape(size, size);
                GL.glViewport(0, 0, size, size);
                MapView view = MapView.create(Display.getCamera(), new Position(viewpoint.time, 215, 0, 0),
                        MapMode.Orthographic, GridType.Viewpoint, new MapScale[]{MapScale.ortho});
                Transform.ortho(1, size * geometry.pixelSizeX, 0, 0, Quat.ZERO);
                GLSLScreenShader.setView(view, Display.getViewport(0));
                lut.upload2D(GLTexture.Format.RGBA8, 256, 1, GL.NEAREST, LUT.gray().rgba());
                ByteBuffer white = BufferUtils.newByteBuffer(1);
                white.put(0, (byte) 255);
                mask.upload2D(GLTexture.Format.R8, 1, 1, GL.NEAREST, white);
                ByteBuffer rendered = renderSampling(texture, difference, mask, image, image,
                        MapMode.Orthographic, 0, size, Mat2.IDENTITY);
                for (int[] sample : new int[][]{{0, 0, 32}, {16, 0, 48}, {0, 16, 48}}) {
                    int actual = rendered.get(4 * ((center + sample[1]) * size + center + sample[0])) & 255;
                    // Integer rings, interpolation and the shader's one-byte dither.
                    if (Math.abs(actual - sample[2]) > 2)
                        throw new AssertionError("Off-center FITS reference rendered " + actual + ", expected " + sample[2]);
                }
            } finally {
                image.release();
            }
            DecodedImage filtered = new DecodedImage(source.decode(0, 0, ImageFilter.of(ImageFilter.Type.RHEF, geometry), null, null), geometry);
            try {
                ShortBuffer decoded = (ShortBuffer) filtered.imageBuffer().buffer;
                for (int y = 0; y < 96; y++) {
                    for (int x = 0; x < 128; x++) {
                        int value = pixels[95 - y][x] & 255;
                        short expected = Float.floatToFloat16(counts[value] >= 5 ? .5f : value / 255f);
                        if (decoded.get(y * 128 + x) != expected)
                            throw new AssertionError("RHEF annulus is not centered on the FITS reference pixel at " + x + "," + y);
                    }
                }
            } finally {
                filtered.release();
            }
        } finally {
            Files.deleteIfExists(path);
        }
        System.out.println("PASS: off-center FITS reference through row reversal, rendering and RHEF annuli");
    }

    private static void sampling(GLStreamingTexture2D texture, GLStreamingTexture2D difference,
                                 GLTexture lut, GLTexture mask, Position viewpoint) throws Exception {
        int size = 257;
        Position centeredView = new Position(viewpoint.time, 215, 0, 0);
        GLRenderer.reshape(size, size);
        GL.glViewport(0, 0, size, size);
        warpPicking(viewpoint.time);
        lut.upload2D(GLTexture.Format.RGBA8, 256, 1, GL.NEAREST, LUT.gray().rgba());
        // Keep samples away from decoder boundary extension. The asymmetric detector
        // mask and slit must cover exactly the same screen pixels at every level.
        ByteBuffer detector = BufferUtils.newByteBuffer(8 * 8);
        for (int y = 3; y < 5; y++)
            for (int x = 2; x < 6; x++)
                detector.put(y * 8 + x, (byte) 255);
        mask.upload2D(GLTexture.Format.R8, 8, 8, GL.NEAREST, detector);
        float[] pv = new float[6];
        WcsSolution wcs = new WcsSolution(WcsSolution.Projection.TAN, pv, 215);
        for (int[] geometry : new int[][]{{128, 96, 0, 0}, {129, 97, 0, 0}, {129, 97, 3, 5}}) {
            ImageGeometry full = new ImageGeometry(geometry[0], geometry[1], wcs,
                    new Mat2(4.0 / geometry[0], 0, 0, -4.0 / geometry[1]), new Vec2(geometry[0] / 2.0, geometry[1] / 2.0));
            DecodedImage reference = ramp(full, geometry[2], geometry[3], 0);
            try {
                for (MapMode mode : MapMode.values()) {
                    MapScale scale = switch (mode) {
                        case Orthographic -> MapScale.ortho;
                        case HPC -> MapScale.hpc(0.6, 0.6);
                        case Latitudinal -> MapScale.lati;
                        case RadialWarp, RectWarp -> MapScale.boxCoxRadial(3, 0);
                    };
                    MapView view = MapView.create(Display.getCamera(), centeredView, mode, GridType.Viewpoint, new MapScale[]{scale});
                    if (mode == MapMode.Orthographic)
                        Transform.ortho(1, 4, 0, 0, Quat.ZERO);
                    else
                        Transform.ortho2D(1, 1, 0, 0);
                    GLSLScreenShader.setView(view, Display.getViewport(0));
                    for (int effect = 0; effect < 4; effect++) {
                        ByteBuffer expected = renderSampling(texture, difference, mask, reference, reference, mode, effect, size, Mat2.IDENTITY);
                        for (int level : new int[]{1, 3}) {
                            DecodedImage image = ramp(full, geometry[2], geometry[3], level);
                            DecodedImage prior = ramp(full, geometry[2], geometry[3], level + 1);
                            try {
                                ByteBuffer actual = renderSampling(texture, difference, mask, image, prior, mode, effect, size, Mat2.IDENTITY);
                                int visible = 0;
                                for (int i = 0; i < actual.capacity(); i += 4) {
                                    int a = actual.get(i) & 255, e = expected.get(i) & 255;
                                    if ((a == 0) != (e == 0) || Math.abs(a - e) > 2)
                                        throw new AssertionError("Sampling/mask/slit mismatch: " + mode + " effect " + effect
                                                + " grid " + full.grid + " origin " + geometry[2] + "," + geometry[3] + " level " + level + " pixel " + i / 4 + ": " + a + " vs " + e);
                                    if (e != 0) visible++;
                                }
                                if (visible < 20)
                                    throw new AssertionError("Sampling test rendered no useful pixels: " + mode);
                            } finally {
                                prior.release();
                                image.release();
                            }
                        }
                    }
                }
            } finally {
                reference.release();
            }
        }
        transforms(texture, difference, mask, wcs, centeredView, size);
        sharpening(texture, difference, mask, wcs, centeredView, size);
        wrapping(texture, difference, mask, centeredView, size);
        orthographicTan(texture, difference, mask, centeredView, size);
        orthoComparison(texture, difference, mask, centeredView, size);
        clipping(texture, difference, lut, mask, wcs, centeredView, size);
        System.out.println("PASS: reduced-grid alignment, fixed detector masks and slits, CAR/CEA wrapping, and nonlinear sharpening against a CPU reference");
    }

    private static void clipping(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture lut, GLTexture mask,
                                 WcsSolution wcs, Position viewpoint, int size) {
        short[] pixels = new short[64 * 64];
        Arrays.fill(pixels, Float.floatToFloat16(.5f));
        ImageBuffer image = ImageBuffer.fromShorts(64, 64, pixels, ImageFilter.NONE);
        DecodedImage decoded = new DecodedImage(image, new ImageGeometry(64, 64, wcs,
                new Mat2(16. / 64, 0, 0, -16. / 64), new Vec2(32, 32)));
        try {
            texture.upload(image);
            difference.upload(image);
            lut.upload2D(GLTexture.Format.RGBA8, 256, 1, GL.NEAREST, LUT.gray().rgba());
            mask.upload2D(GLTexture.Format.R8, 1, 1, GL.NEAREST, BufferUtils.newByteBuffer(1).put(0, (byte) 255));
            mask.bind();
            difference.bind();
            texture.bind();
            for (MapMode mode : MapMode.values()) {
                MapScale scale = switch (mode) {
                    case Orthographic -> MapScale.ortho;
                    case HPC -> MapScale.hpc(.6, .6);
                    case Latitudinal -> MapScale.lati;
                    case RadialWarp, RectWarp -> MapScale.boxCoxRadial(3, 0);
                };
                MapView view = MapView.create(Display.getCamera(), viewpoint, mode, GridType.Viewpoint, new MapScale[]{scale});
                if (mode == MapMode.Orthographic)
                    Transform.ortho(1, 4, 0, 0, Quat.ZERO);
                else
                    Transform.ortho2D(1, 1, 0, 0);
                GLSLScreenShader.setView(view, Display.getViewport(0));
                int x = switch (mode) {
                    case Orthographic -> 224;
                    case HPC -> 51;
                    case Latitudinal -> 149;
                    case RadialWarp -> 192;
                    case RectWarp -> 192;
                };
                int y = mode == MapMode.RectWarp ? 170 : size / 2;
                double radius = switch (mode) {
                    case Orthographic -> 4 * ((x + .5) / size - .5);
                    case HPC -> 215 * Math.abs(Math.tan(Math.toRadians(scale.toMapX((x + .5) / size))));
                    case Latitudinal -> 1;
                    case RadialWarp -> scale.toMapY(2 * Math.abs((x + .5) / size - .5));
                    case RectWarp -> scale.toMapY((y + .5) / size);
                };
                // All chosen points have source X within [.375,.625]. HPC's
                // map X is .20: applying the source slit to it would reject it.
                for (int effect = 0; effect < 8; effect++) {
                    GLSLImageShader.bindDisplay(new float[]{1, 1, 1, 1}, 1f / 64, 1f / 64, 0, 0,
                            0, 1, 1, 1, mode == MapMode.HPC ? (float) Math.PI : 0,
                            effect == 3 ? (float) Math.PI / 4 : effect == 6 ? (float) Math.PI : 0,
                            effect == 7 ? (mode == MapMode.HPC ? 0 : (float) Math.PI) : 0, effect == 7 ? (float) Math.PI / 4 : 0,
                            1, 0, effect == 4 ? .1f : Float.POSITIVE_INFINITY, 0,
                            effect == 2 ? 2 : 0, effect == 1 ? .1f : 100, .375f, .625f, effect == 5 ? 1 : 0);
                    GL.glClearColor(0, 0, 0, 1);
                    GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
                    GLSLImageShader.render(mode, Quat.ZERO, centered(215), decoded.geometry(), centered(215),
                            decoded.geometry(), centered(215), Mat2.IDENTITY, 0, 0, false);
                    ByteBuffer pixel = BufferUtils.newByteBuffer(4);
                    GL.glReadPixels(x, y, 1, 1, GL.RGBA, GL.UNSIGNED_BYTE, pixel);
                    if (GLException.checkErrors("ImageRenderingTest clipping"))
                        throw new AssertionError("Clipping render failed: " + mode);
                    int value = pixel.get(0) & 255;
                    int expected = effect == 0 || effect == 7 ? 128 : effect == 5 ? (int) Math.round(128 * Math.max(1, radius)) : 0;
                    if (Math.abs(value - expected) > (expected == 0 ? 0 : 2))
                        throw new AssertionError("Source slit / masks / enhancement mismatch: " + mode + " effect " + effect + ": " + value);
                }
            }
            // A surface-map mask is in displayed observer coordinates, even
            // when its sampling point is in heliographic coordinates.
            for (WcsSolution.Projection projection : new WcsSolution.Projection[]{WcsSolution.Projection.CAR, WcsSolution.Projection.CEA}) {
                WcsSolution surfaceWcs = new WcsSolution(projection, new float[]{0, 1, 0, 0, 0, 0}, 1);
                ImageGeometry surface = new ImageGeometry(64, 64, surfaceWcs,
                        new Mat2(2 * Math.PI / 64, 0, 0, -(projection == WcsSolution.Projection.CAR ? Math.PI : 2) / 64), new Vec2(32, 32));
                for (double longitude : new double[]{0, Math.PI / 3}) {
                    Position displayed = new Position(viewpoint.time, 215, longitude, 0);
                    for (MapMode mode : new MapMode[]{MapMode.Orthographic, MapMode.Latitudinal}) {
                        MapView view = MapView.create(Display.getCamera(), displayed, mode, GridType.Viewpoint,
                                new MapScale[]{mode == MapMode.Orthographic ? MapScale.ortho : MapScale.lati});
                        if (mode == MapMode.Orthographic) Transform.ortho(1, 4, 0, 0, displayed.toQuat());
                        else Transform.ortho2D(1, 1, 0, 0);
                        GLSLScreenShader.setView(view, Display.getViewport(0));
                        for (int effect = 0; effect < 3; effect++) {
                            GLSLImageShader.bindDisplay(new float[]{1, 1, 1, 1}, 0, 0, 0, 0, 0, 1, 1, 1,
                                    0, 0, 0, 0, 0, 0, Float.POSITIVE_INFINITY, 0, effect == 1 ? .5f : 0, effect == 2 ? .5f : 100, 0, 1, 0);
                            GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
                            GLSLImageShader.render(mode, displayed.toQuat(), displayed, surface, displayed,
                                    surface, displayed, Mat2.IDENTITY, 0, 0, false);
                            assertPixel(size / 2, size / 2, effect == 1 ? 0 : 128, "Rotated surface mask: " + projection + " " + mode);
                        }
                    }
                }
            }
            // Exercise both sides of the Sun-plane horizon, with the same
            // outer radii used by Show corona, the user mask and unmasked metadata.
            WcsSolution arc = new WcsSolution(WcsSolution.Projection.ARC, new float[6], 1);
            ImageGeometry wide = new ImageGeometry(64, 64, arc, new Mat2(2 * Math.PI / 64, 0, 0, -Math.PI / 64), new Vec2(32, 32));
            for (double longitude : new double[]{-178, -110, -90, -89.9, 89.9, 90, 110, 178}) {
                MapScale scale = MapScale.hpc(180, .1);
                MapView hpc = MapView.create(Display.getCamera(), viewpoint, MapMode.HPC, GridType.Viewpoint, new MapScale[]{scale});
                Transform.ortho2D(1, 1, -longitude / 360, 0);
                GLSLScreenShader.setView(hpc, Display.getViewport(0));
                for (float outer : new float[]{1, 32, Float.POSITIVE_INFINITY}) {
                    for (int enhanced : new int[]{0, 1, 3}) {
                        for (int diffMode : new int[]{0, 1}) {
                            for (int effect = 0; effect < 4; effect++) {
                                // Rear-facing rays retain their XY sector direction, with no mirrored plane point.
                                float sector = effect == 1 ? 0 : effect == 2 ? (float) Math.PI : 0;
                                GLSLImageShader.bindDisplay(new float[]{1, 1, 1, 1}, 0, 0, 0, diffMode, 0, 1, 1, 1,
                                        sector, effect == 1 || effect == 2 ? .1f : 0, 0, 0,
                                        1, 0, effect == 3 ? 32 : Float.POSITIVE_INFINITY, 0, 0, outer, 0, 1, enhanced);
                                GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
                                GLSLImageShader.render(MapMode.HPC, Quat.ZERO, centered(10), wide, centered(10),
                                        wide, centered(10), Mat2.IDENTITY, 0, 0, false);
                                boolean masked = !Float.isInfinite(outer) || effect == 3
                                        || effect == 1 && longitude > 0 || effect == 2 && longitude < 0;
                                int expected = masked ? 0 : Math.abs(longitude) >= 90 || enhanced == 0 || diffMode != 0 ? 128 : 255;
                                assertPixel(size / 2, size / 2, expected, "HPC horizon: " + longitude + " outer " + outer
                                        + " enhancement " + enhanced + " difference " + diffMode + " mask " + effect);
                            }
                        }
                    }
                }
            }
        } finally {
            decoded.release();
        }
        System.out.println("PASS: source slits independent of map bounds, inner/outer radius, sector and cutoff masks, and coronal enhancement in every image mode");
    }

    private static void assertPixel(int x, int y, int expected, String context) {
        ByteBuffer pixel = BufferUtils.newByteBuffer(4);
        GL.glReadPixels(x, y, 1, 1, GL.RGBA, GL.UNSIGNED_BYTE, pixel);
        if (GLException.checkErrors(context) || Math.abs((pixel.get(0) & 255) - expected) > (expected == 0 ? 0 : 2))
            throw new AssertionError(context + ": " + (pixel.get(0) & 255) + " vs " + expected);
    }

    private static void warpPicking(JHVTime time) {
        Viewport vp = Display.getViewport(0);
        int x = vp.x + vp.width / 2, y = vp.yAWT + vp.height / 2;
        double oldX = Display.getCamera().getTranslationX(), oldY = Display.getCamera().getTranslationY();
        try {
            for (double distance : new double[]{10, 215}) {
                Position observer = new Position(time, distance, .7, .2);
                Vec3 point = new Vec3(.3, .4, Math.sqrt(.75));
                Vec3 expected = observer.toQuat().rotateInverseVector(point);
                double px = distance * point.x / (distance - point.z), py = distance * point.y / (distance - point.z);
                for (MapMode mode : new MapMode[]{MapMode.RadialWarp, MapMode.RectWarp}) {
                    MapView view = MapView.create(Display.getCamera(), observer, mode, GridType.Viewpoint,
                            new MapScale[]{MapScale.boxCoxRadial(3, 0)});
                    // The inner disk is linear. Place this known perspective ray under the mouse.
                    double upX = mode == MapMode.RadialWarp ? px / 6 : (Math.atan2(-px, py) / (2 * Math.PI) + .5) * vp.aspect;
                    double upY = mode == MapMode.RadialWarp ? py / 6 : Math.hypot(px, py) / 3 - .5;
                    double width = view.cameraWidth(vp);
                    Display.getCamera().setTranslation(((double) (x - vp.x) / vp.width - .5) * width * vp.aspect - upX,
                            (.5 - (double) (y - vp.yAWT) / vp.height) * width - upY);
                    Vec3 actual = view.mouseToSurface(vp, x, y);
                    if (actual == null || !(Math.hypot(Math.hypot(actual.x - expected.x, actual.y - expected.y), actual.z - expected.z) <= 2e-12))
                        throw new AssertionError("Warp picking: " + mode + " distance " + distance + ": " + actual);
                }
            }
        } finally {
            Display.getCamera().setTranslation(oldX, oldY);
        }
        System.out.println("PASS: both warped-view mouse paths recover an independently constructed sphere point for tilted observers");
    }

    private static void orthoComparison(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                        Position viewpoint, int size) {
        MapView view = MapView.create(Display.getCamera(), viewpoint, MapMode.Orthographic, GridType.Viewpoint, new MapScale[]{MapScale.ortho});
        Transform.ortho(1, 4, 0, 0, Quat.ZERO);
        GLSLScreenShader.setView(view, Display.getViewport(0));
        WcsSolution observer = new WcsSolution(WcsSolution.Projection.ZPN, new float[]{0, 1, 0, 2, 0, 0}, 10);
        WcsSolution surface = new WcsSolution(WcsSolution.Projection.CEA, new float[]{0, .5f, 0, 0, 0, 0}, 1);
        ImageGeometry observerGeometry = new ImageGeometry(129, 97, observer,
                new Mat2(4. / 129, 0, 0, -4. / 97), new Vec2(64.5, 48.5));
        ImageGeometry surfaceGeometry = new ImageGeometry(129, 97, surface,
                new Mat2(2 * Math.PI / 129, 0, 0, -4. / 97), new Vec2(64.5, 48.5));
        // The observer image is seen through display * conj(source) = rotation by -pi/3; the map through the display.
        Position observerSource = new Position(viewpoint.time, 10, Math.PI / 2, 0), display = new Position(viewpoint.time, 10, Math.PI / 6, 0);
        for (int level : new int[]{0, 3}) {
            DecodedImage observerImage = ramp(observerGeometry, 3, 5, level);
            DecodedImage surfaceImage = ramp(surfaceGeometry, 3, 5, level + 1);
            try {
                for (boolean reversed : new boolean[]{false, true}) {
                    DecodedImage current = reversed ? surfaceImage : observerImage, prior = reversed ? observerImage : surfaceImage;
                    texture.upload(current.imageBuffer());
                    difference.upload(prior.imageBuffer());
                    mask.bind();
                    difference.bind();
                    texture.bind();
                    GLSLImageShader.bindDisplay(new float[]{1, 1, 1, 1}, 0, 0, 0, 1,
                            0, 1, 1, 1, 0, 0, 0, 0, 0, 0, Float.POSITIVE_INFINITY, 0, 0, 100, 0, 1, 0);
                    GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
                    GLSLImageShader.render(MapMode.Orthographic, display.toQuat(), display,
                            current.geometry(), observerSource,
                            prior.geometry(), observerSource, Mat2.IDENTITY, 0, 0, false);
                    ByteBuffer pixels = BufferUtils.newByteBuffer(size * size * 4);
                    GL.glReadPixels(0, 0, size, size, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
                    for (int offset : new int[]{0, 48}) {
                        int x = size / 2 + offset, y = size / 2 + 16;
                        double vx = 4 * ((x + .5) / size - .5), vy = 4 * ((y + .5) / size - .5);
                        double vz = Math.sqrt(1 - vx * vx - vy * vy);
                        // Analytic Y rotations and tangent-plane intersection, independent of shader helpers.
                        double ox = .5 * vx + Math.sqrt(.75) * vz, oz = -Math.sqrt(.75) * vx + .5 * vz;
                        if (oz <= 0) {
                            ox = 2 * vx;
                            oz = 0;
                        }
                        double radius = Math.hypot(ox, vy), eta = Math.atan2(radius, 10 - oz);
                        double factor = 10 * (eta + 2 * eta * eta * eta) / radius;
                        double observerValue = .1 + .35 * (.5 + factor * ox / 4) + .3 * (.5 - factor * vy / 4);
                        double longitude = Math.atan2(Math.sqrt(.75) * vx - .5 * vz, .5 * vx + Math.sqrt(.75) * vz);
                        double surfaceValue = .1 + .35 * (.5 + longitude / (2 * Math.PI)) + .3 * (.5 - vy / 2);
                        double value = .5 + (reversed ? -2.5 : 2.5) * (observerValue - surfaceValue);
                        int actual = pixels.get(4 * (y * size + x)) & 255;
                        if (Math.abs(actual - Math.floor(value * 256)) > 2)
                            throw new AssertionError("Orthographic comparison: grid " + level + " reversed " + reversed
                                    + " offset " + offset + ": " + actual + " vs " + value);
                    }
                }
            } finally {
                surfaceImage.release();
                observerImage.release();
            }
        }
        System.out.println("PASS: independent Orthographic sphere/plane comparison with distinct rotations, projections, PV coefficients and decoded grids in both image orders");
    }

    private static void orthographicTan(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                  Position viewpoint, int size) {
        MapView view = MapView.create(Display.getCamera(), viewpoint, MapMode.Orthographic, GridType.Viewpoint, new MapScale[]{MapScale.ortho});
        Transform.ortho(1, 4, 0, 0, Quat.ZERO);
        GLSLScreenShader.setView(view, Display.getViewport(0));
        ByteBuffer white = BufferUtils.newByteBuffer(1);
        white.put(0, (byte) 255);
        mask.upload2D(GLTexture.Format.R8, 1, 1, GL.NEAREST, white);
        // Close distance makes perspective TAN clearly distinguishable from the
        // intended affine display mapping. Both images have their own CRVAL and grid.
        WcsSolution wcs = new WcsSolution(WcsSolution.Projection.TAN, new float[6], 2);
        ImageGeometry full = new ImageGeometry(129, 97, wcs,
                new Mat2(.025, .005, .003, -.035), new Vec2(63.25, 46.75));
        Mat2 adjustment = new Mat2(.96, -.28, .28, .96);
        for (int level : new int[]{0, 3}) {
            DecodedImage image = ramp(full, 3, 5, level);
            DecodedImage prior = ramp(full, 3, 5, level + 1);
            try {
                texture.upload(image.imageBuffer());
                difference.upload(prior.imageBuffer());
                mask.bind();
                difference.bind();
                texture.bind();
                ImageGeometry currentGeometry = new ImageGeometry(129, 97, wcs.adjusted(7200, -3600), full.pixelToPlane, full.referencePixel);
                ImageGeometry priorGeometry = new ImageGeometry(129, 97, wcs.adjusted(-3600, 5400), full.pixelToPlane, full.referencePixel);
                for (int effect : new int[]{0, 2}) {
                    GLSLImageShader.bindDisplay(new float[]{1, 1, 1, 1}, 0, 0, 0, effect,
                            0, 1, 1, 1, 0, 0, 0, 0, 0, 0, Float.POSITIVE_INFINITY, 0, 0, 100, 0, 1, 0);
                    GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
                    GLSLImageShader.render(MapMode.Orthographic, Quat.ZERO, centered(2), currentGeometry.forGrid(image.geometry().grid), centered(2),
                            priorGeometry.forGrid(prior.geometry().grid), centered(2), adjustment, 0, 0, false);
                    ByteBuffer pixels = BufferUtils.newByteBuffer(size * size * 4);
                    GL.glReadPixels(0, 0, size, size, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
                    for (int[] sample : new int[][]{{0, 0}, {48, -24}, {-32, 40}, {80, 0}}) {
                        int x = size / 2 + sample[0], y = size / 2 + sample[1];
                        // Parallel screen rays give XY directly, on the sphere and off-limb.
                        double worldX = 4 * ((x + .5) / size - .5), worldY = 4 * ((y + .5) / size - .5);
                        double[] values = new double[2];
                        for (int i = 0; i < 2; i++) {
                            double dx = worldX - 2 * Math.toRadians(i == 0 ? 2 : -1);
                            double dy = worldY - 2 * Math.toRadians(i == 0 ? -1 : 1.5);
                            double px = .96 * dx - .28 * dy, py = .28 * dx + .96 * dy;
                            // Solve the declared two-row pixel matrix independently.
                            double determinant = .025 * -.035 - .005 * .003;
                            double u = (63.25 + (-.035 * px - .005 * py) / determinant) / 129;
                            double v = (46.75 + (-.003 * px + .025 * py) / determinant) / 97;
                            values[i] = .1 + .35 * u + .3 * v;
                        }
                        double value = effect == 0 ? values[0] : .5 + 2.5 * (values[0] - values[1]);
                        int expected = (int) Math.floor(value * 256);
                        int actual = pixels.get(4 * (y * size + x)) & 255;
                        // Half-float filtering, LUT quantization and one-byte dither.
                        if (Math.abs(actual - expected) > 2)
                            throw new AssertionError("Orthographic TAN: grid " + level + " effect " + effect + " point " + x + "," + y
                                    + ": " + actual + " vs " + expected);
                    }
                }
            } finally {
                prior.release();
                image.release();
            }
        }
        System.out.println("PASS: independent Orthographic TAN affine reference, user offsets/rotation, comparison image and reduced grids");
    }

    private static void transforms(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                   WcsSolution wcs, Position viewpoint, int size) {
        MapView view = MapView.create(Display.getCamera(), viewpoint, MapMode.Orthographic, GridType.Viewpoint, new MapScale[]{MapScale.ortho});
        Transform.ortho(1, 4, 0, 0, Quat.ZERO);
        GLSLScreenShader.setView(view, Display.getViewport(0));
        Vec2 referencePixel = new Vec2(61.25, 45.75);
        ImageGeometry referenceGeometry = new ImageGeometry(129, 97, wcs,
                new Mat2(.04, .01, .005, -.06), referencePixel);
        // Rotate both plane axes by 90 degrees. Applying the same rotation to
        // the plane input must recover the reference, including its shear and unequal scales.
        ImageGeometry rotated = new ImageGeometry(129, 97, wcs,
                new Mat2(-.005, .06, .04, .01), referencePixel);
        Mat2 adjustment = new Mat2(0, -1, 1, 0);
        DecodedImage reference = ramp(referenceGeometry, 3, 5, 0);
        try {
            for (int level : new int[]{0, 3}) {
                DecodedImage image = ramp(rotated, 3, 5, level);
                DecodedImage prior = ramp(rotated, 3, 5, level + 1);
                try {
                    for (int effect = 0; effect < 4; effect++) {
                        ByteBuffer expected = renderSampling(texture, difference, mask, reference, reference,
                                MapMode.Orthographic, effect, size, Mat2.IDENTITY);
                        ByteBuffer actual = renderSampling(texture, difference, mask, image, prior,
                                MapMode.Orthographic, effect, size, adjustment);
                        int visible = 0;
                        for (int i = 0; i < actual.capacity(); i += 4) {
                            int a = actual.get(i) & 255, e = expected.get(i) & 255;
                            if ((a == 0) != (e == 0) || Math.abs(a - e) > 2)
                                throw new AssertionError("Rotated sampling mismatch: level " + level + " effect " + effect + " pixel " + i / 4);
                            if (e != 0) visible++;
                        }
                        if (visible < 20)
                            throw new AssertionError("Rotation test rendered no useful pixels");
                    }
                } finally {
                    prior.release();
                    image.release();
                }
            }
        } finally {
            reference.release();
        }
        System.out.println("PASS: Java shader binding composes user rotation, shear, unequal scales and reduced grids");
    }

    private static void wrapping(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                 Position viewpoint, int size) throws Exception {
        MapView view = MapView.create(Display.getCamera(), viewpoint, MapMode.Latitudinal, GridType.Viewpoint, new MapScale[]{MapScale.lati});
        Transform.ortho2D(1, 1, 0, 0);
        GLSLScreenShader.setView(view, Display.getViewport(0));
        for (WcsSolution.Projection projection : new WcsSolution.Projection[]{WcsSolution.Projection.CAR, WcsSolution.Projection.CEA}) {
            float[] pv = {0, 1, 0, 0, 0, 0};
            WcsSolution wcs = new WcsSolution(projection, pv, 1);
            double height = projection == WcsSolution.Projection.CAR ? Math.PI : 2;
            // The mask and slit admit only coordinates that started negative and wrapped.
            // Odd dimensions and a nonzero decoder origin also exercise scale and offset.
            ImageGeometry full = new ImageGeometry(129, 97, wcs,
                    new Mat2(2 * Math.PI / 129, 0, 0, -height / 97), new Vec2(-129.0 / 2, 97.0 / 2));
            DecodedImage reference = ramp(full, 3, 5, 0);
            DecodedImage image = ramp(full, 3, 5, 3);
            DecodedImage prior = ramp(full, 3, 5, 4);
            try {
                for (int effect = 0; effect < 4; effect++) {
                    ByteBuffer expected = renderSampling(texture, difference, mask, reference, reference, MapMode.Latitudinal, effect, size, Mat2.IDENTITY);
                    ByteBuffer actual = renderSampling(texture, difference, mask, image, prior, MapMode.Latitudinal, effect, size, Mat2.IDENTITY);
                    int visible = 0;
                    for (int i = 0; i < actual.capacity(); i += 4) {
                        int a = actual.get(i) & 255, e = expected.get(i) & 255;
                        if ((a == 0) != (e == 0) || Math.abs(a - e) > 2)
                            throw new AssertionError("Wrapped sampling mismatch: " + projection + " effect " + effect
                                    + " pixel " + i / 4 + ": " + a + " vs " + e);
                        if (e != 0) visible++;
                    }
                    if (visible < 20)
                        throw new AssertionError("Wrapping test rendered no useful pixels: " + projection);
                }
            } finally {
                prior.release();
                image.release();
                reference.release();
            }
            surfaceCoverage(texture, difference, mask, wcs, height, size);
            Header header = new Header();
            header.addValue("NAXIS", 2, null);
            header.addValue("CTYPE1", "CRLN-" + projection, null);
            header.addValue("CTYPE2", "CRLT-" + projection, null);
            header.addValue("CUNIT1", "deg", null);
            header.addValue("CUNIT2", "deg", null);
            header.addValue("CRVAL1", 35., null);
            header.addValue("CRVAL2", 20., null);
            header.addValue("PV1_0", 1., null);
            header.addValue("PV1_1", 10., null);
            header.addValue("PV1_2", 15., null);
            // Check actual Java uniform packing with a tilted native frame,
            // nonzero plane origin and a cached user CRVAL adjustment together.
            WcsSolution prepared = new WcsSolution(header, 1).adjusted(7200, -10800);
            surfaceCoverage(texture, difference, mask, prepared, height, size);
        }
    }

    private static void surfaceCoverage(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                        WcsSolution wcs, double height, int size) {
        Mat2[] matrices = {
                new Mat2(Math.PI / (3 * 129), 0, 0, -height / 97),
                new Mat2(2 * Math.PI / 129, 0, 0, -height / 97),
                new Mat2(2 * Math.PI / 129, .4 / 97, .2 / 129, -height / 97),
                new Mat2(0, 2 * Math.PI / 97, height / 129, 0),
                new Mat2(-Math.PI / (3 * 129), 0, 0, height / 97),
                new Mat2(height / 129, 0, 0, -2 * Math.PI / 97)};
        for (int test = 0; test < matrices.length; test++) {
            Vec2 reference = new Vec2(test == 1 || test == 2 ? -64.5 : 64.5, test == 3 || test == 5 ? -48.5 : 48.5);
            Mat2 adjustment = test == 5 ? new Mat2(0, -1, 1, 0) : Mat2.IDENTITY;
            ImageGeometry full = new ImageGeometry(129, 97, wcs, matrices[test], reference);
            ImageGeometry other = new ImageGeometry(129, 97, wcs, matrices[test], new Vec2(reference.x + 11, reference.y - 9));
            for (int level : new int[]{0, 3}) {
                DecodedImage image = ramp(full, 3, 5, level);
                DecodedImage prior = ramp(other, 3, 5, level + 1);
                try {
                    for (int effect : new int[]{0, 2}) {
                        ByteBuffer actual = renderSampling(texture, difference, mask, image, prior, MapMode.Latitudinal, effect, size, adjustment);
                        int visible = 0;
                        for (int y = 0; y < size; y++) {
                            double latitude = Math.PI * ((y + .5) / size - .5);
                            for (int x = 0; x < size; x++) {
                                double mapX = (x + .5) / size;
                                double longitude = 2 * Math.PI * (mapX - .5);
                                Vec2 p = surfacePixel(full, adjustment, longitude, latitude);
                                Vec2 q = surfacePixel(other, adjustment, longitude, latitude);
                                boolean present = p != null &&
                                        (int) (8 * p.y / 97) >= 3 && (int) (8 * p.y / 97) < 5 &&
                                        (effect == 0 || q != null);
                                double expected = 0;
                                if (present) {
                                    expected = .1 + .35 * p.x / 129 + .3 * p.y / 97;
                                    if (effect == 2)
                                        expected = (expected - (.1 + .35 * q.x / 129 + .3 * q.y / 97)) * 2.5 + .5;
                                    visible++;
                                }
                                int value = actual.get(4 * (y * size + x)) & 255;
                                if ((value != 0) != present || Math.abs(value - Math.round(expected * 255)) > 2)
                                    throw new AssertionError("Surface coverage mismatch: " + wcs.projection + " case " + test
                                            + " level " + level + " effect " + effect + " pixel " + x + "," + y
                                            + ": " + value + " vs " + expected);
                            }
                        }
                        if (visible < 20)
                            throw new AssertionError("Empty surface coverage test: " + wcs.projection + " case " + test);
                    }
                } finally {
                    prior.release();
                    image.release();
                }
            }
        }
        System.out.println("PASS: " + wcs.projection + " coverage, slit, mask and comparison sampling against whole-turn enumeration");
    }

    private static Vec2 surfacePixel(ImageGeometry image, Mat2 adjustment, double longitude, double latitude) {
        Vec2 nativePlane = image.wcs.worldToPlane(longitude, latitude);
        if (nativePlane == null)
            return null;
        // These fixtures span fewer than three turns. Enumerate independently
        // of the shader's interval solver, preferring the principal longitude.
        for (int turn : new int[]{0, -1, 1, -2, 2, -3, 3}) {
            Vec2 plane = adjustment.transform(nativePlane.x + turn * 2 * Math.PI, nativePlane.y);
            Vec2 pixel = image.toPixel(plane.x, plane.y);
            if (pixel.x >= .375 * 129 && pixel.x <= .625 * 129 && pixel.y >= 0 && pixel.y <= 97)
                return pixel;
        }
        return null;
    }

    private static void sharpening(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                   WcsSolution wcs, Position viewpoint, int size) {
        // Odd dimensions make the two decoded texture extents differ. At the center,
        // both grids have a sample; the comparison image's blur taps are half as far apart.
        ImageGeometry full = new ImageGeometry(129, 97, wcs, new Mat2(4.0 / 129, 0, 0, -4.0 / 97), new Vec2(64.5, 48.5));
        MapView view = MapView.create(Display.getCamera(), viewpoint, MapMode.Orthographic, GridType.Viewpoint, new MapScale[]{MapScale.ortho});
        Transform.ortho(1, 4, 0, 0, Quat.ZERO);
        GLSLScreenShader.setView(view, Display.getViewport(0));
        DecodedImage[] images = new DecodedImage[2];
        try {
            for (int i = 0; i < images.length; i++) {
                ImageGrid grid = new ImageGrid(3 + i, i == 0 ? 17 : 9, i == 0 ? 13 : 7);
                short[] pixels = new short[grid.width() * grid.height()];
                for (int y = 0; y < grid.height(); y++) {
                    for (int x = 0; x < grid.width(); x++) {
                        int parity = (x - grid.width() / 2 + y - grid.height() / 2) & 1;
                        pixels[y * grid.width() + x] = Float.floatToFloat16(parity == 0 ? .9f : .1f);
                    }
                }
                images[i] = new DecodedImage(ImageBuffer.fromShorts(grid.width(), grid.height(), pixels, ImageFilter.NONE), full.forGrid(grid));
            }
            // CPU reference: bilinear samples of the checkerboard at nine weighted taps.
            // Distances are in texels, independently of the shader's normalized coordinates.
            double[] weights = {.30613, .38774, .30613};
            double blur = 0, differenceBlur = 0;
            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    double dx = x * 1.2004377, dy = y * 1.2004377;
                    double weight = weights[x + 1] * weights[y + 1];
                    double sample = checkerSample(dx, dy);
                    blur += weight * sample;
                    differenceBlur += weight * (sample - checkerSample(dx / 2, dy / 2));
                }
            }
            for (int effect : new int[]{1, 3}) {
                double value = effect == 1 ? 2 * checkerSample(0, 0) - blur : .5 - 2.5 * differenceBlur;
                int expected = (int) Math.clamp(Math.floor(value * 256), 0, 255);
                ByteBuffer actual = renderSampling(texture, difference, mask, images[0], images[1], MapMode.Orthographic, effect, size, Mat2.IDENTITY);
                int center = actual.get(4 * (size * (size / 2) + size / 2)) & 255;
                // Dithering adds at most one grayscale step; LUT quantization adds another.
                if (Math.abs(center - expected) > 2)
                    throw new AssertionError("Nonlinear sharpening differs from CPU reference: effect " + effect + ": " + center + " vs " + expected);
            }
        } finally {
            for (DecodedImage image : images)
                if (image != null)
                    image.release();
        }
    }

    private static double checkerSample(double x, double y) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double low = Float.float16ToFloat(Float.floatToFloat16(.1f));
        double high = Float.float16ToFloat(Float.floatToFloat16(.9f));
        double sign = ((ix + iy) & 1) == 0 ? 1 : -1;
        return (high + low) / 2 + (high - low) / 2 * sign * (1 - 2 * (x - ix)) * (1 - 2 * (y - iy));
    }

    private static DecodedImage ramp(ImageGeometry full, int originX, int originY, int reduction) {
        int step = 1 << reduction;
        int x0 = Math.ceilDiv(originX, step), y0 = Math.ceilDiv(originY, step);
        int width = Math.ceilDiv(originX + full.grid.width(), step) - x0;
        int height = Math.ceilDiv(originY + full.grid.height(), step) - y0;
        double firstX = x0 * step - originX + .5, firstY = y0 * step - originY + .5;
        short[] pixels = new short[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double px = firstX + x * step, py = firstY + y * step;
                pixels[y * width + x] = Float.floatToFloat16((float) (.1 + .35 * px / full.grid.width() + .3 * py / full.grid.height()));
            }
        }
        ImageGrid grid = new ImageGrid(reduction, width, height, firstX, firstY);
        return new DecodedImage(ImageBuffer.fromShorts(width, height, pixels, ImageFilter.NONE), full.forGrid(grid));
    }

    private static ByteBuffer renderSampling(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                             DecodedImage image, DecodedImage prior, MapMode mode, int effect, int size, Mat2 adjustment) {
        texture.upload(image.imageBuffer());
        difference.upload(prior.imageBuffer());
        mask.bind();
        difference.bind();
        texture.bind();
        GLSLImageShader.bindDisplay(new float[]{1, 1, 1, 1}, 1f / image.imageBuffer().width, 1f / image.imageBuffer().height,
                (effect & 1) != 0 ? -1 : 0, effect >= 2 ? 1 : 0,
                0, 1, 1, 1, 0, 0, 0, 0, 0, 0, Float.POSITIVE_INFINITY, 0, 0, 100, .375f, .625f, 0);
        GL.glClearColor(0, 0, 0, 1);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
        GLSLImageShader.render(mode, Quat.ZERO, centered(215), image.geometry(), centered(215),
                prior.geometry(), centered(215), adjustment, 0, 0, false);
        ByteBuffer pixels = BufferUtils.newByteBuffer(size * size * 4);
        GL.glReadPixels(0, 0, size, size, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
        if (GLException.checkErrors("ImageRenderingTest sampling"))
            throw new AssertionError("Sampling render failed");
        return pixels;
    }

    private static Position centered(double distance) {
        return new Position(TIME, distance, 0, 0);
    }
}
