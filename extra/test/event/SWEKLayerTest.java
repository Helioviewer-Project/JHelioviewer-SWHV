package org.helioviewer.jhv.plugins.swek;

import java.awt.EventQueue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.app.state.State;
import org.helioviewer.jhv.event.EventBatch;
import org.helioviewer.jhv.event.EventMetadata;
import org.helioviewer.jhv.event.EventParameter;
import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.timelines.TimelineLayers;

import org.json.JSONObject;

public final class SWEKLayerTest {
    private static final SWEKSession session = new SWEKSession(new SWEKCatalog(0, List.of(), List.of()));
    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        checkLabels();
        checkActiveEvents();
        session.close();
        EventQueue.invokeAndWait(() -> {
            try { checkRestoration(); } catch (Exception e) { throw new AssertionError(e); }
        });
        System.out.println("SWEKLayerTest passed");
    }

    private static void checkRestoration() throws Exception {
        Settings.setProperty("plugins.SWEKPlugin.active", "false");
        PluginManager.setGUIEnabled(false);
        SWEKPlugin plugin = new SWEKPlugin();
        PluginManager.addPlugin(plugin);
        PluginManager.setActive(plugin, true);
        SWEKLayer original = (SWEKLayer) Layers.getLayers().stream().filter(layer -> layer instanceof SWEKLayer).findFirst().orElseThrow();
        Field sessionField = SWEKLayer.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        Object active = sessionField.get(original);
        EventTimelineLayer timeline = plugin.getTimelineLayer();
        PluginManager.addPlugin(new SWEKPlugin());
        check(PluginManager.getPlugin(SWEKPlugin.class) == plugin, "duplicate registration replaced the active owner");
        Method factory = State.class.getDeclaredMethod("readLayer", JSONObject.class);
        factory.setAccessible(true);
        JSONObject state = new JSONObject().put("className", SWEKLayer.class.getName()).put("data", new JSONObject().put("icons", false));
        SWEKLayer restored = (SWEKLayer) factory.invoke(null, state);
        check(restored != null && !restored.isIcons() && !original.isEnabled(), "restoration keeps settings and immediately detaches the old scene layer");
        check(factory.invoke(null, new JSONObject(state.toString()).put("type", "unknown")) == null, "Unknown type used the legacy class name");
        check(factory.invoke(null, new JSONObject().put("type", "events")) == null, "Missing layer data was accepted");
        restored.setEnabled(true);
        List<Layer> layers = new ArrayList<>(Layers.getLayers());
        layers.remove(original);
        layers.add(restored);
        Layers.restore(layers);
        check(sessionField.get(restored) == active, "state restore retains the active session");
        Method timelineFactory = State.class.getDeclaredMethod("readTimeline", JSONObject.class);
        timelineFactory.setAccessible(true);
        Object restoredTimeline = timelineFactory.invoke(null, new JSONObject().put("className", EventTimelineLayer.class.getName()).put("data", new JSONObject()));
        check(restoredTimeline == timeline, "timeline restoration reuses the plugin-owned instance");
        check(timelineFactory.invoke(null, new JSONObject().put("type", "events").put("data", new JSONObject())) == timeline,
                "New timeline type replaced the plugin-owned instance");
        check(timelineFactory.invoke(null, new JSONObject().put("type", "unknown").put("data", new JSONObject())) == null,
                "Unknown timeline type was accepted");
        TimelineLayers.restore(List.of(timeline));
        check(TimelineLayers.get().stream().filter(layer -> layer instanceof EventTimelineLayer).count() == 1, "restoration does not duplicate timeline layers");
        layers.remove(restored);
        Layers.restore(layers);
        check(!restored.isEnabled(), "state omitting SWEK detaches the scene adapter");
        PluginManager.setActive(plugin, false);
        check(plugin.getTimelineLayer() == null, "plugin deactivation drops the restored timeline");
        try {
            plugin.restoreLayer(new JSONObject());
            throw new AssertionError("inactive plugin exposes a session");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("inactive"), "inactive restore is rejected at the boundary");
        }
    }

    private static void checkActiveEvents() throws Exception {
        EventQueue.invokeAndWait(() -> {
            try {
                run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void run() throws Exception {
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Test", ""), "test", "Test",
                new SWEK.Source("test", List.of(), null, Map.of()), "test", List.of());
        Method publish = SWEKSession.class.getDeclaredMethod("replaceEvents", EventBatch.class);
        publish.setAccessible(true);
        SWEKLayer layer = new SWEKLayer(session);
        layer.setEnabled(true);
        SolarEvent first = new SolarEvent(supplier, 1, 100, 200);
        SolarEvent second = new SolarEvent(supplier, 2, 150, 300);
        SolarEvent third = new SolarEvent(supplier, 3, 400, 500);
        List<SolarEvent.Link> links = List.of(new SolarEvent.Link(1, 2), new SolarEvent.Link(2, 3));
        publish.invoke(session, new EventBatch(1, List.of(first, second, third), links));
        check(layer, 150, first);
        List<SWEKLayer.ActiveEvent> active = layer.activeEvents(150);
        session.highlight(active.getFirst().relatedEvents());
        if (active != layer.activeEvents(150) || !active.getFirst().relatedEvents().isHighlighted())
            throw new AssertionError("highlight remains live without rebuilding selections");
        session.highlight(null);
        check(layer, 200, first);
        check(layer, 201, second);
        check(layer, 300, second);
        check(layer, 301, null);
        check(layer, 400, third);
        check(layer, 150, first);
        SolarEvent refreshed = new SolarEvent(supplier, 1, 100, 200);
        publish.invoke(session, new EventBatch(2, List.of(refreshed), links));
        check(layer, 150, second); // Refreshed observation moves to the end of insertion order.
        check(layer, 100, refreshed);
        layer.timeRangeChanged(100, 500);
        check(layer, 100, refreshed);
        layer.setEnabled(false);
        SolarEvent replacement = new SolarEvent(supplier, 1, 100, 200);
        publish.invoke(session, new EventBatch(3, List.of(replacement), links));
        layer.setEnabled(true);
        check(layer, 100, replacement);
        Method remove = SWEKSession.class.getDeclaredMethod("removeSupplier", SWEKSupplier.class);
        remove.setAccessible(true);
        remove.invoke(session, supplier);
        check(layer, 100, null);
        layer.setEnabled(false);
    }

    private static void check(SWEKLayer layer, long time, SolarEvent expected) {
        List<SWEKLayer.ActiveEvent> active = layer.activeEvents(time);
        if (active != layer.activeEvents(time)) throw new AssertionError("cache reuse");
        if (expected == null) {
            if (!active.isEmpty()) throw new AssertionError("unexpected active event at " + time);
            return;
        }
        if (active.size() != 1) throw new AssertionError("group count");
        SWEKLayer.ActiveEvent item = active.getFirst();
        SolarEvent actual = item.event();
        if (item.relatedEvents() != session.getRelatedEvents(expected.getUniqueID())) throw new AssertionError("group identity");
        if (actual != expected) throw new AssertionError("selection at " + time);
    }

    private static void checkLabels() {
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Test", ""), "test", "Test",
                new SWEK.Source("test", List.of(), null, Map.of()), "test", List.of());
        EventMetadata.Builder metadata = new EventMetadata.Builder(supplier, true);
        metadata.add("event_title", "Title", "Excluded title", true);
        metadata.add("event_description", "Description", "Excluded description", true);
        metadata.add("link", "Link", "https://example.com/event", true);
        metadata.add("speed", "Speed", "679.9", true);
        metadata.add("text", "Text", "https:// is not a valid URL", true);
        metadata.add("hidden", "Hidden", "Hidden detail", false);
        SolarEvent event = new SolarEvent(supplier, 1, 0, 1, null, SolarEvent.CMEParameters.DEFAULT, metadata.build());
        metadata.add("later", "Later", "Not in the built event", true);
        List<String> expected = List.of("Speed : 6.799E2", "Text : https:// is not a valid URL");
        check(SWEKData.visibleParameterLines(event).equals(expected), "hover labels preserve order, formatting and exclusions");
        check(event.getAllEventParameters().length == 6 && event.getVisibleEventParameters().length == 5, "detail lists retain URLs and hidden metadata");
        EventParameter link = event.getVisibleEventParameters()[2];
        check(link.isUrl(), "URL classification");
        check("https://example.com/event".equals(link.getLink()), "URL values link to themselves");
        EventParameter region = new EventParameter("ar_noaanum", "Region", "13664");
        check(region.getLink() != null && region.getLink().contains("NOAA%2013664") && !region.isUrl(), "generated region links remain distinct from URL values");
    }

    public static void checkPropagation(SolarEvent event) {
        check(Double.isFinite(SWEKData.cactusDistance(event, event.end)), "finite CME propagation distance");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
