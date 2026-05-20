package org.helioviewer.jhv.display;

public interface GridScale {

    double getInterpolatedXDisplayValue(double v, GridType gridType);

    double getInterpolatedXValue(double v);

    double getInterpolatedYValue(double v);

    double getXValueInv(double v);

    double getYValueInv(double v);

    double getYstart();

    double getYstop();

    void set(double _xStart, double _xStop, double _yStart, double _yStop);

    GridScale ortho = new GridScaleIdentity(0, 0, 0, 0);
    GridScale hpc = new GridScaleIdentity(-5, 5, -5, 5);
    GridScale lati = new GridScaleLati(-180, 180, -90, 90);
    GridScale polar = new GridScaleIdentity(0, 360, 0, 0);
    GridScale logpolar = new GridScaleLogY(0, 360, 0.05, 1);

    abstract class GridScaleAbstract implements GridScale {

        protected double xStart;
        protected double xStop;
        protected double yStart;
        protected double yStop;

        GridScaleAbstract(double _xStart, double _xStop, double _yStart, double _yStop) {
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
        }

        @Override
        public double getInterpolatedXDisplayValue(double v, GridType gridType) {
            return getInterpolatedXValue(v);
        }

        @Override
        public double getInterpolatedXValue(double v) {
            return invScaleX(xStart + v * (xStop - xStart));
        }

        @Override
        public double getInterpolatedYValue(double v) {
            return invScaleY(yStart + v * (yStop - yStart));
        }

        @Override
        public double getXValueInv(double v) {
            return (scaleX(v) - xStart) / (xStop - xStart) - 0.5;
        }

        @Override
        public double getYValueInv(double v) {
            return (scaleY(v) - yStart) / (yStop - yStart) - 0.5;
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

    class GridScaleLogY extends GridScaleAbstract {

        GridScaleLogY(double _xStart, double _xStop, double _yStart, double _yStop) {
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

    class GridScaleIdentity extends GridScaleAbstract {

        GridScaleIdentity(double _xStart, double _xStop, double _yStart, double _yStop) {
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

    class GridScaleLati extends GridScaleIdentity {

        GridScaleLati(double _xStart, double _xStop, double _yStart, double _yStop) {
            super(_xStart, _xStop, _yStart, _yStop);
        }

        @Override
        public double getInterpolatedXDisplayValue(double v, GridType gridType) {
            double ix = getInterpolatedXValue(v);
            if (gridType == GridType.Carrington && ix < 0)
                ix += 360;
            return ix;
        }

    }
}
