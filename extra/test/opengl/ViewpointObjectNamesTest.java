package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.DisplayLayout;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.TimestampLayer;
import org.helioviewer.jhv.layers.ViewpointLayer;
import org.helioviewer.jhv.layers.viewpoint.SpaceObjectElement;
import org.helioviewer.jhv.layers.viewpoint.ViewpointLayerOptions;
import org.helioviewer.jhv.layers.viewpoint.ViewpointLayerOptionsExpert;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.thread.UiThread;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ViewpointObjectNamesTest {

    private static final JHVTime TIME = new JHVTime("2024-06-25T00:00:00");
    private static final long END = TIME.milli + 1000;
    private static final Target EARTH = new Target("Earth", 26, 14, 4);
    private static final Target ORBITER = new Target("Solar Orbiter", 26, 14, 4);

    public static void main(String[] args) throws Exception {
        Path output = args.length == 0 ? null : Files.createDirectories(Path.of(args[0]));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        DisplayController.setRenderRequestHandler(_ -> {});
        field(Player.class, "movieStart").setLong(null, TIME.milli);
        field(Player.class, "movieEnd").setLong(null, END);
        field(Player.class, "lastTimestamp").set(null, TIME);
        // Keep the drawable large enough for the export checks on ANGLE Vulkan.
        AngleRenderer renderer = AngleRenderer.pbuffer(2048, 2048);
        try {
            for (boolean white : new boolean[]{false, true}) {
                Display.whiteBackground = white;
                checkZoomAndPlacement(output, white);
                checkCrowded(output, white);
                checkReferenceRing(output, white);
            }
            Display.whiteBackground = false;
            checkPlaybackAndMode(false);
            checkPlaybackAndMode(true);
            checkClipping();
            checkExportScaling();
            check(!GLException.checkErrors("ViewpointObjectNamesTest"), "OpenGL error after drawing names");
        } finally {
            renderer.destroy();
        }
        System.out.println("PASS: Heliosphere names, reference ring, marker alignment, clipping, crowded names, first-frame playback updates, and viewport-relative export overlays");
    }

    private static void checkZoomAndPlacement(Path output, boolean white) throws Exception {
        for (Target target : List.of(EARTH, ORBITER)) {
            Bounds original = null;
            for (int width : new int[]{160, 320}) {
                try (Fixture fixture = new Fixture(List.of(target), width, true)) {
                    ByteBuffer scene = fixture.scene();
                    ByteBuffer named = fixture.withNames();
                    Bounds marker = difference(null, scene, fixture.viewport, white);
                    Bounds text = difference(scene, named, fixture.viewport, white);
                    check(marker.count > 0, "Missing marker for " + target.name);
                    check(text.count > 30, "Missing persistent label for " + target.name);
                    check(text.left > marker.right && text.left - marker.right <= 12, "Label detached from " + target.name);
                    check(Math.abs(text.centerY() - marker.centerY()) <= 8, "Label is vertically misaligned with " + target.name);
                    if (original == null)
                        original = text;
                    else
                        check(Math.abs(original.width() - text.width()) <= 2, "Label width changed with zoom");
                    if (width == 160)
                        save(output, target.name.replace(' ', '-') + "-" + white, named, fixture.viewport);
                }
            }
        }
    }

    private static void checkCrowded(Path output, boolean white) throws Exception {
        List<Target> targets = List.of(
                new Target("Earth", 0, 0, 0), new Target("SOHO", 0.2, 0, 0), new Target("SDO", 0, 0.2, 0),
                new Target("PROBA-2", -0.2, 0, 0), new Target("Solar Orbiter", 0, -0.2, 0),
                new Target("Parker Solar Probe", 0.2, 0.2, 0));
        try (Fixture fixture = new Fixture(targets, 160, false)) {
            ByteBuffer scene = fixture.scene();
            ByteBuffer named = fixture.withNames();
            checkCrowdedName("SOHO", -1, 1, scene, named, fixture.viewport);
            checkCrowdedName("Solar Orbiter", 1, 1, scene, named, fixture.viewport);
            checkCrowdedName("Parker Solar Probe", -1, 0, scene, named, fixture.viewport);
            checkCrowdedName("Earth", 1, 0, scene, named, fixture.viewport);
            checkCrowdedName("PROBA-2", -1, -1, scene, named, fixture.viewport);
            checkCrowdedName("SDO", 1, -1, scene, named, fixture.viewport);
            save(output, "crowded-" + white, named, fixture.viewport);
        }
    }

    private static void checkCrowdedName(String name, int side, int row, ByteBuffer scene, ByteBuffer named, Viewport vp) {
        int centerX = vp.width / 2, centerY = vp.height / 2 + row * 17;
        int x0 = side < 0 ? centerX - 140 : centerX + 7;
        int x1 = side < 0 ? centerX - 7 : centerX + 140;
        int left = x1, right = x0 - 1, pixels = 0;
        for (int y = centerY - 7; y < centerY + 7; y++) {
            for (int x = x0; x < x1; x++) {
                int offset = 4 * (y * vp.width + x);
                int delta = Math.abs((scene.get(offset) & 255) - (named.get(offset) & 255));
                if (delta > 130) {
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                    pixels++;
                }
            }
        }
        float textWidth = GLText.renderer().measureWidth(name) * 12 / GLText.renderer().getFontSize();
        check(pixels > 6 && Math.abs(right - left + 1 - textWidth) <= 7,
                "Crowded frame lost " + name + " (" + pixels + " pixels, expected width " + textWidth + ", got " + (right - left + 1) + ")");
    }

    private static void checkReferenceRing(Path output, boolean white) throws Exception {
        try (Fixture fixture = new Fixture(List.of(new Target("Earth", 26, 0, 0)), 160, false)) {
            ByteBuffer scene = fixture.scene();
            Bounds marker = difference(null, scene, fixture.viewport, white);
            ByteBuffer withoutRing = fixture.withNames();
            Bounds name = difference(scene, withoutRing, fixture.viewport, white);
            check(marker.count > 0 && name.left > marker.right, "Absolute mode without a spiral drew a reference ring");

            ViewpointLayerOptionsExpert options = fixture.layer.getOptions().getEquatorialOptions();
            options.setRelative(true);
            check(scene.equals(fixture.scene()), "The fixture must keep the marker in place when switching longitude mode");
            ByteBuffer relative = fixture.withNames();
            Bounds ring = difference(withoutRing, relative, fixture.viewport, white);
            check(ring.count > 10 && ring.left < marker.left && ring.right > marker.right
                    && ring.bottom < marker.bottom && ring.top > marker.top, "Relative reference did not get a ring: " + ring);
            save(output, "reference-" + white, relative, fixture.viewport);

            options.setRelative(false);
            options.setSpiral(true);
            ByteBuffer spiral = fixture.scene();
            Bounds overlay = difference(spiral, fixture.withNames(), fixture.viewport, white);
            check(overlay.left < marker.left && overlay.bottom < marker.bottom && overlay.top > marker.top,
                    "Spiral reference did not get a ring: " + overlay);
            options.setSpiral(false);
            fixture.scene();
            check(withoutRing.equals(fixture.withNames()), "Reference ring survived disabling Relative and spiral");
        }
    }

    private static void checkPlaybackAndMode(boolean relative) throws Exception {
        Target target = relative ? new Target("Venus", 26, 14, 4) : ORBITER;
        try (Fixture fixture = new Fixture(List.of(target), 160, relative, relative)) {
            fixture.layer.getOptions().getEquatorialOptions().setSpiral(true);
            ByteBuffer marker = fixture.scene();
            ByteBuffer named = fixture.withNames();

            Field timestamp = field(Player.class, "lastTimestamp");
            try {
                timestamp.set(null, new JHVTime(TIME.milli - 1));
                check(marker.equals(fixture.scene()) && named.equals(fixture.withNames()),
                        "The orbit and label did not use the start of the playback range");

                timestamp.set(null, new JHVTime(TIME.milli + 700));
                ByteBuffer nextMarker = fixture.scene();
                Bounds firstText = difference(marker, named, fixture.viewport, false);
                Bounds nextText = difference(nextMarker, fixture.withNames(), fixture.viewport, false);
                check(!marker.equals(nextMarker) && nextText.count > 30 && nextText.left > firstText.left,
                        "The orbit and " + (relative ? "relative reference" : "absolute reference") + " label did not update on the first draw");

                timestamp.set(null, new JHVTime(END));
                ByteBuffer endScene = fixture.scene();
                ByteBuffer endNames = fixture.withNames();
                timestamp.set(null, new JHVTime(END + 60_000));
                check(endScene.equals(fixture.scene()) && endNames.equals(fixture.withNames()),
                        "The orbit and label did not use the end of the playback range");
            } finally {
                timestamp.set(null, TIME);
            }
            check(marker.equals(fixture.scene()) && named.equals(fixture.withNames()),
                    "The orbit and label did not return to the earlier time on the first draw");

            fixture.layer.getOptions().setCameraMode(ViewpointLayerOptions.CameraMode.ObserverAt1au, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
            ByteBuffer scene = fixture.scene();
            check(scene.equals(fixture.withNames()), "Previous Heliosphere labels survived a mode switch");
        }
    }

    private static void checkClipping() throws Exception {
        try (Fixture fixture = new Fixture(List.of(new Target("Earth", 650.0 / 3, 0, 0)), 13000, false)) {
            ByteBuffer scene = fixture.scene();
            check(difference(null, scene, fixture.viewport, false).count > 0, "Wide view must show the Earth marker");
            check(difference(scene, fixture.withNames(), fixture.viewport, false).count > 30, "Wide view lost the Earth name");
        }

        try (Fixture fixture = new Fixture(List.of(new Target("Parker Solar Probe", 78, 44, 0)), 90, false)) {
            ByteBuffer scene = fixture.scene();
            Bounds marker = difference(null, scene, fixture.viewport, false);
            Bounds text = difference(scene, fixture.withNames(), fixture.viewport, false);
            check(marker.count > 0 && text.count > 50 && text.right < marker.left, "An edge marker lost its name or overlaps the name: " + marker + " and " + text);
            check(text.left > 0 && text.bottom > 0 && text.right < fixture.viewport.width - 1 && text.top < fixture.viewport.height - 1,
                    "The viewport clipped an edge label");
        }

        try (Fixture fixture = new Fixture(List.of(new Target("Earth", -80.25, 0, 0)), 90, false)) {
            ByteBuffer scene = fixture.scene();
            Bounds marker = difference(null, scene, fixture.viewport, false);
            check(marker.count > 0 && marker.left == 0, "Fixture marker must straddle the viewport edge");
            check(difference(scene, fixture.withNames(), fixture.viewport, false).count > 30, "Partially visible marker lost its name");
        }

        for (double x : new double[]{-80.875, -80.84375}) {
            try (Fixture fixture = new Fixture(List.of(new Target("Earth", x, 0, 0)), 90, false)) {
                ByteBuffer scene = fixture.scene();
                check(difference(null, scene, fixture.viewport, false).count == 0, "Near-tangent marker must have no visible pixels");
                check(scene.equals(fixture.withNames()), "Near-tangent marker left a visible name at " + x);
            }
        }

        List<Target> hidden = List.of(new Target("Earth", 100, 0, 0), new Target("Mercury", 0, 0, 40));
        for (Target target : hidden) {
            try (Fixture fixture = new Fixture(List.of(target), 20, false)) {
                ByteBuffer scene = fixture.scene();
                check(difference(null, scene, fixture.viewport, false).count == 0, "Fixture marker must be clipped");
                check(scene.equals(fixture.withNames()), "Clipped marker acquired a name");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkExportScaling() throws Exception {
        List<Layer> layers = (List<Layer>) field(Layers.class, "layers").get(null);
        List<Layer> originalLayers = List.copyOf(layers);
        MapView originalView = GLRenderer.getMapView();
        MapMode originalMode = Display.mode;
        Viewport originalViewport = Display.fullViewport;
        double originalScaleX = Display.pixelScale[0];
        double originalScaleY = Display.pixelScale[1];
        UpdateViewpoint originalUpdate = DisplayController.getViewpointUpdate();
        JSONObject originalCamera = DisplayController.cameraToJson();
        double cameraWidth = 160;
        try (Fixture fixture = new Fixture(List.of(ORBITER), cameraWidth, false)) {
            fixture.scene();
            layers.clear();
            layers.add(fixture.layer);
            Display.mode = MapMode.Orthographic;
            GLRenderer.reshape(640, 360);
            field(GLRenderer.class, "screenView").set(null, fixture.view);
            Position viewpoint = fixture.view.viewpoint();
            UiThread.postAndWait(() -> {
                DisplayController.setViewpointUpdate(_ -> viewpoint, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
                DisplayController.cameraFromJson(new JSONObject().put("fov", 2 * Math.atan(cameraWidth / (2 * viewpoint.distance)))
                        .put("translation", new JSONArray().put(0).put(0)).put("dragRotation", Quat.ZERO.toJson()));
                DisplayController.refreshCamera();
            });
            TimestampLayer timestamp = new TimestampLayer(new JSONObject().put("top", true));
            timestamp.setEnabled(true);
            layers.add(timestamp);
            ByteBuffer normalFrame = captureFrame(new GLGrab(1024, 1024));
            ByteBuffer largeFrame = captureFrame(new GLGrab(2048, 2048));
            int normal = nameWidth(normalFrame, 1024, 1024);
            int large = nameWidth(largeFrame, 2048, 2048);
            check(normal > 40 && Math.abs(2 * normal - large) <= 0.15 * normal,
                    "Label width did not scale with export resolution: " + normal + " and " + large);
            int normalStamp = timestampWidth(normalFrame, 1024, 1024);
            int largeStamp = timestampWidth(largeFrame, 2048, 2048);
            check(normalStamp > 100 && Math.abs(2 * normalStamp - largeStamp) <= 4,
                    "Timestamp width did not scale with export resolution");
            check(GLRenderer.getMapView() == fixture.view && Display.fullViewport.height == 360
                            && Display.getRenderPixelsPerPoint() == 1,
                    "Export scaling changed the screen view");
            TextWidths screen = fixture.textWidths(timestamp);
            Display.pixelScale[0] = Display.pixelScale[1] = 2;
            GLRenderer.reshape(1280, 720);
            TextWidths retina = fixture.textWidths(timestamp);
            check(Math.abs(2 * screen.name - retina.name) <= 4 && Math.abs(2 * screen.timestamp - retina.timestamp) <= 4,
                    "Screen text did not retain its logical size at 2x");
            ByteBuffer matchingCapture = captureFrame(new GLGrab(1280, 720, Display.getRenderPixelsPerPoint()));
            check(Math.abs(retina.name - nameWidth(matchingCapture, 1280, 720)) <= 4
                            && Math.abs(retina.timestamp - timestampWidth(matchingCapture, 1280, 720)) <= 4,
                    "On screen export did not match the displayed text sizes");
            GLRenderer.reshape(1920, 1080);
            TextWidths resized = fixture.textWidths(timestamp);
            check(Math.abs(retina.name - resized.name) <= 2 && Math.abs(retina.timestamp - resized.timestamp) <= 2,
                    "Resizing the screen changed its text sizes");
            check(largeFrame.equals(captureFrame(new GLGrab(2048, 2048))),
                    "Fixed-resolution overlays changed when the live canvas or display scale changed");
            timestamp.setScale(300);
            timestamp.setExtra(true);
            GLRenderer.reshape(320, 360);
            Bounds narrowTimestamp = fixture.timestampInk(timestamp);
            check(narrowTimestamp.width() > 285 && narrowTimestamp.right < 319 && narrowTimestamp.height() < 30,
                    "The large timestamp did not fit on one line: " + narrowTimestamp);
            timestamp.setScale(100);
            GLRenderer.reshape(1440, 400);
            Bounds topDetails = fixture.timestampInk(timestamp);
            timestamp.setTop(false);
            Bounds bottomDetails = fixture.timestampInk(timestamp);
            check(topDetails.count > 0 && bottomDetails.height() >= topDetails.height() - 1,
                    "Bottom timestamp clipped its details: " + bottomDetails + " vs " + topDetails);

            timestamp.setScale(300);
            timestamp.setExtra(false);
            Display.pixelScale[0] = Display.pixelScale[1] = 1;
            GLRenderer.reshape(1100, 748);
            Bounds bottomTimestamp = fixture.timestampInk(timestamp);
            check(bottomTimestamp.height() >= 38 && bottomTimestamp.bottom <= 8,
                    "The bottom timestamp moved from its inset: " + bottomTimestamp);
            layers.remove(timestamp);
            checkMultiviewExport(large);
        } finally {
            Display.pixelScale[0] = originalScaleX;
            Display.pixelScale[1] = originalScaleY;
            layers.clear();
            layers.addAll(originalLayers);
            Display.mode = originalMode;
            GLRenderer.reshape(originalViewport.width, originalViewport.height);
            field(GLRenderer.class, "screenView").set(null, originalView);
            UiThread.postAndWait(() -> {
                DisplayController.setViewpointUpdate(originalUpdate, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
                DisplayController.cameraFromJson(originalCamera);
            });
        }
    }

    private static void checkMultiviewExport(int singleNameWidth) {
        boolean originalMultiview = Display.multiview;
        List<ImageLayer> images = new ArrayList<>();
        TimestampLayer timestamp = new TimestampLayer(new JSONObject().put("top", true));
        timestamp.setEnabled(true);
        Layers.add(timestamp);
        try {
            Display.multiview = true;
            for (int i = 0; i < 3; i++) {
                ImageLayer image = ImageLayer.create();
                images.add(image);
                image.setEnabled(true);
            }

            int frameSize = 2048;
            GLRenderer.reshape(frameSize, frameSize);
            Viewport[] tiles = Display.getViewports();
            check(tiles.length == 3, "Fixture must produce a two-row multiview layout");

            Display.pixelScale[0] = Display.pixelScale[1] = 1;
            GLRenderer.reshape(640, 360);
            ByteBuffer frame = captureFrame(new GLGrab(frameSize, frameSize));
            Display.pixelScale[0] = Display.pixelScale[1] = 2;
            GLRenderer.reshape(1920, 1080);
            check(frame.equals(captureFrame(new GLGrab(frameSize, frameSize))),
                    "Multiview export overlays changed with the live canvas or monitor DPI");
            for (Viewport tile : tiles) {
                int right = tile.x + tile.width;
                int top = tile.yGL + tile.height;
                int timestampStart = top - tile.height / 8;
                int timestampWidth = exportedTextWidth(frame, frameSize, tile.x, timestampStart, right, top);
                check(timestampWidth > 100, "Missing top timestamp in viewport " + tile.idx);
                int nameWidth = exportedTextWidth(frame, frameSize, tile.x + tile.width / 2, tile.yGL, right, timestampStart);
                check(Math.abs(nameWidth - singleNameWidth) <= 2,
                        "Name width changed in viewport " + tile.idx + ": " + nameWidth + " vs " + singleNameWidth);
            }
        } finally {
            Display.multiview = originalMultiview;
            images.forEach(Layers::remove);
            Layers.remove(timestamp);
        }
    }

    private static ByteBuffer captureFrame(GLGrab grab) {
        try {
            ByteBuffer pixels = ByteBuffer.allocate(3 * grab.w * grab.h);
            grab.renderFrame(pixels);
            return pixels;
        } finally {
            grab.dispose();
        }
    }

    private static int nameWidth(ByteBuffer pixels, int width, int height) {
        return exportedTextWidth(pixels, width, width / 2, 0, width, 7 * height / 8);
    }

    private static int timestampWidth(ByteBuffer pixels, int width, int height) {
        return exportedTextWidth(pixels, width, 0, 7 * height / 8, width, height);
    }

    private record TextWidths(int name, int timestamp) {}

    private static int exportedTextWidth(ByteBuffer pixels, int width, int x0, int y0, int x1, int y1) {
        int left = x1, right = x0 - 1;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int offset = 3 * (y * width + x);
                int r = pixels.get(offset) & 255, g = pixels.get(offset + 1) & 255, b = pixels.get(offset + 2) & 255;
                if (r > 130 && Math.abs(r - g) < 10 && Math.abs(r - b) < 10) {
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                }
            }
        }
        return right - left + 1;
    }

    private static Bounds difference(ByteBuffer before, ByteBuffer after, Viewport vp, boolean white) {
        int left = vp.width, bottom = vp.height, right = -1, top = -1, count = 0;
        int background = white ? 255 : 0;
        for (int y = 0; y < vp.height; y++) {
            for (int x = 0; x < vp.width; x++) {
                int offset = 4 * (y * vp.width + x);
                int delta = 0;
                for (int c = 0; c < 3; c++)
                    delta += Math.abs((before == null ? background : before.get(offset + c) & 255) - (after.get(offset + c) & 255));
                if (delta >= 30) {
                    left = Math.min(left, x);
                    bottom = Math.min(bottom, y);
                    right = Math.max(right, x);
                    top = Math.max(top, y);
                    count++;
                }
            }
        }
        return new Bounds(left, bottom, right, top, count);
    }

    private static void save(Path output, String name, ByteBuffer pixels, Viewport vp) throws Exception {
        if (output == null)
            return;
        BufferedImage image = new BufferedImage(vp.width, vp.height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < vp.height; y++) {
            for (int x = 0; x < vp.width; x++) {
                int offset = 4 * (y * vp.width + x);
                image.setRGB(x, vp.height - 1 - y, (pixels.get(offset) & 255) << 16 | (pixels.get(offset + 1) & 255) << 8 | pixels.get(offset + 2) & 255);
            }
        }
        check(ImageIO.write(image, "png", output.resolve(name + ".png").toFile()), "PNG writer unavailable");
    }

    private record Target(String name, double x, double y, double z) {
    }

    private record Bounds(int left, int bottom, int right, int top, int count) {
        int width() {
            return right - left + 1;
        }

        int height() {
            return top - bottom + 1;
        }

        double centerY() {
            return (bottom + top) / 2.;
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final ViewpointLayer layer;
        private Viewport viewport;
        private final MapView view;

        Fixture(List<Target> targets, double width, boolean rotated) throws Exception {
            this(targets, width, rotated, false);
        }

        @SuppressWarnings("unchecked")
        Fixture(List<Target> targets, double width, boolean rotated, boolean relative) throws Exception {
            viewport = DisplayLayout.viewport(0, 17, 13, 640, 360, 386);
            Constructor<Camera> cameraConstructor = Camera.class.getDeclaredConstructor();
            cameraConstructor.setAccessible(true);
            Camera camera = cameraConstructor.newInstance();
            field(Camera.class, "cameraWidth").setDouble(camera, width);
            if (rotated) {
                camera.rotateDragRotation(Quat.createXY(0.3, -0.4));
                camera.setTranslation(5, -3);
            }
            Position viewpoint = new Position(TIME, 215, rotated ? 0.7 : 0, Math.PI / 2);
            view = MapView.create(camera, viewpoint, MapMode.Orthographic, Display.gridType, new MapScale[]{MapScale.ortho});
            JSONObject empty = new JSONObject().put("objects", new JSONArray());
            layer = new ViewpointLayer(new JSONObject().put("mode", "Heliosphere").put("location", empty)
                    .put("equatorial", new JSONObject(empty.toString()).put("relativeLongitude", relative)));
            field(AbstractLayer.class, "enabled").setBoolean(layer, true);
            ViewpointLayerOptionsExpert options = layer.getOptions().getEquatorialOptions();
            Map<SpaceObjectElement, PositionLoad> loads = (Map<SpaceObjectElement, PositionLoad>) field(ViewpointLayerOptionsExpert.class, "loads").get(options);
            Constructor<PositionResponse> response = PositionResponse.class.getDeclaredConstructor(Position.Cartesian[].class, Position.Cartesian[].class, long.class, long.class);
            response.setAccessible(true);
            Constructor<PositionLoad> load = PositionLoad.class.getDeclaredConstructor(PositionLoad.StatusReceiver.class, SpaceObject.class, Future.class);
            load.setAccessible(true);
            for (Target target : targets) {
                SpaceObjectElement element = options.getContainer().getElements().stream()
                        .filter(e -> target.name.equals(e.toString())).findFirst().orElseThrow();
                options.getContainer().selectElement(element);
                PositionResponse sample = response.newInstance(new Position.Cartesian[]{
                        new Position.Cartesian(TIME.milli, target.x, target.y, target.z),
                        new Position.Cartesian(END, target.x + 20, target.y + 12, target.z)
                }, null, TIME.milli, END);
                loads.put(element, load.newInstance((PositionLoad.StatusReceiver) _ -> {}, element.getTarget(), CompletableFuture.completedFuture(sample)));
            }
            layer.init();
        }

        private void clearViewport() {
            GL.glViewport(viewport.x, viewport.yGL, viewport.width, viewport.height);
            float background = Display.whiteBackground ? 1 : 0;
            GL.glClearColor(background, background, background, 1);
            GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
        }

        ByteBuffer scene() {
            clearViewport();
            Transform.ortho(viewport.aspect, view.cameraWidth(viewport), view.cameraTranslationX(), view.cameraTranslationY(), view.viewRotation());
            layer.render(view, viewport);
            return pixels();
        }

        ByteBuffer withNames() {
            layer.renderFloat(view, viewport);
            return pixels();
        }

        TextWidths textWidths(TimestampLayer timestamp) {
            viewport = Display.getViewport(0);
            int nameWidth = difference(scene(), withNames(), viewport, false).width();
            return new TextWidths(nameWidth, timestampInk(timestamp).width());
        }

        Bounds timestampInk(TimestampLayer timestamp) {
            viewport = Display.getViewport(0);
            clearViewport();
            timestamp.renderFloat(view, viewport);
            return difference(null, pixels(), viewport, false);
        }

        private ByteBuffer pixels() {
            ByteBuffer pixels = BufferUtils.newByteBuffer(4 * viewport.width * viewport.height);
            GL.glReadPixels(viewport.x, viewport.yGL, viewport.width, viewport.height, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
            return pixels;
        }

        @Override
        public void close() {
            layer.dispose();
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

}
