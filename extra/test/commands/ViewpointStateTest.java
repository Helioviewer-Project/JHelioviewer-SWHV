package org.helioviewer.jhv.layers;

import java.lang.reflect.Method;
import java.util.List;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.state.State;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.UiThread;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ViewpointStateTest {

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
            UiThread.postAndWait(ViewpointStateTest::checkModeSwitching);
            UiThread.postAndWait(ViewpointStateTest::checkSkyAndSpiralSettings);
        } finally {
            UiThread.postAndWait(() -> {
                Layers.getViewpointLayer().setCameraMode(ViewpointLayerOptions.CameraMode.Observer);
                Player.pause();
                ViewState.setRefresh(false);
            });
        }
        System.out.println("ViewpointStateTest passed");
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
        JSONObject saved = UiThread.postAndWait(ViewpointStateTest::sessionJson);
        JSONObject before = viewpointEntry(saved);
        check(before.getBoolean("enabled") == enabled, "Saved enabled flag");
        check(before.getJSONObject("data").getString("mode").equals(mode), "Saved legacy mode");
        JSONObject spiral = before.getJSONObject("data").getJSONObject("equatorial");
        check(spiral.getBoolean("spiral") == !enabled && spiral.getInt("spiralSpeed") == (enabled ? 1300 : 850), "Spiral settings not saved");
        UiThread.postAndWait(() -> State.load(null, saved));
        // State applies playback after its asynchronous layer-load completion callback.
        UiThread.postAndWait(() -> {});
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
        State.load(null, saved);
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
            if (layer.getString("className").equals(ViewpointLayer.class.getName()))
                return layer;
        }
        throw new AssertionError("Missing saved viewpoint");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

}
