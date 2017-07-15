package org.helioviewer.jhv.base.scale;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.renderable.components.RenderableGrid;
import org.helioviewer.jhv.renderable.components.RenderableGrid.GridType;
import org.helioviewer.jhv.renderable.gui.ImageLayers;

public abstract class GridScale {

    public static final GridScale polar = new GridScaleIdentity(0, 360, 0, 0.5 * ImageLayers.getLargestPhysicalSize(), Transform.transformpolar);
    public static final GridScale lati = new GridScaleIdentity(0, 360, -90, 90, Transform.transformlatitudinal);
    public static final GridScale logpolar = new GridScaleLogY(0, 360, 0, 0.5 * ImageLayers.getLargestPhysicalSize(), Transform.transformpolar);
    public static final GridScale ortho = new GridScaleOrtho(0, 0, 0, 0, Transform.transformlatitudinal);

    protected abstract double scaleX(double val);

    protected abstract double invScaleX(double val);

    protected abstract double scaleY(double val);

    protected abstract double invScaleY(double val);

    public abstract double getInterpolatedXValue(double v);

    public abstract double getInterpolatedYValue(double v);

    public abstract double getXValueInv(double v);

    public abstract double getYValueInv(double v);

    public abstract double getYstart();

    public abstract double getYstop();

    public abstract void set(double _xStart, double _xStop, double _yStart, double _yStop);

    public abstract Vec2 transform(Vec3 pt);

    public abstract Vec3 transformInverse(Vec2 pt);

    public abstract Vec2 mouseToGrid(int px, int py, Viewport vp, Camera camera, GridType gridType);

    public abstract Vec2 mouseToGridInv(int px, int py, Viewport vp, Camera camera);

    private abstract static class GridScaleAbstract extends GridScale {

        protected double xStart;
        protected double xStop;
        protected double yStart;
        protected double yStop;
        protected final Transform transform;

        private GridScaleAbstract(double _xStart, double _xStop, double _yStart, double _yStop, Transform _transform) {
            set(_xStart, _xStop, _yStart, _yStop);
            transform = _transform;
        }

        @Override
        public void set(double _xStart, double _xStop, double _yStart, double _yStop) {
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

        @Override
        public Vec2 transform(Vec3 pt) {
            return transform.transform(pt, this);
        }

        @Override
        public Vec3 transformInverse(Vec2 pt) {
            return transform.transformInverse(pt);
        }

        @Override
        public Vec2 mouseToGrid(int px, int py, Viewport vp, Camera camera, GridType gridType) {
            double x = CameraHelper.computeUpX(camera, vp, px) / vp.aspect + 0.5;
            double y = CameraHelper.computeUpY(camera, vp, py) + 0.5;
            return new Vec2(getInterpolatedXValue(x), getInterpolatedYValue(y));
        }

        @Override
        public Vec2 mouseToGridInv(int px, int py, Viewport vp, Camera camera) {
            double x = CameraHelper.computeUpX(camera, vp, px) / vp.aspect;
            double y = CameraHelper.computeUpY(camera, vp, py);
            return new Vec2(x, y);
        }
    }

    private static class GridScaleLogY extends GridScaleAbstract {

        GridScaleLogY(double _xStart, double _xStop, double _yStart, double _yStop, Transform _transform) {
            super(_xStart, _xStop, _yStart, _yStop, _transform);
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

    private static class GridScaleIdentity extends GridScaleAbstract {

        GridScaleIdentity(double _xStart, double _xStop, double _yStart, double _yStop, Transform _transform) {
            super(_xStart, _xStop, _yStart, _yStop, _transform);
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

    private static class GridScaleOrtho extends GridScaleIdentity {
        GridScaleOrtho(double _xStart, double _xStop, double _yStart, double _yStop, Transform _transform) {
            super(_xStart, _xStop, _yStart, _yStop, _transform);
        }

        @Override
        public Vec2 mouseToGrid(int px, int py, Viewport vp, Camera camera, GridType gridType) {
            Vec3 p = CameraHelper.getVectorFromSphere(camera, vp, px, py, Quat.ZERO, true);
            if (p == null)
                return Vec2.NAN_VECTOR;

            if (gridType != GridType.Viewpoint) {
                Quat q = Quat.rotateWithConjugate(camera.getViewpoint().orientation, RenderableGrid.getGridQuat(camera, gridType));
                p = q.rotateInverseVector(p);
            }

            double theta = 90 - MathUtils.radeg * Math.acos(p.y);
            double phi = 90 - MathUtils.radeg * Math.atan2(p.z, p.x);
            phi = MathUtils.mapToMinus180To180(phi);

            if (gridType == GridType.Carrington && phi < 0)
                phi += 360;
            return new Vec2(phi, theta);
        }
    }

}
