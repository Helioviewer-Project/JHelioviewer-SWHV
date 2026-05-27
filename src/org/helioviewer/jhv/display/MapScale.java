package org.helioviewer.jhv.display;

public interface MapScale {

    double getInterpolatedXValue(double v);

    double getInterpolatedYValue(double v);

    double getDisplayXValue(double v, GridType gridType);

    double getXValueInv(double v);

    double getYValueInv(double v);

    double getYstart();

    double getYstop();

    MapScale ortho = new LinearMapScale(0, 0, 0, 0);
    MapScale lati = new LatitudinalMapScale(-180, 180, -90, 90);

    static MapScale hpc(double halfWidth, double halfHeight) {
        return new LinearMapScale(-halfWidth, halfWidth, -halfHeight, halfHeight);
    }

    static MapScale polar(double radialSize) {
        return new LinearMapScale(0, 360, 0, radialSize);
    }

    static MapScale logpolar(double radialSize) {
        return new LogMapScale(0, 360, 0.05, Math.max(0.05, radialSize));
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
            double xs = _xStart;
            double xe = _xStop;
            if (xs == xe)
                xe = Math.nextUp(xs);

            double ys = scaleY(_yStart);
            double ye = scaleY(_yStop);
            if (ys == ye)
                ye = Math.nextUp(ys);

            xStart = xs;
            xStop = xe;
            yStart = ys;
            yStop = ye;
            xRange = xe - xs;
            yRange = ye - ys;
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

    final class LogMapScale extends MapScaleBase {

        LogMapScale(double _xStart, double _xStop, double _yStart, double _yStop) {
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
            return Math.log(val);
        }

        @Override
        public double invScaleY(double val) {
            return Math.exp(val);
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
