package org.helioviewer.gl3d.wcs;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class TimedHeliocentricCartesianToHeliocentricCartesian2000Conversion implements CoordinateConversion {
    private TimedHeliocentricCartesianCoordinateSystem  source;
    private HeliocentricCartesian2000CoordinateSystem  h2000;
    public TimedHeliocentricCartesianToHeliocentricCartesian2000Conversion(TimedHeliocentricCartesianCoordinateSystem source, HeliocentricCartesian2000CoordinateSystem h2000) {
        this.h2000 = h2000;
        this.source = source;
    }
	@Override
	public CoordinateSystem getSourceCoordinateSystem() {
		return this.source;
	}

	@Override
	public CoordinateSystem getTargetCoordinateSystem() {
		return (CoordinateSystem)(this.h2000);
	}

	@Override
	public CoordinateVector convert(CoordinateVector vector) {
        double x = vector.getValue(TimedHeliocentricCartesianCoordinateSystem.X_COORDINATE);
        double y = vector.getValue(TimedHeliocentricCartesianCoordinateSystem.Y_COORDINATE);
        double z = vector.getValue(TimedHeliocentricCartesianCoordinateSystem.Z_COORDINATE);

        ImmutableDateTime dt = source.getDatetime();
        Calendar cal = new GregorianCalendar();
        cal.setTime(dt.getTime());
        
        double b0 = Astronomy.getB0InRadians(cal);
        b0=0.0;


        double xheeq = z * Math.cos(b0) - y * Math.sin(b0);
        double yheeq = x;
        double zheeq = z * Math.sin(b0) + y * Math.cos(b0);
        
        cal.set(2000, 1, 1, 0, 0, 0);
        b0 = Astronomy.getB0InRadians(cal);

        b0=0.0;

        double timediff = (dt.getMillis()-cal.getTime().getTime())/1000.0;
        double rot = -DifferentialRotation.calculateRotationInRadians(0, timediff);
        rot = 0.0;
        double xp = xheeq * Math.cos(rot) - yheeq * Math.sin(rot);
        double yp = xheeq * Math.sin(rot) + yheeq * Math.cos(rot);
        double zp = zheeq;
        
        double xf = yp;  
        double yf = zp * Math.cos(b0) - xp * Math.sin(b0);
        double zf = zp * Math.sin(b0) + xp * Math.cos(b0);

        return this.h2000.createCoordinateVector(xf, yf, zf);
	}


}
