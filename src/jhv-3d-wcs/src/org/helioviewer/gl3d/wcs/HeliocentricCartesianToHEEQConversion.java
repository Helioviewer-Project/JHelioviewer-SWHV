package org.helioviewer.gl3d.wcs;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.helioviewer.base.physics.Astronomy;

public class HeliocentricCartesianToHEEQConversion implements CoordinateConversion {
    private HEEQCoordinateSystem heeq;
    private HeliocentricCartesianCoordinateSystem source;

    public HeliocentricCartesianToHEEQConversion(HeliocentricCartesianCoordinateSystem source, HEEQCoordinateSystem heeq) {
        this.heeq = heeq;
        this.source = source;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double x = vector.getValue(HeliocentricCartesianCoordinateSystem.X_COORDINATE);
        double y = vector.getValue(HeliocentricCartesianCoordinateSystem.Y_COORDINATE);
        double z = vector.getValue(HeliocentricCartesianCoordinateSystem.Z_COORDINATE);

        Calendar observationDate = new GregorianCalendar();
        observationDate.setTime(this.heeq.getObservationDate());
        double b0 = Astronomy.getB0InRadians(observationDate);

        double xheeq = z * Math.cos(b0) - y * Math.sin(b0);
        double yheeq = x;
        double zheeq = z * Math.sin(b0) + y * Math.cos(b0);

        return this.heeq.createCoordinateVector(xheeq, yheeq, zheeq);
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return this.source;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.heeq;
    }

}
