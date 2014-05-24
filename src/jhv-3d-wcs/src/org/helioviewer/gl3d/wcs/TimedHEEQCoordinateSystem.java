package org.helioviewer.gl3d.wcs;

import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class TimedHEEQCoordinateSystem extends Cartesian3DCoordinateSystem {

    private ImmutableDateTime datetime;
    private final double extraLongitude;
    private final double extraLatitude;

    public TimedHEEQCoordinateSystem(ImmutableDateTime datetime, double extraLatitude, double extraLongitude) {
        this.setDatetime(datetime);
        this.extraLongitude = extraLongitude;
        this.extraLatitude = extraLatitude;
    }

    @Override
    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof HeliocentricCartesian2000CoordinateSystem) {
            return new TimedHEEQToHeliocentricCartesian2000Conversion(this, (HeliocentricCartesian2000CoordinateSystem) coordinateSystem);
        }
        return super.getConversion(coordinateSystem);
    }

    public ImmutableDateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(ImmutableDateTime datetime) {
        this.datetime = datetime;
    }

    public double getExtraLongitude() {
        return extraLongitude;
    }

    public double getExtraLatitude() {
        return extraLatitude;
    }
}
