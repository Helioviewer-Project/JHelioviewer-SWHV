package org.helioviewer.jhv.layers;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.state.State;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.UiThread;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ViewpointBehaviorTest {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.io.tmpdir", System.getProperty("user.home"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        try {
            UiThread.postAndWait(() -> {
                DisplayController.setRenderRequestHandler(_ -> {});
                Layers.restore(List.of());
                check(Layers.getViewpointLayer().getCameraMode() == ViewpointLayerOptions.CameraMode.Observer, "New session mode");
                check(DisplayController.getViewpointUpdate() == UpdateViewpoint.observer, "New session strategy");
            });
            for (String mode : List.of("ObserverAt1au", "Location", "Heliosphere")) {
                for (boolean enabled : new boolean[]{false, true})
                    roundTrip(mode, enabled);
            }
            UiThread.postAndWait(ViewpointBehaviorTest::checkModeSwitching);
            UiThread.postAndWait(ViewpointBehaviorTest::checkTracking);
            UiThread.postAndWait(ViewpointBehaviorTest::checkSkyAndSpiralSettings);
        } finally {
            UiThread.postAndWait(() -> {
                Layers.getViewpointLayer().setCameraMode(ViewpointLayerOptions.CameraMode.Observer);
                Player.pause();
                ViewState.setRefresh(false);
            });
        }
        System.out.println("ViewpointBehaviorTest passed");
    }

    private static void roundTrip(String mode, boolean enabled) throws Exception {
        JSONObject custom = new JSONObject().put("objects", new JSONArray()).put("syncInterval", false)
                .put("startTime", "2025-01-01T00:00:00").put("endTime", "2025-01-02T00:00:00")
                .put("planets", false).put("stars", false);
        JSONObject legacy = new JSONObject().put("mode", mode).put("location", custom)
                .put("equatorial", new JSONObject(custom.toString()).put("relativeLongitude", true));
        UiThread.postAndWait(() -> {
            ViewpointLayer layer = new ViewpointLayer(legacy);
            layer.getOptions().getEquatorialOptions().setSpiral(!enabled);
            layer.getOptions().getEquatorialOptions().setSpiralSpeed(enabled ? 1300 : 850);
            layer.setEnabled(enabled);
            Layers.restore(List.of(layer));
            Display.getCamera().setTranslation(0.25, -0.5);
            DisplayController.rotateView90("Z");
        });
        JSONObject saved = UiThread.postAndWait(ViewpointBehaviorTest::sessionJson);
        JSONObject before = viewpointEntry(saved);
        check(before.getBoolean("enabled") == enabled, "Saved enabled flag");
        check(before.getJSONObject("data").getString("mode").equals(mode), "Saved legacy mode");
        JSONObject spiral = before.getJSONObject("data").getJSONObject("equatorial");
        check(spiral.getBoolean("spiral") == !enabled && spiral.getInt("spiralSpeed") == (enabled ? 1300 : 850), "Spiral settings not saved");
        CompletableFuture<Boolean> completion = new CompletableFuture<>();
        UiThread.postAndWait(() -> State.load((success, message, output) ->
                completion.complete(success && message.equals("State loaded.") && output == null), saved));
        // State applies playback after its asynchronous layer-load completion callback.
        UiThread.postAndWait(() -> {});
        check(completion.get(5, TimeUnit.SECONDS), "State restoration changed its completion payload");
        JSONObject after = UiThread.postAndWait(() -> {
            ViewpointLayer layer = Layers.getViewpointLayer();
            ViewpointLayerOptions.CameraMode expected = enabled ? ViewpointLayerOptions.CameraMode.valueOf(mode) : ViewpointLayerOptions.CameraMode.Observer;
            check(layer.getCameraMode() == expected, "Restored effective mode: " + mode + "/" + enabled);
            check(layer.isEnabled() == enabled, "Restored enabled flag");
            ViewpointLayerOptionsExpert equatorial = layer.getOptions().getEquatorialOptions();
            check(equatorial.isSpiral() == !enabled && equatorial.getSpiralSpeedValue() == (enabled ? 1300 : 850), "Spiral settings not restored");
            if (!enabled)
                check(DisplayController.getViewpointUpdate() == UpdateViewpoint.observer, "Disabled legacy mode changed observer");
            return viewpointEntry(sessionJson());
        });
        check(before.similar(after), "Camera or per-mode settings did not round-trip: " + mode + "/" + enabled + "\n" + before + "\n" + after);
    }

    private static void checkTracking() {
        UpdateViewpoint originalUpdate = DisplayController.getViewpointUpdate();
        boolean originalTracking = DisplayController.getTrackingMode();
        JSONObject originalCamera = DisplayController.cameraToJson();
        AtomicInteger updates = new AtomicInteger();
        AtomicReference<Position> result = new AtomicReference<>();
        AtomicReference<Position> rendered = new AtomicReference<>();
        try {
            DisplayController.setTrackingMode(false);
            DisplayController.setRenderRequestHandler(rendered::set);
            DisplayController.setViewpointUpdate(time -> {
                updates.incrementAndGet();
                Position position = new Position(time, Sun.MeanEarthDistance, 0, 0);
                result.set(position);
                return position;
            }, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
            check(updates.get() == 1, "Installing a strategy must evaluate it once");
            JHVTime next = new JHVTime(Player.getTime().milli + 1000);
            DisplayController.timeChanged(next);
            check(updates.get() == 2 && result.get().time == next, "Untracked time change did not reach the strategy");
            DisplayController.setTrackingMode(true);
            check(updates.get() == 3, "Enabling tracking did not refresh the viewpoint");
            Position frozen = result.get();
            DisplayController.timeChanged(next);
            DisplayController.display();
            check(updates.get() == 3 && rendered.get() == frozen, "Tracking did not freeze the logical viewpoint");
            DisplayController.setTrackingMode(true);
            check(updates.get() == 3, "Unchanged tracking mode refreshed the viewpoint");
            DisplayController.refreshCamera();
            check(updates.get() == 4 && rendered.get() == result.get(), "Explicit refresh must update while tracking");
            DisplayController.zoomMiniToFit();
            check(updates.get() == 4, "Mini fitting evaluated the main viewpoint strategy");
            DisplayController.display(frozen);
            check(rendered.get() == frozen, "Explicit display lost the supplied viewpoint");
            DisplayController.display();
            check(rendered.get() == result.get(), "Explicit display replaced the logical viewpoint");
            DisplayController.resetCamera();
            check(updates.get() == 5, "Camera reset must evaluate the strategy once");
            DisplayController.setTrackingMode(false);
            check(updates.get() == 6, "Disabling tracking did not refresh the viewpoint");
            DisplayController.setTrackingMode(false);
            check(updates.get() == 6, "Unchanged untracked mode refreshed the viewpoint");
        } finally {
            DisplayController.setViewpointUpdate(originalUpdate, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
            DisplayController.setTrackingMode(originalTracking);
            DisplayController.cameraFromJson(originalCamera);
            DisplayController.setRenderRequestHandler(_ -> {});
        }
    }

    private static void checkSkyAndSpiralSettings() {
        for (boolean planets : new boolean[]{false, true}) {
            for (boolean stars : new boolean[]{false, true}) {
                JSONObject empty = new JSONObject().put("objects", new JSONArray());
                ViewpointLayerOptions options = new ViewpointLayerOptions(new JSONObject().put("location", empty).put("equatorial", empty));
                options.getLocationOptions().setPlanets(planets);
                options.getLocationOptions().setStars(stars);
                JSONObject saved = new JSONObject();
                options.serialize(saved);
                ViewpointLayerOptions restored = new ViewpointLayerOptions(saved);
                check(restored.getLocationOptions().isPlanets() == planets && restored.getLocationOptions().isStars() == stars,
                        "Planets/Stars settings did not round-trip");
            }
        }
        JSONObject empty = new JSONObject().put("objects", new JSONArray());
        ViewpointLayerOptions defaults = new ViewpointLayerOptions(new JSONObject().put("location", empty).put("equatorial", empty));
        check(defaults.getLocationOptions().isPlanets() && defaults.getLocationOptions().isStars(), "Missing sky settings changed defaults");
        check(!defaults.getEquatorialOptions().isSpiral() && defaults.getEquatorialOptions().getSpiralSpeedValue() == 500, "Missing Spiral settings changed defaults");
        for (int speed : new int[]{-1, 100000}) {
            JSONObject equatorial = new JSONObject().put("objects", new JSONArray()).put("spiralSpeed", speed);
            ViewpointLayerOptions bounded = new ViewpointLayerOptions(new JSONObject().put("location", empty).put("equatorial", equatorial));
            check(bounded.getEquatorialOptions().getSpiralSpeedValue() == Math.clamp(speed, ViewpointLayerOptionsExpert.MIN_SPEED_SPIRAL, ViewpointLayerOptionsExpert.MAX_SPEED_SPIRAL),
                    "Restored Spiral speed outside slider range");
        }
    }

    private static void checkModeSwitching() {
        ViewpointLayer layer = Layers.getViewpointLayer();
        layer.setCameraMode(ViewpointLayerOptions.CameraMode.ObserverAt1au);
        check(DisplayController.getViewpointUpdate() == UpdateViewpoint.observerAt1au, "Observer at 1au strategy");
        JSONObject camera = DisplayController.cameraToJson();
        layer.setCameraMode(ViewpointLayerOptions.CameraMode.Observer);
        check(!layer.isEnabled() && DisplayController.getViewpointUpdate() == UpdateViewpoint.observer, "Observer is not equivalent to off");
        check(camera.similar(DisplayController.cameraToJson()), "Observer reset camera transform");
        layer.getOptions().setCameraMode(ViewpointLayerOptions.CameraMode.Location, DisplayController.ViewpointApplyMode.RESET);
        layer.getOptions().getLocationOptions().setTimeSelection(1735689600000L, 1735776000000L);
        check(DisplayController.getViewpointUpdate() == UpdateViewpoint.observer, "Inactive options changed viewpoint");
        layer.setCameraMode(ViewpointLayerOptions.CameraMode.Location);
        check(layer.isEnabled() && DisplayController.getViewpointUpdate() instanceof UpdateViewpoint.Location, "Location activation");
        layer.setCameraMode(ViewpointLayerOptions.CameraMode.Observer);
        JSONObject saved = sessionJson();
        State.load(Commands.Completion.NONE, saved);
        check(Layers.getViewpointLayer().getCameraMode() == ViewpointLayerOptions.CameraMode.Observer, "Explicit Observer restore");
        layer = Layers.getViewpointLayer();
        layer.setCameraMode(ViewpointLayerOptions.CameraMode.ObserverAt1au);
        Layers.restore(List.of());
        Player.resetToMaster();
        check(DisplayController.getViewpointUpdate() == UpdateViewpoint.observer, "Missing viewpoint retained previous strategy");
        ViewpointLayer oldWithoutMode = new ViewpointLayer(new JSONObject().put("location", new JSONObject().put("objects", new JSONArray())));
        oldWithoutMode.setEnabled(true);
        check(oldWithoutMode.getCameraMode() == ViewpointLayerOptions.CameraMode.Location, "Legacy enabled state missing mode changed its default");
        oldWithoutMode.setEnabled(false);
    }

    private static JSONObject sessionJson() {
        try {
            Method method = State.class.getDeclaredMethod("toJson");
            method.setAccessible(true);
            return ((JSONObject) method.invoke(null)).getJSONObject("org.helioviewer.jhv.state");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static JSONObject viewpointEntry(JSONObject session) {
        for (Object entry : session.getJSONArray("layers")) {
            JSONObject layer = (JSONObject) entry;
            if (layer.getString("type").equals("viewpoint"))
                return layer;
        }
        throw new AssertionError("Missing saved viewpoint");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

}
