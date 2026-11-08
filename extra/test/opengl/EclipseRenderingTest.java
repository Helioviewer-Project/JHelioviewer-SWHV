package org.helioviewer.jhv.opengl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.Stars;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.ViewpointLayer;
import org.helioviewer.jhv.layers.ViewpointLayerOptions;
import org.helioviewer.jhv.layers.ViewpointLayerOptionsExpert;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectElement;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.wcs.ImageGeometry;
import org.helioviewer.jhv.wcs.WcsSolution;

import org.json.JSONArray;
import org.json.JSONObject;

public final class EclipseRenderingTest {

    private static final int SIZE = 320;
    private static final SpaceObject MOON = SpaceObject.get("Moon");
    private static final JHVTime TIME = new JHVTime("2024-04-08T18:18:00");

    @FunctionalInterface
    private interface Mask {
        boolean contains(double x, double y);
    }

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        DisplayController.setRenderRequestHandler(_ -> {});
        for (int context = 0; context < 2; context++) {
            AngleRenderer renderer = AngleRenderer.pbuffer(SIZE, SIZE);
            try {
                GLRenderer.reshape(SIZE, SIZE);
                cap();
                outside();
                crossing();
                imageMapping();
                background();
                proba2();
                ordering();
                nearbyDisk();
                starsFirst();
                planetLoads();
                clearing();
                check(!GLException.checkErrors("EclipseRenderingTest"), "Moon rendering GL errors");
            } finally {
                renderer.destroy();
            }
        }
        System.out.println("PASS: foreground eclipses, background markers, stars first, back-to-front planets, limb crossing, rotation, planet loading, clearing, colors and context recreation");
    }

    private static void cap() throws Exception {
        double distance = .55, sunDistance = 215;
        double radius = sunDistance * MOON.getRadius() / Math.sqrt(distance * distance - MOON.getRadius() * MOON.getRadius());
        double minimumZ = Math.sqrt(1 - radius * radius);
        try (Fixture fixture = new Fixture(new double[]{0, 0, sunDistance}, new double[]{0, 0, distance}, "Earth")) {
            for (boolean white : new boolean[]{false, true}) {
                for (Quat drag : new Quat[]{Quat.ZERO, Quat.createAxisY(Math.PI / 6), Quat.createAxisY(Math.PI / 3),
                        Quat.Y90, Quat.createAxisY(2 * Math.PI / 3), Quat.createAxisY(Math.PI), Quat.createXY(.4, .7)}) {
                    Vec3 axis = drag.rotateVector(Vec3.ZAxis);
                    Mask mask = (x, y) -> {
                        double r2 = x * x + y * y;
                        return r2 < 1 && axis.x * x + axis.y * y + axis.z * Math.sqrt(1 - r2) >= minimumZ;
                    };
                    fixture.compare(fixture.draw(drag, 4, 0, 0, white), mask, 4, 0, 0, drag, white, "rotated lunar cap");
                }
            }
        }
    }

    private static void outside() throws Exception {
        double distance = .6, center = 1.95, sunDistance = 215;
        double angle = Math.atan(center / sunDistance);
        double[] sun = {0, 0, sunDistance};
        double[][] axes = axes(sun);
        double[] moon = new double[3];
        for (int i = 0; i < 3; i++)
            moon[i] = distance * (Math.cos(angle) * axes[0][i] + Math.sin(angle) * axes[2][i]);
        try (Fixture fixture = new Fixture(sun, moon, "Earth")) {
            for (double rotation : new double[]{0, Math.PI / 3, Math.PI / 2, 2 * Math.PI / 3}) {
                Quat drag = Quat.createAxisY(rotation);
                double cos = Math.cos(rotation);
                Mask mask = (x, y) -> {
                    if (x * x + y * y < 1 || Math.abs(cos) < 1e-12)
                        return false;
                    double sourceX = x / cos;
                    return sourceX * sourceX + y * y >= 1 && fixture.onMoon(sourceX, y);
                };
                fixture.compare(fixture.draw(drag, 6, 0, 0, false), mask, 6, 0, 0, drag, false, "outside lunar disk");
            }
            Quat edgeOn = new Quat(.5, .5, .5, .5);
            check(edgeOn.rotateVector(Vec3.ZAxis).z == 0, "edge-on test rotation is not exact");
            fixture.compare(fixture.draw(edgeOn, 6, 0, 0, false), (x, y) -> false, 6, 0, 0, edgeOn, false, "edge-on lunar disk");
        }
    }

    private static void crossing() throws Exception {
        double sunDistance = 215, distance = .55;
        double angle = Math.atan(1 / sunDistance);
        double[] sun = {0, 0, sunDistance};
        double[][] axes = axes(sun);
        double[] moon = new double[3];
        for (int i = 0; i < 3; i++)
            moon[i] = distance * (Math.cos(angle) * axes[0][i] + Math.sin(angle) * axes[2][i]);
        try (Fixture fixture = new Fixture(sun, moon, "Earth")) {
            for (boolean white : new boolean[]{false, true}) {
                for (double angleY : new double[]{-Math.PI / 3, 0, Math.PI / 6, Math.PI / 3, Math.PI / 2, 2 * Math.PI / 3}) {
                    Quat drag = Quat.createAxisY(angleY);
                    double cos = Math.cos(angleY), sin = Math.sin(angleY);
                    Mask mask = (x, y) -> {
                        double r2 = x * x + y * y;
                        if (r2 < 1) {
                            double z = Math.sqrt(1 - r2);
                            if (sin * x + cos * z >= 0)
                                return fixture.onMoon(cos * x - sin * z, y);
                            if (cos <= 0)
                                return false;
                        }
                        return Math.abs(cos) > 1e-12 && fixture.onMoon(x / cos, y);
                    };
                    fixture.compare(fixture.draw(drag, 6, 0, 0, white), mask, 6, 0, 0, drag, white, "foreground Moon crossing solar limb");
                }
            }
        }
    }

    private static void imageMapping() throws Exception {
        int textureSize = 512;
        double angle = Math.atan(1 / 215.);
        double[] sun = {0, 0, 215};
        double[][] axes = axes(sun);
        double[] moon = new double[3];
        for (int i = 0; i < 3; i++)
            moon[i] = .55 * (Math.cos(angle) * axes[0][i] + Math.sin(angle) * axes[2][i]);
        GLStreamingTexture2D texture = new GLStreamingTexture2D(GLTexture.Unit.ZERO, GL.NEAREST);
        GLStreamingTexture2D difference = new GLStreamingTexture2D(GLTexture.Unit.TWO, GL.NEAREST);
        GLTexture lut = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.ONE);
        GLTexture mask = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.THREE);
        try (Fixture fixture = new Fixture(sun, moon, "Earth")) {
            byte[] samples = new byte[textureSize * textureSize];
            for (int row = 0; row < textureSize; row++)
                for (int col = 0; col < textureSize; col++)
                    if (fixture.onMoon(8 * (col + .5) / textureSize - 4, 4 - 8 * (row + .5) / textureSize))
                        samples[row * textureSize + col] = (byte) 255;
            ImageBuffer image = ImageBuffer.fromBytes(textureSize, textureSize, samples);
            texture.upload(image);
            difference.upload(image);
            lut.upload2D(GLTexture.Format.RGBA8, 256, 1, GL.NEAREST, LUT.gray().rgba());
            mask.upload2D(GLTexture.Format.R8, 1, 1, GL.NEAREST, BufferUtils.newByteBuffer(1).put(0, (byte) 255));
            ImageGeometry geometry = new ImageGeometry(textureSize, textureSize,
                    new WcsSolution(WcsSolution.Projection.TAN, new float[6], 215),
                    new Mat2(8. / textureSize, 0, 0, -8. / textureSize), new Vec2(textureSize / 2., textureSize / 2.));
            Position source = new Position(TIME, 215, 0, 0);
            for (Quat drag : new Quat[]{Quat.createAxisY(-Math.PI / 3), Quat.createAxisY(-Math.PI / 6), Quat.ZERO,
                    Quat.createAxisY(Math.PI / 6), Quat.createAxisY(Math.PI / 3), Quat.createXY(.4, .7)}) {
                ByteBuffer overlay = fixture.draw(drag, 6, 0, 0, false);
                GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
                GLSLSphereShader.render();
                mask.bind();
                difference.bind();
                texture.bind();
                lut.bind();
                GLSLImageShader.bindDisplay(new float[]{1, 1, 1, 1}, 0, 0, 0, 0, 0, 1, 1, 1,
                        0, 0, 0, 0, 0, 0, Float.POSITIVE_INFINITY, 0, 0, 100, 0, 1, 0);
                GLSLImageShader.render(MapMode.Orthographic, Quat.rotate(drag, source.toQuat()), source,
                        geometry, source, geometry, source, Mat2.IDENTITY, 0, 0, false);
                ByteBuffer rendered = BufferUtils.newByteBuffer(4 * SIZE * SIZE);
                GL.glReadPixels(0, 0, SIZE, SIZE, GL.RGBA, GL.UNSIGNED_BYTE, rendered);
                int moonPixels = 0;
                for (int row = 2; row < SIZE - 2; row++) {
                    for (int col = 2; col < SIZE - 2; col++) {
                        int expected = rendered.get(4 * (row * SIZE + col)) & 255;
                        boolean edge = false;
                        for (int dy = -2; dy <= 2; dy++)
                            for (int dx = -2; dx <= 2; dx++)
                                edge |= (rendered.get(4 * ((row + dy) * SIZE + col + dx)) & 255) != expected;
                        if (edge || expected != 0 && expected < 250)
                            continue;
                        boolean onMoon = expected >= 250;
                        if (onMoon)
                            moonPixels++;
                        check(((overlay.get(4 * (row * SIZE + col)) & 255) != 0) == onMoon,
                                "eclipse fill differs from image silhouette at " + col + "," + row + " drag " + drag.toJson());
                    }
                }
                check(moonPixels > 10, "image mapping fixture lost the Moon silhouette");
            }
        } finally {
            mask.delete();
            lut.delete();
            difference.delete();
            texture.delete();
        }
    }

    private static void background() throws Exception {
        double distance = 430, sunDistance = 215, center = 2;
        double angle = Math.atan(center / sunDistance);
        double[] sun = {0, 0, sunDistance};
        double[][] axes = axes(sun);
        double[] moon = new double[3];
        for (int i = 0; i < 3; i++)
            moon[i] = distance * (Math.cos(angle) * axes[0][i] + Math.sin(angle) * axes[2][i]);
        try (Fixture fixture = new Fixture(sun, moon, "Earth")) {
            double width = 16 * sunDistance * MOON.getRadius() / distance;
            ByteBuffer pixels = fixture.draw(Quat.ZERO, width, center, 0, false);
            int lit = 0;
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    int value = pixels.get(4 * (row * SIZE + col)) & 255;
                    if (value == 0)
                        continue;
                    lit++;
                    check(Math.hypot(col + .5 - SIZE / 2., row + .5 - SIZE / 2.) <= 4, "background Moon entered the eclipse renderer");
                }
            }
            check(lit > 0, "background Moon lost its normal planet marker");
        }
        try (Fixture fixture = new Fixture(sun, new double[]{0, 0, distance}, "Earth")) {
            fixture.empty("background Moon occulted by Sun");
        }
    }

    private static void proba2() throws Exception {
        double[] sun = {-115645852.2213014, 89916853.33968577, 38969502.99897306};
        double[] moon = {-280525.84345485264, 216104.9598872855, 92950.98825861335};
        for (int i = 0; i < 3; i++) {
            sun[i] *= Sun.RadiusKMeterInv;
            moon[i] *= Sun.RadiusKMeterInv;
        }
        try (Fixture fixture = new Fixture(sun, moon, "PROBA-2", new JHVTime("2026-08-12T19:12:35.554"))) {
            for (boolean white : new boolean[]{false, true})
                check(fixture.compare(fixture.draw(Quat.ZERO, 4, 0, 0, white), fixture::onMoon, 4, 0, 0, Quat.ZERO, white, "PROBA-2 observation epoch") > 100,
                        "PROBA-2 Moon fixture did not render its disk");
        }
    }

    private static void ordering() throws Exception {
        SpaceObject earth = SpaceObject.get("Earth");
        for (boolean moonNearer : new boolean[]{false, true}) {
            for (boolean white : new boolean[]{false, true}) {
                try (Fixture fixture = new Fixture(new double[]{0, 0, 215}, new double[]{0, 0, .55}, "PROBA-2")) {
                    double[] moon = fixture.vector(2, 0, moonNearer ? 1 : 2);
                    double[] planet = fixture.vector(2, 0, moonNearer ? 2 : 1);
                    fixture.apparentPlanets.clear();
                    fixture.apparentPlanets.add(load(MOON, moon, TIME));
                    fixture.apparentPlanets.add(load(earth, planet, TIME));
                    ByteBuffer pixels = fixture.draw(Quat.ZERO, 8, 0, 0, white);
                    byte[] nearColor = (moonNearer ? MOON : earth).getTrajectoryColor(white);
                    byte[] farColor = (moonNearer ? earth : MOON).getTrajectoryColor(white);
                    int col = 245, row = 165; // inside both disks, away from their center markers and solar limb
                    for (int channel = 0; channel < 3; channel++) {
                        double expected = .5 * (nearColor[channel] & 255) + .25 * (farColor[channel] & 255) + (white ? 63.75 : 0);
                        check(Math.abs((pixels.get(4 * (row * SIZE + col) + channel) & 255) - expected) <= 1,
                                "Overlapping disks did not blend farthest to nearest");
                    }
                    int marker = 4 * (160 * SIZE + 240);
                    for (int channel = 0; channel < 3; channel++)
                        check(Math.abs((pixels.get(marker + channel) & 255) - (nearColor[channel] & 255)) <= 1,
                                "Farther disk covered the nearer planet marker");
                    Collections.reverse(fixture.apparentPlanets);
                    check(pixels.equals(fixture.draw(Quat.ZERO, 8, 0, 0, white)), "Planet input order changed rendered pixels");
                }
            }
        }
        try (Fixture fixture = new Fixture(new double[]{0, 0, 215}, new double[]{0, 0, .55}, "PROBA-2")) {
            fixture.apparentPlanets.clear();
            fixture.apparentPlanets.add(load(MOON, fixture.vector(2, 0, 430), TIME));
            fixture.apparentPlanets.add(load(earth, fixture.vector(2, 0, 860), TIME));
            ByteBuffer pixels = fixture.draw(Quat.ZERO, 8, 0, 0, false);
            byte[] color = MOON.getTrajectoryColor(false);
            for (int channel = 0; channel < 3; channel++)
                check((pixels.get(4 * (160 * SIZE + 240) + channel) & 255) == (color[channel] & 255),
                        "Background planet markers did not draw farthest to nearest");
        }
    }

    private static void nearbyDisk() throws Exception {
        SpaceObject earth = SpaceObject.get("Earth");
        try (Fixture fixture = new Fixture(new double[]{0, 0, 215}, new double[]{0, 0, .55}, "PROBA-2")) {
            for (double degrees : new double[]{89.9, 90, 90.1}) {
                double angle = Math.toRadians(degrees);
                double[] body = new double[3];
                for (int i = 0; i < 3; i++)
                    body[i] = earth.getRadius() / .9 * (Math.cos(angle) * fixture.axes[0][i] + Math.sin(angle) * fixture.axes[2][i]);
                fixture.apparentPlanets.clear();
                fixture.apparentPlanets.add(load(earth, body, TIME));
                ByteBuffer pixels = fixture.draw(Quat.ZERO, 700, 0, 0, false);
                check(fixture.onBody(700 * (250.5 / SIZE - .5), 700 * (.5 / SIZE), body, earth.getRadius()),
                        "Nearby Earth fixture lost its visible edge");
                byte[] color = earth.getTrajectoryColor(false);
                for (int channel = 0; channel < 3; channel++)
                    check(Math.abs((pixels.get(4 * (160 * SIZE + 250) + channel) & 255) - .5 * (color[channel] & 255)) <= 1,
                            "Nearby disk vanished with its center at " + degrees + " degrees from the Sun");
            }
        }
    }

    private static void starsFirst() throws Exception {
        try (Fixture fixture = new Fixture(new double[]{0, 0, 215}, new double[]{0, 0, .55}, "Earth")) {
            double[] direction = fixture.vector(2, 0, 1);
            double ra = Math.atan2(direction[1], direction[0]);
            double dec = Math.asin(direction[2]);
            Stars.Star star = new Stars.Star(ra, dec, 0, 0, 0, 0, 0, 1, direction[0], direction[1], direction[2]);
            field(ViewpointLayerOptionsExpert.class, "starCatalog").set(fixture.options,
                    CompletableFuture.completedFuture(new Stars.Catalog(Stars.julianYear(TIME.milli), new Stars.Star[]{star})));
            field(ViewpointLayerOptionsExpert.class, "locationHeliocentric").set(fixture.options,
                    load(SpaceObject.get("Earth"), new double[]{0, 0, -215}, TIME));
            field(ViewpointLayerOptionsExpert.class, "locationState").set(fixture.options,
                    load(SpaceObject.get("Earth"), new double[]{0, 0, 0}, TIME, true));
            fixture.apparentPlanets.clear();
            ByteBuffer stars = fixture.draw(Quat.ZERO, 8, 0, 0, false);
            int marker = 4 * (160 * SIZE + 240);
            check((stars.get(marker) & 255) == 255, "Synthetic star did not cover the planet-marker sample");
            fixture.apparentPlanets.add(load(MOON, fixture.vector(2, 0, 430), TIME));
            ByteBuffer pixels = fixture.draw(Quat.ZERO, 8, 0, 0, false);
            byte[] color = MOON.getTrajectoryColor(false);
            for (int channel = 0; channel < 3; channel++)
                check((pixels.get(marker + channel) & 255) == (color[channel] & 255), "Star covered a planet marker");
            fixture.apparentPlanets.clear();
            fixture.apparentPlanets.add(load(MOON, fixture.vector(2.15, 0, 1), TIME));
            ByteBuffer eclipse = fixture.draw(Quat.ZERO, 8, 0, 0, false);
            for (int channel = 0; channel < 3; channel++) {
                double expected = .5 * (color[channel] & 255) + 127.5;
                check(Math.abs((eclipse.get(marker + channel) & 255) - expected) <= 1, "Star did not draw before the eclipse fill");
            }
        }
    }

    private static void planetLoads() throws Exception {
        for (String observer : new String[]{"Earth", "Moon"}) {
            try (Fixture fixture = new Fixture(new double[]{0, 0, 215}, new double[]{0, 0, .55}, observer)) {
                field(ViewpointLayerOptionsExpert.class, "skyActive").setBoolean(fixture.options, true);
                fixture.options.setTimeSelection(TIME.milli, TIME.milli + 60000);
                fixture.options.setPlanets(true);
                check(fixture.apparentPlanets.size() == 8, "Planets must include Moon and exclude the observer");
                PositionLoad moon = null;
                for (PositionLoad load : fixture.apparentPlanets) {
                    check(load.target() != SpaceObject.get(observer), "Observer loaded as its own apparent planet");
                    if (load.target() == MOON)
                        moon = load;
                    apparentLoad(load);
                }
                if (observer.equals("Earth"))
                    check(moon != null, "Planets did not include Moon");
                apparentLoad((PositionLoad) field(ViewpointLayerOptionsExpert.class, "apparentSun").get(fixture.options));
                @SuppressWarnings("unchecked")
                List<PositionLoad> owned = (List<PositionLoad>) field(ViewpointLayerOptionsExpert.class, "locationLoads").get(fixture.options);
                check(owned.size() == 9 && owned.containsAll(fixture.apparentPlanets), "Planet loads did not enter the normal cancellation list");
                fixture.options.setPlanets(false);
                check(fixture.apparentPlanets.isEmpty() && owned.isEmpty(), "Disabled Planets retained position loads");
            }
        }
    }

    private static void apparentLoad(PositionLoad load) throws Exception {
        Future<?> future = (Future<?>) field(PositionLoad.class, "future").get(load);
        PositionResponse response = (PositionResponse) future.get(10, TimeUnit.SECONDS);
        Position.Cartesian[] positions = (Position.Cartesian[]) field(PositionResponse.class, "position").get(response);
        Position.Cartesian[] velocities = (Position.Cartesian[]) field(PositionResponse.class, "velocity").get(response);
        check(positions.length == 2 && positions[1].milli() - positions[0].milli() == 60000,
                "Apparent position cadence changed for " + load.target());
        check(velocities != null && velocities.length == positions.length,
                "Apparent position load omitted interpolation tangents for " + load.target());
    }

    private static void clearing() throws Exception {
        try (Fixture fixture = new Fixture(new double[]{0, 0, 215}, new double[]{0, 0, .55}, "Earth")) {
            ByteBuffer initial = fixture.draw(Quat.ZERO, 4, 0, 0, false);
            fixture.moon[2] = 430;
            fixture.install();
            fixture.empty("foreground Moon moved behind Sun");
            fixture.moon[2] = .55;
            fixture.install();
            fixture.apparentPlanets.clear();
            fixture.empty("missing Moon response");
            fixture.install();
            field(ViewpointLayerOptionsExpert.class, "apparentSun").set(fixture.options, null);
            fixture.empty("missing Sun response");
            fixture.install();
            fixture.options.setPlanets(false);
            field(ViewpointLayerOptionsExpert.class, "apparentSun").set(fixture.options, load(SpaceObject.SUN, fixture.sun, fixture.time));
            fixture.empty("Planets disabled");
            fixture.options.setPlanets(true);
            fixture.install();
            field(AbstractLayer.class, "enabled").setBoolean(fixture.layer, false);
            fixture.empty("layer disabled");
            field(AbstractLayer.class, "enabled").setBoolean(fixture.layer, true);
            fixture.layer.getOptions().setCameraMode(ViewpointLayerOptions.CameraMode.Observer, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
            fixture.install();
            fixture.empty("Location disabled");
            fixture.layer.getOptions().setCameraMode(ViewpointLayerOptions.CameraMode.Location, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
            fixture.install();
            check(initial.equals(fixture.draw(Quat.ZERO, 4, 0, 0, false)), "re-enabled Moon changed its pixels");
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final ViewpointLayer layer;
        private final ViewpointLayerOptionsExpert options;
        private final double[] sun;
        private final double[] moon;
        private final double[][] axes;
        private final double distance;
        private final JHVTime time;
        private final List<PositionLoad> apparentPlanets;

        Fixture(double[] _sun, double[] _moon, String observer) throws Exception {
            this(_sun, _moon, observer, TIME);
        }

        @SuppressWarnings("unchecked")
        Fixture(double[] _sun, double[] _moon, String observer, JHVTime _time) throws Exception {
            time = _time;
            sun = _sun;
            moon = _moon;
            axes = axes(sun);
            distance = Math.sqrt(dot(sun, sun));
            JSONObject empty = new JSONObject().put("objects", new JSONArray()).put("stars", false).put("planets", true);
            layer = new ViewpointLayer(new JSONObject().put("mode", "Location").put("location", empty).put("equatorial", empty));
            options = layer.getOptions().getLocationOptions();
            apparentPlanets = (List<PositionLoad>) field(ViewpointLayerOptionsExpert.class, "apparentPlanets").get(options);
            for (SpaceObjectElement element : options.getContainer().getElements()) {
                if (element.getTarget() == SpaceObject.get(observer)) {
                    options.getContainer().selectElement(element);
                    Map<SpaceObjectElement, PositionLoad> loads = (Map<SpaceObjectElement, PositionLoad>) field(ViewpointLayerOptionsExpert.class, "loads").get(options);
                    loads.put(element, load(element.getTarget(), new double[]{distance, 0, 0}, time));
                    break;
                }
            }
            field(AbstractLayer.class, "enabled").setBoolean(layer, true);
            layer.init();
            install();
        }

        private void install() throws Exception {
            field(ViewpointLayerOptionsExpert.class, "apparentSun").set(options, load(SpaceObject.SUN, sun, time));
            apparentPlanets.clear();
            apparentPlanets.add(load(MOON, moon, time));
        }

        private ByteBuffer draw(Quat drag, double width, double x, double y, boolean white) {
            Display.whiteBackground = white;
            Position viewpoint = new Position(time, distance, 0, 0);
            DisplayController.setViewpointUpdate(_ -> viewpoint, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
            DisplayController.cameraFromJson(new JSONObject().put("fov", 2 * Math.atan(width / (2 * distance))).put("dragRotation", drag.toJson()));
            Display.getCamera().setTranslation(-x, -y);
            MapView mv = MapView.create(Display.getCamera(), viewpoint, MapMode.Orthographic, GridType.Viewpoint, new MapScale[]{MapScale.ortho});
            Viewport vp = Display.getViewport(0);
            GL.glViewport(0, 0, SIZE, SIZE);
            GL.glClearColor(white ? 1 : 0, white ? 1 : 0, white ? 1 : 0, 0);
            GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
            Transform.ortho(vp.aspect, mv.cameraWidth(vp), -x, -y, mv.viewRotation());
            GLSLScreenShader.setView(mv, vp);
            GLSLSphereShader.render();
            layer.render(mv, vp);
            ByteBuffer pixels = BufferUtils.newByteBuffer(4 * SIZE * SIZE);
            GL.glReadPixels(0, 0, SIZE, SIZE, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
            return pixels;
        }

        private double[] vector(double x, double y, double length) {
            double[] vector = new double[3];
            for (int i = 0; i < 3; i++)
                vector[i] = axes[0][i] + (x * axes[2][i] + y * axes[1][i]) / distance;
            normalize(vector);
            for (int i = 0; i < 3; i++)
                vector[i] *= length;
            return vector;
        }

        private boolean onMoon(double x, double y) {
            return onBody(x, y, moon, MOON.getRadius());
        }

        private boolean onBody(double x, double y, double[] body, double radius) {
            double[] ray = new double[3];
            for (int i = 0; i < 3; i++)
                ray[i] = axes[0][i] + (x * axes[2][i] + y * axes[1][i]) / distance;
            double along = dot(body, ray) / dot(ray, ray);
            double miss = 0;
            for (int i = 0; i < 3; i++)
                miss += (body[i] - along * ray[i]) * (body[i] - along * ray[i]);
            return along > 0 && miss <= radius * radius;
        }

        private int compare(ByteBuffer pixels, Mask expected, double width, double x, double y, Quat drag, boolean white, String label) {
            double z = dot(moon, axes[0]);
            double mx = distance * dot(moon, axes[2]) / z, my = distance * dot(moon, axes[1]) / z;
            double r2 = mx * mx + my * my;
            Vec3 marker = drag.rotateVector(new Vec3(mx, my, r2 < 1 ? Math.sqrt(1 - r2) : 0));
            byte[] color = MOON.getTrajectoryColor(white);
            double step = width / SIZE;
            int checked = 0;
            for (int row = 1; row < SIZE - 1; row++) {
                for (int col = 1; col < SIZE - 1; col++) {
                    double px = x + width * ((col + .5) / SIZE - .5), py = y + width * ((row + .5) / SIZE - .5);
                    if (Math.hypot(px - marker.x, py - marker.y) < 5 * step)
                        continue;
                    boolean inside = expected.contains(px, py);
                    boolean sunPixel = px * px + py * py < 1;
                    if (Math.abs(Math.hypot(px, py) - 1) < 2 * step)
                        continue;
                    boolean edge = false;
                    for (int dx = -1; dx <= 1; dx++)
                        for (int dy = -1; dy <= 1; dy++)
                            edge |= expected.contains(px + dx * step, py + dy * step) != inside;
                    if (edge)
                        continue;
                    int background = white && !sunPixel ? 255 : 0;
                    for (int channel = 0; channel < 3; channel++) {
                        int actual = pixels.get(4 * (row * SIZE + col) + channel) & 255;
                        double value = inside ? .5 * (color[channel] & 255) + .5 * background : background;
                        check(Math.abs(actual - value) <= 1, label + " pixel " + col + "," + row + ": expected " + value + ", got " + actual);
                    }
                    if (inside)
                        checked++;
                }
            }
            return checked;
        }

        private void empty(String label) {
            ByteBuffer pixels = draw(Quat.ZERO, 4, 0, 0, false);
            for (int i = 0; i < SIZE * SIZE; i++)
                for (int channel = 0; channel < 3; channel++)
                    check(pixels.get(4 * i + channel) == 0, label + " retained lunar pixels");
        }

        @Override
        public void close() {
            layer.dispose();
        }
    }

    private static double[][] axes(double[] sun) {
        double[] z = sun.clone();
        normalize(z);
        double ra = Math.toRadians(286.13), dec = Math.toRadians(63.87);
        double[] north = {Math.cos(dec) * Math.cos(ra), Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
        double projection = dot(north, z);
        for (int i = 0; i < 3; i++)
            north[i] -= projection * z[i];
        normalize(north);
        return new double[][]{z, north, {z[1] * north[2] - z[2] * north[1], z[2] * north[0] - z[0] * north[2], z[0] * north[1] - z[1] * north[0]}};
    }

    private static PositionLoad load(SpaceObject target, double[] vector, JHVTime time) throws Exception {
        return load(target, vector, time, false);
    }

    private static PositionLoad load(SpaceObject target, double[] vector, JHVTime time, boolean state) throws Exception {
        Constructor<PositionResponse> response = PositionResponse.class.getDeclaredConstructor(Position.Cartesian[].class, Position.Cartesian[].class, long.class, long.class);
        response.setAccessible(true);
        PositionResponse positions = response.newInstance(new Position.Cartesian[]{new Position.Cartesian(time.milli, vector[0], vector[1], vector[2])},
                state ? new Position.Cartesian[]{new Position.Cartesian(time.milli, 0, 0, 0)} : null, time.milli, time.milli);
        Constructor<PositionLoad> load = PositionLoad.class.getDeclaredConstructor(PositionLoad.StatusReceiver.class, SpaceObject.class, Future.class);
        load.setAccessible(true);
        return load.newInstance((PositionLoad.StatusReceiver) status -> {}, target, CompletableFuture.completedFuture(positions));
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void normalize(double[] vector) {
        double norm = Math.sqrt(dot(vector, vector));
        for (int i = 0; i < vector.length; i++)
            vector[i] /= norm;
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
