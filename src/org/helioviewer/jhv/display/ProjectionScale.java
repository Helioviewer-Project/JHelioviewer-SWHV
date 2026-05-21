package org.helioviewer.jhv.display;

public interface ProjectionScale {

    double getInterpolatedXValue(double v);

    double getInterpolatedYValue(double v);

    double getDisplayXValue(double v, GridType gridType);

    double getXValueInv(double v);

    double getYValueInv(double v);

    double getYstart();

    double getYstop();

    void set(double _xStart, double _xStop, double _yStart, double _yStop);

    ProjectionScale ortho = new ProjectionScaleIdentity(0, 0, 0, 0);
    ProjectionScale hpc = new ProjectionScaleIdentity(-5, 5, -5, 5);
    ProjectionScale lati = new ProjectionScaleLati(-180, 180, -90, 90);
    ProjectionScale polar = new ProjectionScaleIdentity(0, 360, 0, 0);
    ProjectionScale logpolar = new ProjectionScaleLogY(0, 360, 0.05, 1);

    abstract class ProjectionScaleAbstract implements ProjectionScale {

        protected double xStart;
        protected double xStop;
        protected double yStart;
        protected double yStop;

        protected double xRange;
        protected double yRange;
        protected double invXRange;
        protected double invYRange;

        ProjectionScaleAbstract(double _xStart, double _xStop, double _yStart, double _yStop) {
            set(_xStart, _xStop, _yStart, _yStop);
        }

        @Override
        public void set(double _xStart, double _xStop, double _yStart, double _yStop) {
            xStart = _xStart;
            xStop = _xStop;
            if (xStart == xStop)
                xStop = Math.nextUp(xStart);
            yStart = scaleY(_yStart);
            yStop = scaleY(_yStop);
            if (yStart == yStop)
                yStop = Math.nextUp(yStart);

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

    final class ProjectionScaleLogY extends ProjectionScaleAbstract {

        ProjectionScaleLogY(double _xStart, double _xStop, double _yStart, double _yStop) {
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

    class ProjectionScaleIdentity extends ProjectionScaleAbstract {

        ProjectionScaleIdentity(double _xStart, double _xStop, double _yStart, double _yStop) {
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

    final class ProjectionScaleLati extends ProjectionScaleIdentity {

        ProjectionScaleLati(double _xStart, double _xStop, double _yStart, double _yStop) {
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
