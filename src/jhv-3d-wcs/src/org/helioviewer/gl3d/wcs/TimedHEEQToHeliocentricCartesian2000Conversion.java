package org.helioviewer.gl3d.wcs;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class TimedHEEQToHeliocentricCartesian2000Conversion implements CoordinateConversion {
    private final TimedHEEQCoordinateSystem source;
    private final HeliocentricCartesian2000CoordinateSystem h2000;

    public TimedHEEQToHeliocentricCartesian2000Conversion(TimedHEEQCoordinateSystem source, HeliocentricCartesian2000CoordinateSystem h2000) {
        this.h2000 = h2000;
        this.source = source;
    }

    @Override
    public CoordinateSystem getSourceCoordinateSystem() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CoordinateSystem getTargetCoordinateSystem() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CoordinateVector convert(CoordinateVector vector) {
        double x = vector.getValue(TimedHEEQCoordinateSystem.X_COORDINATE);
        double y = vector.getValue(TimedHEEQCoordinateSystem.Y_COORDINATE);
        double z = vector.getValue(TimedHEEQCoordinateSystem.Z_COORDINATE);

        ImmutableDateTime dt = source.getDatetime();
        Calendar cal = new GregorianCalendar();
        cal.setTime(dt.getTime());

        double b0 = Astronomy.getB0InRadians(cal);
        double b0extra = b0 + source.getExtraLatitude();
        double xheeq = z * Math.cos(b0extra) - y * Math.sin(b0extra);
        double yheeq = x;
        double zheeq = z * Math.sin(b0extra) + y * Math.cos(b0extra);

        double timediff = (dt.getMillis() - cal.getTime().getTime()) / 1000.0;
        double rot = source.getExtraLongitude();
        double xp = xheeq * Math.cos(rot) - yheeq * Math.sin(rot);
        double yp = xheeq * Math.sin(rot) + yheeq * Math.cos(rot);
        double zp = zheeq;

        double xf = z * Math.cos(-b0) - y * Math.sin(-b0);
        double yf = x;
        double zf = z * Math.sin(-b0) + y * Math.cos(-b0);

        return this.h2000.createCoordinateVector(xf, yf, zf);
    }

}
