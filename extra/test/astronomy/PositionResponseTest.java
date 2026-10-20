package org.helioviewer.jhv.astronomy;

import org.json.JSONObject;

public class PositionResponseTest {

    public static void main(String[] arguments) throws Exception {
        state();
        spherical();
        singleSample();
        jsonState();
        System.out.println("PASS: position/state interpolation, playback mapping, spherical conventions and JSON units");
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
        PositionResponse response = new PositionResponse(position, velocity);
        PositionResponse.Interpolated out = new PositionResponse.Interpolated();
        double[] pos = new double[3], vel = new double[3];
        response.interpolateState(125, 100, 200, pos, vel, out);
        vector(pos, .5, .5, 0);
        vector(vel, 1, 3, 5);
        check(out.time == 10500, "mapped state time");
        vector(new double[]{out.x, out.y, out.z}, .5, .5, 0);

        response.interpolateState(175, 100, 200, pos, vel, out);
        vector(pos, -.5, .5, 0);
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
        vector(pos, .5, .5, 0);

        boolean refused = false;
        try {
            new PositionResponse(position, null).interpolateState(125, 100, 200, pos, vel, out);
        } catch (IllegalStateException e) {
            refused = true;
        }
        check(refused, "position-only response refuses state interpolation");
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
            PositionResponse response = new PositionResponse(new Position.Cartesian[]{new Position.Cartesian(10000, c[0], c[1], c[2])}, null);
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
        PositionResponse response = new PositionResponse(position, velocity);
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
                """), true);
        double[] pos = new double[3], vel = new double[3];
        response.interpolateState(150, 100, 200, pos, vel, new PositionResponse.Interpolated());
        vector(pos, .5, .5, 0);
        // The km/s-to-c conversion rounds one ULP below .5.
        vector(vel, 0.49999999999999994, 0.49999999999999994, 0);
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
