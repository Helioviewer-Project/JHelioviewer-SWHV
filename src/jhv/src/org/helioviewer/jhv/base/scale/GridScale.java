package org.helioviewer.jhv.base.scale;

import java.awt.Point;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableGrid;
import org.helioviewer.jhv.renderable.components.RenderableGrid.GridChoiceType;

public abstract class GridScale {

    public static GridScale polar = new GridScale.GridScaleIdentity(0, 360, 0, Layers.getLargestPhysicalSize() / 2, Transform.transformpolar);
    public static GridScale latitudinal = new GridScale.GridScaleIdentity(0, 360, -90, 90, Transform.transformlatitudinal);
    public static GridScale logpolar = new GridScale.GridScaleLogY(0, 360, 0, Layers.getLargestPhysicalSize() / 2, Transform.transformpolar);
    public static GridScale ortho = new GridScale.GridScaleOrtho(0, 0, 0, 0, Transform.transformlatitudinal);

    public static GridScale current = ortho;

    abstract public double scaleX(double val);

    abstract public double invScaleX(double val);

    abstract public double scaleY(double val);

    abstract public double invScaleY(double val);

    abstract public double getInterpolatedXValue(double v);

    abstract public double getInterpolatedYValue(double v);

    abstract public double getXValueInv(double v);

    abstract public double getYValueInv(double v);

    abstract public double getYstart();

    abstract public double getYstop();

    abstract public void set(double _xStart, double _xStop, double _yStart, double _yStop);

    abstract public Vec2 transform(Vec3 pt);

    abstract public Vec3 transformInverse(Vec2 pt);

    abstract public Vec2 mouseToGrid(Point point, Viewport vp, Camera camera, GridChoiceType gridChoice);

    abstract public Vec2 mouseToGridInv(Point point, Viewport vp, Camera camera);

    public static abstract class GridScaleAbstract extends GridScale {

        protected double xStart;
        protected double xStop;
        protected double yStart;
        protected double yStop;
        protected final Transform transform;

        public GridScaleAbstract(double _xStart, double _xStop, double _yStart, double _yStop, Transform _transform) {
            xStart = _xStart;
            xStop = _xStop;
            yStart = scaleY(_yStart);
            yStop = scaleY(_yStop);
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
            return transform.transformInverse(pt, this);
        }

        @Override
        public Vec2 mouseToGrid(Point point, Viewport vp, Camera camera, GridChoiceType gridChoice) {
            double w = camera.getWidth();
            Vec2 translation = camera.getCurrentTranslation();
            double x = (CameraHelper.computeNormalizedX(vp, point.x) * w - translation.x / vp.aspect) + 0.5;
            double y = (CameraHelper.computeNormalizedY(vp, point.y) * w - translation.y) + 0.5;
            return new Vec2(getInterpolatedXValue(x), getInterpolatedYValue(y));
        }

        @Override
        public Vec2 mouseToGridInv(Point point, Viewport vp, Camera camera) {
            double w = camera.getWidth();
            Vec2 translation = camera.getCurrentTranslation();
            double x = (CameraHelper.computeNormalizedX(vp, point.x) * w - translation.x / vp.aspect);
            double y = (CameraHelper.computeNormalizedY(vp, point.y) * w - translation.y);
            return new Vec2(x, y);
        }
    }

    public static class GridScaleLogY extends GridScaleAbstract {

        public GridScaleLogY(double _xStart, double _xStop, double _yStart, double _yStop, Transform _transform) {
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

    public static class GridScaleIdentity extends GridScaleAbstract {

        public GridScaleIdentity(double _xStart, double _xStop, double _yStart, double _yStop, Transform _transform) {
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

    public static class GridScaleOrtho extends GridScaleIdentity {
        public GridScaleOrtho(double _xStart, double _xStop, double _yStart, double _yStop, Transform _transform) {
            super(_xStart, _xStop, _yStart, _yStop, _transform);
        }

        @Override
        public Vec2 mouseToGrid(Point point, Viewport vp, Camera camera, GridChoiceType gridChoice) {
            Vec3 p = CameraHelper.getVectorFromSphereAlt(camera, vp, point);
            if (p == null)
                return null;

            if (gridChoice != GridChoiceType.VIEWPOINT) {
                Quat q = Quat.rotateWithConjugate(camera.getViewpoint().orientation, RenderableGrid.getGridQuat(camera, gridChoice));
                p = q.rotateInverseVector(p);
            }

            double theta = 90 - MathUtils.radeg * Math.acos(p.y);
            double phi = 90 - MathUtils.radeg * Math.atan2(p.z, p.x);
            phi = MathUtils.mapToMinus180To180(phi);

            if (gridChoice == GridChoiceType.CARRINGTON && phi < 0)
                phi += 360;
            return new Vec2(phi, theta);
        }
    }
}
