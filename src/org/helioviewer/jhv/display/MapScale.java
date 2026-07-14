package org.helioviewer.jhv.display;

public interface MapScale {

    double getInterpolatedXValue(double v);

    double getInterpolatedYValue(double v);

    double getDisplayXValue(double v, GridType gridType);

    double getXValueInv(double v);

    double getYValueInv(double v);

    double getYstart();

    double getYstop();

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
        protected final double xStop;
        protected final double yStart;
        protected final double yStop;

        protected final double xRange;
        protected final double yRange;
        protected final double invXRange;
        protected final double invYRange;

        MapScaleBase(double _xStart, double _xStop, double _yStart, double _yStop) {
            xStart = _xStart;
            xStop = _xStart == _xStop ? Math.nextUp(_xStart) : _xStop;

            yStart = scaleY(_yStart);
            double scaledYStop = scaleY(_yStop);
            yStop = yStart == scaledYStop ? Math.nextUp(yStart) : scaledYStop;

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
        public double getInterpolatedXValue(double v) {
            return invScaleX(xStart + v * xRange);
        }

        @Override
        public double getInterpolatedYValue(double v) {
            return invScaleY(yStart + v * yRange);
        }

        @Override
        public double getXValueInv(double v) {
            return (scaleX(v) - xStart) * invXRange - 0.5;
        }

        @Override
        public double getYValueInv(double v) {
            return (scaleY(v) - yStart) * invYRange - 0.5;
        }

        @Override
        public double getYstart() {
            return yStart;
        }

        @Override
        public double getYstop() {
            return yStop;
        }

        protected abstract double scaleX(double val);

        protected abstract double invScaleX(double val);

        protected abstract double scaleY(double val);

        protected abstract double invScaleY(double val);

    }

    // Box-Cox radial scale outside radius 1.
    final class BoxCoxRadialScale extends MapScaleBase {

        BoxCoxRadialScale(double _xStart, double _xStop, double _yStart, double _yStop) {
            super(_xStart, _xStop, _yStart, _yStop);
        }

        @Override
        public double getLambda() {
            return lambda();
        }

        @Override
        public double scaleX(double val) {
            return val;
        }

        @Override
        public double invScaleX(double val) {
            return val;
        }

        @Override
        public double scaleY(double val) {
            if (val <= 1)
                return val;

            double lambda = lambda();
            return lambda == 0 ? 1 + Math.log(val) : 1 + (Math.pow(val, lambda) - 1) / lambda;
        }

        @Override
        public double invScaleY(double val) {
            if (val <= 1)
                return val;

            double lambda = lambda();
            return lambda == 0 ? Math.exp(val - 1) : Math.pow(1 + lambda * (val - 1), 1 / lambda);
        }

        private static double lambda() {
            return Display.getWarpLambda();
        }

    }

    class LinearMapScale extends MapScaleBase {

        LinearMapScale(double _xStart, double _xStop, double _yStart, double _yStop) {
            super(_xStart, _xStop, _yStart, _yStop);
        }

        @Override
        public double scaleX(double val) {
            return val;
        }

        @Override
        public double invScaleX(double val) {
            return val;
        }

        @Override
        public double scaleY(double val) {
            return val;
        }

        @Override
        public double invScaleY(double val) {
            return val;
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
