package org.helioviewer.gl3d.wcs;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.physics.Astronomy;

public class HEEQCoordinateSystem extends Cartesian3DCoordinateSystem implements CoordinateSystem {
    private Date observationDate;

    private double b0;

    public HEEQCoordinateSystem(Date observationDate) {
        this.observationDate = observationDate;
        Calendar cal = new GregorianCalendar();
        cal.setTime(observationDate);
        double b0 = Astronomy.getB0InRadians(cal);
        this.b0 = b0;
    }

    public HEEQCoordinateSystem(double b0) {
        this.b0 = b0;
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof HeliocentricCartesianCoordinateSystem) {
            return new HEEQToHeliocentricCartesianConversion(this, (HeliocentricCartesianCoordinateSystem) coordinateSystem);
        }
        return super.getConversion(coordinateSystem);
    }

    public Date getObservationDate() {
        return this.observationDate;
    }

    public void setObservationDate(Date observationDate) {
        this.observationDate = observationDate;
    }

    public double getB0() {
        return this.b0;
    }
}
