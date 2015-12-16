package org.helioviewer.jhv.base.scale;

public interface GridScale {

    public double scaleX(double val);

    public double invScaleX(double val);

    public double scaleY(double val);

    public double invScaleY(double val);

    public double getInterpolatedXValue(double v);

    public double getInterpolatedYValue(double v);

    public double getYstart();

    public double getYstop();

    public static abstract class GridScaleAbstract implements GridScale {
        protected final double xStart;
        protected final double xStop;
        protected final double yStart;
        protected final double yStop;

        public GridScaleAbstract(double _xStart, double _xStop, double _yStart, double _yStop) {
            xStart = _xStart;
            xStop = _xStop;
            yStart = scaleY(_yStart);
            yStop = scaleY(_yStop);
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
        public double getYstart() {
            return yStart;
        }

        @Override
        public double getYstop() {
            return yStop;
        }

    }

    public static class GridScaleLogY extends GridScaleAbstract {

        public GridScaleLogY(double _xStart, double _xStop, double _yStart, double _yStop) {
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

    public static class GridScaleIdentity extends GridScaleAbstract {

        public GridScaleIdentity(double _xStart, double _xStop, double _yStart, double _yStop) {
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

}
