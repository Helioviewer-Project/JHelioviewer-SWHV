package org.helioviewer.gl3d.wcs;

/**
 * Provides {@link UnitConverter}s. These again provide factors that canb e used
 * convert {@link CoordinateVector} from one unit to others.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class UnitConverterFactory {

    public static UnitConverter getUnitConverter(Unit source) {
        switch (source) {
        case Kilometer:
            return new KilometerConverter();
        case Meter:
            return new MeterConverter();
        case Radian:
            return new RadianConverter();
        case Degree:
            return new DegreeConverter();
        default:
            throw new IllegalArgumentException("No UnitConverter available for Unit " + source);
        }
    }

    public interface UnitConverter {
        public double getConversionFactor(Unit targetUnit);
    }

    public static class MeterConverter implements UnitConverter {

        public double getConversionFactor(Unit targetUnit) {
            switch (targetUnit) {
            case Meter:
                return 1.0;
            case Kilometer:
                return 1000.0;
            default:
                throw new IllegalArgumentException("No Unit Conversion available from " + Unit.Meter + " to " + targetUnit);
            }
        }
    }

    public static class KilometerConverter implements UnitConverter {

        public double getConversionFactor(Unit targetUnit) {
            switch (targetUnit) {
            case Meter:
                return 0.001;
            case Kilometer:
                return 1.0;
            default:
                throw new IllegalArgumentException("No Unit Conversion available from " + Unit.Kilometer + " to " + targetUnit);
            }
        }
    }

    public static class RadianConverter implements UnitConverter {
        private static final double rad2deg = Math.PI / 180.0;

        public double getConversionFactor(Unit targetUnit) {
            switch (targetUnit) {
            case Radian:
                return 1.0;
            case Degree:
                return rad2deg;
            default:
                throw new IllegalArgumentException("No Unit Conversion available from " + Unit.Radian + " to " + targetUnit);
            }
        }
    }

    public static class DegreeConverter implements UnitConverter {
        private static final double deg2rad = 180.0 / Math.PI;

        public double getConversionFactor(Unit targetUnit) {
            switch (targetUnit) {
            case Degree:
                return 1.0;
            case Radian:
                return deg2rad;
            default:
                throw new IllegalArgumentException("No Unit Conversion available from " + Unit.Degree + " to " + targetUnit);
            }
        }
    }
}
