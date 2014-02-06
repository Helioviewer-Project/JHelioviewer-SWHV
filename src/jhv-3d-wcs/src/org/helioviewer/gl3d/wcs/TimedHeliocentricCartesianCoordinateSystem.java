package org.helioviewer.gl3d.wcs;

import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;


public class TimedHeliocentricCartesianCoordinateSystem extends Cartesian3DCoordinateSystem {

    private ImmutableDateTime datetime;
    
	public TimedHeliocentricCartesianCoordinateSystem(ImmutableDateTime datetime) {
        this.setDatetime(datetime);
    }
    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof HeliocentricCartesian2000CoordinateSystem) {
            return new TimedHeliocentricCartesianToHeliocentricCartesian2000Conversion(this, (HeliocentricCartesian2000CoordinateSystem) coordinateSystem);
        }
        return super.getConversion(coordinateSystem);
    }
	public ImmutableDateTime getDatetime() {
		return datetime;
	}
	public void setDatetime(ImmutableDateTime datetime) {
		this.datetime = datetime;
	}
}
