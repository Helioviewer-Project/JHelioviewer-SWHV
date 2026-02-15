package org.helioviewer.jhv.display;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

public interface GridScale {

    double getInterpolatedXValue(double v, GridType gridType);

    double getInterpolatedYValue(double v);

    double getXValueInv(double v);

    double getYValueInv(double v);

    double getYstart();

    double getYstop();

    void set(double _xStart, double _xStop, double _yStart, double _yStop);

    @Nonnull
    Vec2 mouseToGrid(int px, int py, Viewport vp, Camera camera, GridType gridType);

    @Nonnull
    Vec2 mouseToGridInv(int px, int py, Viewport vp, Camera camera);

    GridScale ortho = new GridScaleOrtho(0, 0, 0, 0);
    GridScale lati = new GridScaleLati(-180, 180, -90, 90);
    GridScale polar = new GridScaleIdentity(0, 360, 0, 0);
    GridScale logpolar = new GridScaleLogY(0, 360, 0, 0);

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
        public double getInterpolatedXValue(double v, GridType gridType) {
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

        @Nonnull
        @Override
        public Vec2 mouseToGrid(int px, int py, Viewport vp, Camera camera, GridType gridType) {
            double x = CameraHelper.computeUpX(camera, vp, px) / vp.aspect + 0.5;
            double y = CameraHelper.computeUpY(camera, vp, py) + 0.5;
            return new Vec2(getInterpolatedXValue(x, gridType), getInterpolatedYValue(y));
        }

        @Nonnull
        @Override
        public Vec2 mouseToGridInv(int px, int py, Viewport vp, Camera camera) {
            double x = CameraHelper.computeUpX(camera, vp, px) / vp.aspect;
            double y = CameraHelper.computeUpY(camera, vp, py);
            return new Vec2(x, y);
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
        public double getInterpolatedXValue(double v, GridType gridType) {
            double ix = super.getInterpolatedXValue(v, gridType);
            if (gridType == GridType.Carrington && ix < 0)
                ix += 360;
            return ix;
        }

    }

    class GridScaleOrtho extends GridScaleIdentity {

        GridScaleOrtho(double _xStart, double _xStop, double _yStart, double _yStop) {
            super(_xStart, _xStop, _yStart, _yStop);
        }

        @Nonnull
        @Override
        public Vec2 mouseToGrid(int px, int py, Viewport vp, Camera camera, GridType gridType) {
            Vec3 p = CameraHelper.getVectorFromSphere(camera, vp, px, py, Quat.ZERO, true);
            if (p == null)
                return Vec2.NAN;

            if (gridType != GridType.Viewpoint) {
                Position viewpoint = camera.getViewpoint();
                Quat q = Quat.rotateWithConjugate(viewpoint.toQuat(), gridType.toCarrington(viewpoint));
                p = q.rotateInverseVector(p);
            }

            double theta = Math.asin(MathUtils.clip(p.y, -1., 1.)) * MathUtils.radeg;
            double phi = Math.atan2(p.x, p.z) * MathUtils.radeg;

            if (gridType == GridType.Carrington && phi < 0)
                phi += 360;
            return new Vec2(phi, theta);
        }

    }

}
