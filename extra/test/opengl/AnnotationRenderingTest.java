package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.helioviewer.jhv.annotation.Annotations;
import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.ConnectionLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.connect.LoadConnectivity;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONArray;
import org.json.JSONObject;

public final class AnnotationRenderingTest {

    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);
        Platform.init();
        Directories.createPersistentDirs();
        Log.init();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        DisplayController.setRenderRequestHandler(_ -> {});
        AngleRenderer renderer = AngleRenderer.pbuffer(384, 384);
        try {
            for (Layer layer : Layers.getLayers())
                layer.setEnabled(false);
            ConnectionLayer connections = Layers.getConnectionLayer();
            connections.setEnabled(true);
            JHVTime time = new JHVTime("2026-01-15T12:00:00");
            for (int[] size : new int[][]{{320, 256}, {256, 320}}) {
                GLRenderer.reshape(size[0], size[1]);
                Viewport vp = Display.getViewport(0);
                for (double distance : new double[]{2, 10, 215}) {
                    for (double tilt : new double[]{0, .35}) {
                        Position displayed = new Position(time, distance, tilt, tilt / 2);
                        DisplayController.setViewpointUpdate(_ -> displayed, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
                        DisplayController.cameraFromJson(new JSONObject().put("fov", 2 * Math.atan(.5 / distance))
                                .put("dragRotation", Quat.createAxisY(.6).toJson()));
                        for (MapMode mode : new MapMode[]{MapMode.HPC, MapMode.RadialWarp, MapMode.RectWarp, MapMode.Latitudinal}) {
                            Display.mode = mode;
                            double angle = 1.5 * Math.toDegrees(Math.asin(1 / distance));
                            MapScale scale = switch (mode) {
                                case HPC -> MapScale.hpc(angle, angle);
                                case Latitudinal -> MapScale.lati;
                                default -> MapScale.boxCoxRadial(2, 1);
                            };
                            MapView view = MapView.create(Display.getCamera(), displayed, mode, GridType.Carrington, new MapScale[]{scale});
                            String context = mode + " D=" + distance + " tilt=" + tilt + " " + vp.width + "x" + vp.height;
                            for (double z : new double[]{.8, .5 / distance, .9975 / distance, -.4, 1 / distance}) {
                                if (z == 1 / distance && tilt != 0)
                                    continue; // Exact tangency is constructed in the unrotated observer frame.
                                Vec3 point = point(z, .6);
                                Vec3 world = world(point, displayed);
                                Vec2 expected = screen(mode, world, point, distance, vp);
                                Vec2 projected = view.projectToScreen(vp, world);
                                check(Math.hypot(projected.x - expected.x, projected.y - expected.y) < 2e-12, context + " public projection");
                                boolean visible = mode == MapMode.Latitudinal || z >= 1 / distance;
                                ByteBuffer pixels = draw(view, vp, connections, List.of(world), List.of(), null);
                                checkPoint(pixels, vp, expected, visible, context + " point z=" + z);
                            }
                            Vec3 hidden0 = world(point(.5 / distance, .4), displayed);
                            Vec3 hidden1 = world(point(.5 / distance, .8), displayed);
                            ByteBuffer pixels = draw(view, vp, connections, List.of(), List.of(hidden0, hidden1), null);
                            check((count(pixels) > 0) == (mode == MapMode.Latitudinal), context + " hidden line");

                            Vec3 a = point(.8, Math.PI / 2 - .6), b = point(.8, Math.PI / 2 + .6);
                            List<Vec3> split = List.of(world(point(.8, Math.PI / 2 - .8), displayed), world(a, displayed),
                                    world(point(.5 / distance, Math.PI / 2), displayed), world(b, displayed),
                                    world(point(.8, Math.PI / 2 + .8), displayed));
                            pixels = draw(view, vp, connections, List.of(), split, null);
                            checkPoint(pixels, vp, screen(mode, world(a, displayed), a, distance, vp), true, context + " first strip");
                            checkPoint(pixels, vp, screen(mode, world(b, displayed), b, distance, vp), true, context + " second strip");
                            if (mode != MapMode.Latitudinal) {
                                Vec2 pa = screen(mode, world(a, displayed), a, distance, vp);
                                Vec2 pb = screen(mode, world(b, displayed), b, distance, vp);
                                check(near(pixels, vp, new Vec2((pa.x + pb.x) / 2, (pa.y + pb.y) / 2)) == 0, context + " hidden strip bridge");
                            }
                            if (tilt == 0) {
                                annotationChecks(view, vp, connections, context);
                            }
                        }
                    }
                }
            }
            check(!GLException.checkErrors("AnnotationRenderingTest"), "Annotation GL errors");
        } finally {
            Annotations.clear();
            renderer.destroy();
        }
        System.out.println("AnnotationRenderingTest passed");
    }

    private static void annotationChecks(MapView view, Viewport vp, ConnectionLayer connections, String context) {
        double distance = view.viewpoint().distance;
        MapMode mode = view.mode();
        for (double z : new double[]{.8, -.4}) {
            Vec3 point = point(z, .6);
            ByteBuffer pixels = draw(view, vp, connections, List.of(), List.of(), annotation("Cross", point, null));
            check((count(pixels) > 0) == (mode == MapMode.Latitudinal || z > 0), context + " cross z=" + z);
        }
        // A real elevated loop has hidden surface feet and a visible apex above the limb.
        // Keep the hidden feet spatially separated from the visible stroke at each distance.
        double longitude = distance == 2 ? Math.PI / 2 : 2 * Math.PI / 3;
        double center = Math.sqrt(3) / 2;
        Vec3 foot = new Vec3(center * Math.sin(longitude), .5, center * Math.cos(longitude));
        Vec3 otherFoot = new Vec3(foot.x, -.5, foot.z);
        Vec3 apex = new Vec3((center + .5) * Math.sin(longitude), 0, (center + .5) * Math.cos(longitude));
        ByteBuffer pixels = draw(view, vp, connections, List.of(), List.of(), annotation("Loop", foot, otherFoot));
        checkPoint(pixels, vp, screen(mode, apex, apex, distance, vp), true, context + " elevated loop apex");
        if (mode != MapMode.Latitudinal)
            check(near(pixels, vp, screen(mode, foot, foot, distance, vp)) == 0, context + " occulted loop foot");

        if (mode == MapMode.RectWarp || mode == MapMode.Latitudinal) {
            Vec3 seam = mode == MapMode.RectWarp ? new Vec3(0, .6, .8) : new Vec3(0, 0, -1);
            pixels = draw(view, vp, connections, List.of(), List.of(), annotation("Cross", seam, null));
            Vec2 location = screen(mode, seam, seam, distance, vp);
            check(near(pixels, vp, new Vec2(-.5 * vp.aspect, location.y)) > 0, context + " left seam");
            check(near(pixels, vp, new Vec2(.5 * vp.aspect, location.y)) > 0, context + " right seam");
            check(near(pixels, vp, new Vec2(0, location.y)) == 0, context + " seam bridge");
        }
    }

    private static JSONObject annotation(String type, Vec3 start, Vec3 end) {
        JSONObject item = new JSONObject().put("type", type).put("color", "Magenta").put("thickness", .008)
                .put("startPoint", start.toJson());
        if (end != null)
            item.put("endPoint", end.toJson());
        return new JSONObject().put("annotateables", new JSONArray().put(item));
    }

    private static ByteBuffer draw(MapView view, Viewport vp, ConnectionLayer connections, List<Vec3> points, List<Vec3> line, JSONObject annotation) {
        connections.setConnectivity(new LoadConnectivity.Connectivity(view.viewpoint().time, points, List.of(), List.of()));
        connections.setHCS(line);
        Annotations.fromJson(annotation);
        Layers.prerender();
        GL.glClearColor(0, 0, 0, 1);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
        GLRenderer.renderScene(view);
        ByteBuffer pixels = BufferUtils.newByteBuffer(4 * vp.width * vp.height);
        GL.glReadPixels(0, 0, vp.width, vp.height, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
        return pixels;
    }

    private static Vec3 point(double z, double angle) {
        double r = Math.sqrt(1 - z * z);
        return new Vec3(r * Math.sin(angle), r * Math.cos(angle), z);
    }

    // Explicit inverse axis rotations, independent of Position.toQuat().
    private static Vec3 world(Vec3 point, Position displayed) {
        double y = Math.cos(displayed.lat) * point.y + Math.sin(displayed.lat) * point.z;
        double z = -Math.sin(displayed.lat) * point.y + Math.cos(displayed.lat) * point.z;
        return new Vec3(Math.cos(displayed.lon) * point.x - Math.sin(displayed.lon) * z, y,
                Math.sin(displayed.lon) * point.x + Math.cos(displayed.lon) * z);
    }

    private static Vec2 screen(MapMode mode, Vec3 world, Vec3 point, double distance, Viewport vp) {
        double x, y;
        if (mode == MapMode.Latitudinal) {
            x = Math.atan2(world.x, world.z) / (2 * Math.PI);
            y = Math.asin(world.y / world.length()) / Math.PI;
        } else if (mode == MapMode.HPC) {
            double angle = 1.5 * Math.asin(1 / distance);
            x = Math.atan2(point.x, distance - point.z) / (2 * angle);
            y = Math.atan2(point.y, Math.hypot(point.x, distance - point.z)) / (2 * angle);
        } else {
            double px = distance * point.x / (distance - point.z), py = distance * point.y / (distance - point.z);
            if (mode == MapMode.RadialWarp)
                return new Vec2(px / 4, py / 4); // Linear radial scale with outer radius 2.
            double angle = Math.atan2(-px, py);
            if (angle < 0)
                angle += 2 * Math.PI;
            x = angle / (2 * Math.PI) - .5;
            y = Math.hypot(px, py) / 2 - .5;
        }
        return new Vec2(x * vp.aspect, y);
    }

    private static boolean colored(ByteBuffer pixels, int offset) {
        // Both consumer colors have red > green, including antialiased stroke edges.
        return (pixels.get(offset) & 255) > (pixels.get(offset + 1) & 255);
    }

    private static int count(ByteBuffer pixels) {
        int count = 0;
        for (int i = 0; i < pixels.capacity(); i += 4)
            if (colored(pixels, i))
                count++;
        return count;
    }

    private static int near(ByteBuffer pixels, Viewport vp, Vec2 point) {
        double width = Display.mode == MapMode.RadialWarp ? 1.1 : 1;
        int cx = (int) Math.round(vp.width / 2. + point.x * vp.height / width - .5);
        int cy = (int) Math.round(vp.height / 2. + point.y * vp.height / width - .5);
        int count = 0;
        for (int y = Math.max(0, cy - 3); y <= Math.min(vp.height - 1, cy + 3); y++)
            for (int x = Math.max(0, cx - 3); x <= Math.min(vp.width - 1, cx + 3); x++)
                if (colored(pixels, 4 * (y * vp.width + x)))
                    count++;
        return count;
    }

    private static void checkPoint(ByteBuffer pixels, Viewport vp, Vec2 point, boolean visible, String context) {
        check(visible ? near(pixels, vp, point) > 0 : count(pixels) == 0, context);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
