package org.helioviewer.jhv.display;

public interface MapScale {

    double toMapX(double unitX);

    double toMapY(double unitY);

    double toUnitX(double mapX);

    double toUnitY(double mapY);

    default double warpLambda() {
        return 1;
    }

    MapScale ortho = new LinearMapScale(0, 0, 0, 0);
    MapScale lati = new LinearMapScale(-180, 180, -90, 90);

    static MapScale hpc(double halfWidth, double halfHeight) {
        return new LinearMapScale(-halfWidth, halfWidth, -halfHeight, halfHeight);
    }

    static MapScale boxCoxRadial(double radialSize) {
        return new BoxCoxRadialScale(Math.max(radialSize, 1));
    }

    class LinearMapScale implements MapScale {

        private final double xStart;
        private final double yStart;

        private final double xRange;
        private final double yRange;
        private final double invXRange;
        private final double invYRange;

        LinearMapScale(double xStart, double xStop, double yStart, double yStop) {
            this.xStart = xStart;
            double effectiveXStop = xStart == xStop ? Math.nextUp(xStart) : xStop;

            this.yStart = yStart;
            double effectiveYStop = yStart == yStop ? Math.nextUp(yStart) : yStop;

            xRange = effectiveXStop - xStart;
            yRange = effectiveYStop - yStart;
            invXRange = 1.0 / xRange;
            invYRange = 1.0 / yRange;
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
    final class BoxCoxRadialScale extends LinearMapScale {

        private final double radialSize;
        private final double limb;

        BoxCoxRadialScale(double radialSize) {
            super(0, 360, 0, radialSize);
            this.radialSize = radialSize;
            limb = 1 / radialSize;
        }

        @Override
        public double warpLambda() {
            return Display.getWarpLambda();
        }

        @Override
        public double toMapY(double unitY) {
            if (radialSize <= 1 || unitY <= limb)
                return unitY / limb;

            double u = (unitY - limb) / (1 - limb);
            double lambda = warpLambda();
            return lambda == 0
                    ? Math.pow(radialSize, u)
                    : Math.pow(1 + u * (Math.pow(radialSize, lambda) - 1), 1 / lambda);
        }

        @Override
        public double toUnitY(double mapY) {
            if (radialSize <= 1 || mapY <= 1)
                return mapY * limb;

            double lambda = warpLambda();
            double u = lambda == 0
                    ? Math.log(mapY) / Math.log(radialSize)
                    : (Math.pow(mapY, lambda) - 1) / (Math.pow(radialSize, lambda) - 1);
            return limb + u * (1 - limb);
        }

    }

}
