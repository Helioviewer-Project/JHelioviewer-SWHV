package org.helioviewer.jhv.astronomy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class PositionResponseTest {

    public static void main(String[] arguments) throws Exception {
        requestedInterval();
        apparent();
        paddedInterval();
        state();
        cubic();
        spherical();
        singleSample();
        jsonState();
        jsonInterval();
        System.out.println("PASS: requested-interval mapping and coverage, linear positions/velocities, cubic state positions, nearby-body limbs, spherical conventions and JSON units");
    }

    private static void requestedInterval() throws Exception {
        long start = TimeUtils.parse("2026-08-12T15:25:05.000");
        long end = TimeUtils.parse("2026-08-12T19:27:00.000");
        long time = TimeUtils.parse("2026-08-12T19:12:35.554");
        Position.Cartesian[] samples = new Position.Cartesian[243];
        for (int i = 0; i < samples.length; i++)
            samples[i] = new Position.Cartesian(start + i * 60000L, i, 0, 0);
        PositionResponse response = new PositionResponse(samples, null, start, end);
        PositionResponse.Interpolated out = new PositionResponse.Interpolated();
        double[] pos = new double[3];
        response.interpolateRectangular(time, start, end, pos, out);
        check(out.time == time, "synchronized timestamp shifted by " + (time - out.time) + " ms");
        check(Math.abs(pos[0] - (time - start) / 60000.) < 1e-12, "position evaluated at a shifted epoch");
        check(response.interpolateTime(start, start, end) == start, "requested start");
        check(response.interpolateTime(end, start, end) == end, "requested end selected padding");
        check(response.interpolateTime(start - 1, start, end) == start, "clamped requested start");
        check(response.interpolateTime(end + 1, start, end) == end, "clamped requested end");

        boolean refused = false;
        try {
            new PositionResponse(Arrays.copyOf(samples, 242), null, start, end);
        } catch (Exception e) {
            refused = true;
        }
        check(refused, "original short response accepted");
        refused = false;
        try {
            new PositionResponse(Arrays.copyOfRange(samples, 1, samples.length), null, start, end);
        } catch (Exception e) {
            refused = true;
        }
        check(refused, "response missing the requested start accepted");
    }

    private static void apparent() throws Exception {
        JSONObject fixture = new JSONObject(Files.readString(Path.of("extra/test/astronomy/apparent-state-interpolation.json")));
        for (Object value : fixture.getJSONArray("cases")) {
            JSONObject geometry = (JSONObject) value;
            long start = TimeUtils.parse(geometry.getString("start")), end = start + 60000;
            JSONArray result = new JSONArray();
            for (int i = 0; i < 2; i++) {
                JSONArray sample = geometry.getJSONArray("samples").getJSONArray(i);
                JSONArray position = new JSONArray(), velocity = new JSONArray();
                for (int j = 0; j < 3; j++) {
                    position.put(sample.getDouble(j));
                    velocity.put(sample.getDouble(j + 3));
                }
                result.put(new JSONObject().put(TimeUtils.format(start + i * 60000L), new JSONArray().put(position).put(velocity)));
            }
            JSONObject json = new JSONObject().put("result", result);
            PositionResponse response = new PositionResponse(json, true, start, end);
            JSONArray positions = new JSONArray();
            for (Object row : result) {
                JSONObject entry = (JSONObject) row;
                String time = entry.keys().next();
                positions.put(new JSONObject().put(time, entry.getJSONArray(time).getJSONArray(0)));
            }
            PositionResponse linear = new PositionResponse(new JSONObject().put("result", positions), false, start, end);
            PositionResponse.Interpolated out = new PositionResponse.Interpolated();
            double[] actual = new double[3];
            double worstLinear = 0;
            double radius = geometry.getDouble("radiusKM");
            for (Object point : geometry.getJSONArray("reference")) {
                JSONObject reference = (JSONObject) point;
                long time = start + reference.getLong("offsetMillis");
                JSONArray state = reference.getJSONArray("state");
                double[] expected = {state.getDouble(0), state.getDouble(1), state.getDouble(2)};
                response.interpolateRectangular(time, start, end, actual, out);
                check(out.time == time, "Apparent interpolation shifted the observation epoch");
                check(limbError(actual, expected, radius) < .1,
                        geometry.getString("observer") + " -> " + geometry.getString("target") + " interpolation exceeds 0.1 arcsec");
                linear.interpolateRectangular(time, start, end, actual, out);
                worstLinear = Math.max(worstLinear, limbError(actual, expected, radius));
            }
            if (geometry.getString("observer").equals("PROBA2") && geometry.getString("target").equals("EARTH"))
                check(worstLinear > 100, "Nearby-Earth fixture no longer exposes the linear interpolation error");
        }
    }

    private static double limbError(double[] actual, double[] expected, double radius) {
        double x = actual[0] * expected[1] - actual[1] * expected[0];
        double y = actual[1] * expected[2] - actual[2] * expected[1];
        double z = actual[2] * expected[0] - actual[0] * expected[2];
        double dot = actual[0] * expected[0] + actual[1] * expected[1] + actual[2] * expected[2];
        double center = Math.atan2(Math.sqrt(x * x + y * y + z * z), dot);
        double actualDistance = Math.sqrt(actual[0] * actual[0] + actual[1] * actual[1] + actual[2] * actual[2]) / Sun.RadiusKMeterInv;
        double expectedDistance = Math.sqrt(expected[0] * expected[0] + expected[1] * expected[1] + expected[2] * expected[2]);
        double size = Math.abs(Math.asin(radius / actualDistance) - Math.asin(radius / expectedDistance));
        return Math.toDegrees(center + size) * 3600;
    }

    private static void paddedInterval() throws Exception {
        Position.Cartesian[] position = {
                new Position.Cartesian(10000, 0, 0, 0),
                new Position.Cartesian(11000, 1, 0, 0),
                new Position.Cartesian(12000, 2, 0, 0)
        };
        double speed = Sun.RadiusMilli / 1000;
        Position.Cartesian[] velocity = {
                new Position.Cartesian(10000, speed, 0, 0),
                new Position.Cartesian(11000, speed, 0, 0),
                new Position.Cartesian(12000, speed, 0, 0)
        };
        PositionResponse response = new PositionResponse(position, velocity, 10000, 11500);
        PositionResponse.Interpolated out = new PositionResponse.Interpolated();
        double[] pos = new double[3], vel = new double[3];
        response.interpolateState(125, 100, 200, pos, vel, out);
        check(out.time == 10375, "independent interval used padded sample end");
        vector(pos, .375, 0, 0);
        near(vel, speed, 0, 0);
        response.interpolateState(200, 100, 200, pos, vel, out);
        check(out.time == 11500, "end mapped to padding");
        vector(pos, 1.5, 0, 0);
        check(response.interpolateTime(100, 100, 100) == 11500, "collapsed movie interval selected padding");
    }

    private static void state() throws Exception {
        Position.Cartesian[] position = {
                new Position.Cartesian(10000, 1, 0, 0),
                new Position.Cartesian(11000, 0, 1, 0),
                new Position.Cartesian(12000, -1, 0, 0)
        };
        Position.Cartesian[] velocity = {
                new Position.Cartesian(10000, 0, 2, 4),
                new Position.Cartesian(11000, 2, 4, 6),
                new Position.Cartesian(12000, 6, 8, 10)
        };
        PositionResponse response = new PositionResponse(position, velocity, 10000, 12000);
        PositionResponse.Interpolated out = new PositionResponse.Interpolated();
        double[] pos = new double[3], vel = new double[3];
        response.interpolateState(125, 100, 200, pos, vel, out);
        double offset = 1000 / Sun.RadiusMilli / 4;
        near(pos, .5 - offset, .5 - offset, -offset);
        vector(vel, 1, 3, 5);
        check(out.time == 10500, "mapped state time");
        near(new double[]{out.x, out.y, out.z}, .5 - offset, .5 - offset, -offset);

        response.interpolateState(175, 100, 200, pos, vel, out);
        near(pos, -.5 - 2 * offset, .5 - 2 * offset, -2 * offset);
        vector(vel, 4, 6, 8);
        response.interpolateState(0, 100, 200, pos, vel, out);
        vector(pos, 1, 0, 0);
        vector(vel, 0, 2, 4);
        check(out.time == 10000, "clamped start time");
        response.interpolateState(300, 100, 200, pos, vel, out);
        vector(pos, -1, 0, 0);
        vector(vel, 6, 8, 10);
        check(out.time == 12000, "clamped end time");
        response.interpolateState(100, 100, 100, pos, vel, out);
        check(out.time == 12000, "collapsed playback interval selects end");
        response.interpolateState(125, 100, 200, pos, pos, out);
        near(pos, .5 - offset, .5 - offset, -offset);

        boolean refused = false;
        try {
            new PositionResponse(position, null, 10000, 12000).interpolateState(125, 100, 200, pos, vel, out);
        } catch (IllegalStateException e) {
            refused = true;
        }
        check(refused, "position-only response refuses state interpolation");
    }

    private static void cubic() throws Exception {
        double speedScale = Sun.RadiusMilli / 1000 * 1e-6;
        Position.Cartesian[] positions = {
                new Position.Cartesian(10000, 0, 0, 1e-6),
                new Position.Cartesian(14000, 64e-6, 28e-6, 13e-6)
        };
        Position.Cartesian[] velocities = {
                new Position.Cartesian(10000, 0, -speedScale, 3 * speedScale),
                new Position.Cartesian(14000, 48 * speedScale, 15 * speedScale, 3 * speedScale)
        };
        PositionResponse response = new PositionResponse(positions, velocities, 10000, 14000);
        PositionResponse.Interpolated out = new PositionResponse.Interpolated();
        double[] pos = new double[3], vel = new double[3], lati = new double[3];
        for (long offset : new long[]{0, 1, 123, 1000, 1755, 2000, 3999, 4000}) {
            double t = offset / 1000.;
            response.interpolateRectangular(10000 + offset, 10000, 14000, pos, out);
            near(pos, t * t * t * 1e-6, (2 * t * t - t) * 1e-6, (3 * t + 1) * 1e-6);
            response.interpolateState(10000 + offset, 10000, 14000, pos, vel, out);
            near(pos, t * t * t * 1e-6, (2 * t * t - t) * 1e-6, (3 * t + 1) * 1e-6);
            near(vel, 12 * t * speedScale, (-1 + 4 * t) * speedScale, 3 * speedScale);
            response.interpolateLatitudinal(10000 + offset, 10000, 14000, lati, out);
            near(lati, Math.sqrt(pos[0] * pos[0] + pos[1] * pos[1] + pos[2] * pos[2]),
                    Math.atan2(pos[1], pos[0]), Math.atan2(pos[2], Math.hypot(pos[0], pos[1])));
            check(out.time == 10000 + offset, "Cubic interpolation changed fractional sample time");
        }
        new PositionResponse(positions, null, 10000, 14000).interpolateRectangular(12000, 10000, 14000, pos, out);
        near(pos, 32e-6, 14e-6, 7e-6);
    }

    private static void spherical() throws Exception {
        // x, y, z, distance, ordinary longitude, latitude
        double[][] cases = {
                {0, 0, 0, 0, 0, 0},
                {1, 0, 0, 1, 0, 0},
                {0, 1, 0, 1, Math.PI / 2, 0},
                {-1, 0, 0, 1, Math.PI, 0},
                {0, -1, 0, 1, -Math.PI / 2, 0},
                {0, 0, 1, 1, 0, Math.PI / 2},
                {0, 0, -1, 1, 0, -Math.PI / 2},
                {1, -0., -0., 1, -0., -0.},
                {-1, -0., 0, 1, -Math.PI, 0},
                {1, -1, 0, Math.sqrt(2), -Math.PI / 4, 0}
        };
        for (double[] c : cases) {
            PositionResponse response = new PositionResponse(new Position.Cartesian[]{new Position.Cartesian(10000, c[0], c[1], c[2])}, null, 10000, 10000);
            PositionResponse.Interpolated out = new PositionResponse.Interpolated();
            double[] lati = new double[3];
            response.interpolateLatitudinal(150, 100, 200, lati, out);
            vector(lati, c[3], c[4], c[5]);
            Position carrington = response.interpolateCarrington(150, 100, 200, out);
            double longitude = c[4] < 0 ? c[4] + 2 * Math.PI : c[4];
            vector(new double[]{carrington.distance, carrington.lon, carrington.lat}, c[3], -longitude, c[5]);
            vector(new double[]{out.x, out.y, out.z}, c[0], c[1], c[2]);
            check(carrington.time.milli == 10000, "Carrington sample time");
        }
    }

    private static void singleSample() throws Exception {
        Position.Cartesian[] position = {new Position.Cartesian(10000, -0., 2, -3)};
        Position.Cartesian[] velocity = {new Position.Cartesian(10000, 4, -0., 6)};
        PositionResponse response = new PositionResponse(position, velocity, 10000, 10000);
        PositionResponse.Interpolated out = new PositionResponse.Interpolated();
        double[] pos = new double[3], vel = new double[3];
        response.interpolateState(300, 100, 200, pos, vel, out);
        vector(pos, -0., 2, -3);
        vector(vel, 4, -0., 6);
        check(out.time == 10000, "single sample time");
        float[] xyz = {0, 0, 0, 7};
        double distance = response.interpolateRectangular(300, 100, 200, xyz, out);
        check(Float.floatToIntBits(xyz[0]) == Float.floatToIntBits(-0.f), "signed zero in float output");
        check(xyz[1] == 2 && xyz[2] == -3 && xyz[3] == 7, "float output leaves fourth component unchanged");
        check(distance == Math.sqrt(13), "rectangular distance");
    }

    private static void jsonState() throws Exception {
        PositionResponse response = new PositionResponse(new JSONObject("""
                {"result": [
                    {"2026-10-20T00:00:00": [[695700, 0, 0], [299792.458, 0, 0]]},
                    {"2026-10-20T00:01:00": [[0, 695700, 0], [0, 299792.458, 0]]}
                ]}
                """), true, TimeUtils.parse("2026-10-20T00:00:00"), TimeUtils.parse("2026-10-20T00:01:00"));
        double[] pos = new double[3], vel = new double[3];
        response.interpolateState(150, 100, 200, pos, vel, new PositionResponse.Interpolated());
        double tangent = 60000 / Sun.RadiusMilli / 8 * (2 * vel[0]);
        near(pos, .5 + tangent, .5 - tangent, 0);
        // The km/s-to-c conversion rounds one ULP below .5.
        vector(vel, 0.49999999999999994, 0.49999999999999994, 0);
    }

    private static void jsonInterval() throws Exception {
        JSONObject json = new JSONObject("""
                {"result": [
                    {"2026-08-12T19:12:05.000": [695700, 0, 0]},
                    {"2026-08-12T19:13:05.000": [0, 695700, 0]}
                ]}
                """);
        long start = TimeUtils.parse("2026-08-12T19:12:05.000");
        long end = TimeUtils.parse("2026-08-12T19:12:35.554");
        PositionResponse response = new PositionResponse(json, false, start, end);
        PositionResponse.Interpolated out = new PositionResponse.Interpolated();
        response.interpolateRectangular(end, start, end, new double[3], out);
        check(out.time == end, "JSON response mapped the requested end to padding");
        boolean refused = false;
        try {
            new PositionResponse(json, false, start, start + 60001);
        } catch (Exception e) {
            refused = true;
        }
        check(refused, "short JSON response accepted");
    }

    private static void near(double[] actual, double x, double y, double z) {
        check(Math.abs(actual[0] - x) < 1e-12 && Math.abs(actual[1] - y) < 1e-12 && Math.abs(actual[2] - z) < 1e-12,
                "interpolated vector: " + Arrays.toString(actual) + " expected " + Arrays.toString(new double[]{x, y, z}));
    }

    private static void vector(double[] actual, double x, double y, double z) {
        check(Double.doubleToLongBits(actual[0]) == Double.doubleToLongBits(x)
                && Double.doubleToLongBits(actual[1]) == Double.doubleToLongBits(y)
                && Double.doubleToLongBits(actual[2]) == Double.doubleToLongBits(z),
                "vector: [" + actual[0] + ", " + actual[1] + ", " + actual[2] + "] expected [" + x + ", " + y + ", " + z + "]");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
