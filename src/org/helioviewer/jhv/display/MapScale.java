package org.helioviewer.jhv.display;

public interface MapScale {

    double toMapX(double unitX);

    double toMapY(double unitY);

    double getDisplayXValue(double v, GridType gridType);

    double toUnitX(double mapX);

    double toUnitY(double mapY);

    default double getLambda() {
        return 1;
    }

    MapScale ortho = new LinearMapScale(0, 0, 0, 0);
    MapScale lati = new LatitudinalMapScale(-180, 180, -90, 90);

    static MapScale hpc(double halfWidth, double halfHeight) {
        return new LinearMapScale(-halfWidth, halfWidth, -halfHeight, halfHeight);
    }

    static MapScale boxCoxRadial(double radialSize) {
        return new BoxCoxRadialScale(0, 360, 0, Math.max(radialSize, 1));
    }

    abstract class MapScaleBase implements MapScale {

        protected final double xStart;
        protected final double yStart;

        protected final double xRange;
        protected final double yRange;
        protected final double invXRange;
        protected final double invYRange;

        MapScaleBase(double _xStart, double _xStop, double _yStart, double _yStop) {
            xStart = _xStart;
            double xStop = _xStart == _xStop ? Math.nextUp(_xStart) : _xStop;

            yStart = _yStart;
            double yStop = _yStart == _yStop ? Math.nextUp(_yStart) : _yStop;

            xRange = xStop - xStart;
            yRange = yStop - yStart;
            invXRange = 1.0 / xRange;
            invYRange = 1.0 / yRange;
        }

        @Override
        public double getDisplayXValue(double v, GridType gridType) {
            return v;
        }

        @Override
        public double toMapX(double unitX) {
            return xStart + unitX * xRange;
        }

        @Override
        public double toMapY(double unitY) {
            return yStart + unitY * yRange;
        }

        @Override
        public double toUnitX(double mapX) {
            return (mapX - xStart) * invXRange;
        }

        @Override
        public double toUnitY(double mapY) {
            return (mapY - yStart) * invYRange;
        }

    }

    // Box-Cox radial scale outside radius 1, anchored so the limb has the
    // same normalized position as the linear scale for every lambda.
    final class BoxCoxRadialScale extends MapScaleBase {

        private final double radialSize;

        BoxCoxRadialScale(double _xStart, double _xStop, double _yStart, double _yStop) {
            super(_xStart, _xStop, _yStart, _yStop);
            radialSize = _yStop;
        }

        @Override
        public double getLambda() {
            return lambda();
        }

        @Override
        public double toMapY(double unitY) {
            double limb = limb();
            if (radialSize <= 1 || unitY <= limb)
                return unitY / limb;

            double u = (unitY - limb) / (1 - limb);
            double lambda = lambda();
            return lambda == 0
                    ? Math.pow(radialSize, u)
                    : Math.pow(1 + u * (Math.pow(radialSize, lambda) - 1), 1 / lambda);
        }

        @Override
        public double toUnitY(double mapY) {
            double limb = limb();
            if (radialSize <= 1 || mapY <= 1)
                return mapY * limb;

            double lambda = lambda();
            double u = lambda == 0
                    ? Math.log(mapY) / Math.log(radialSize)
                    : (Math.pow(mapY, lambda) - 1) / (Math.pow(radialSize, lambda) - 1);
            return limb + u * (1 - limb);
        }

        private double limb() {
            return 1 / radialSize;
        }

        private static double lambda() {
            return Display.getWarpLambda();
        }

    }

    class LinearMapScale extends MapScaleBase {

        LinearMapScale(double _xStart, double _xStop, double _yStart, double _yStop) {
            super(_xStart, _xStop, _yStart, _yStop);
        }

    }

    final class LatitudinalMapScale extends LinearMapScale {

        LatitudinalMapScale(double _xStart, double _xStop, double _yStart, double _yStop) {
            super(_xStart, _xStop, _yStart, _yStop);
        }

        @Override
        public double getDisplayXValue(double v, GridType gridType) {
            if (gridType == GridType.Carrington && v < 0)
                return v + 360;
            return v;
        }

    }
}
