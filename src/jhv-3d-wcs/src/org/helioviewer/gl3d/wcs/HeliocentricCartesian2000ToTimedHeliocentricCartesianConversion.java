package org.helioviewer.gl3d.wcs;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class HeliocentricCartesian2000ToTimedHeliocentricCartesianConversion implements CoordinateConversion {
    private TimedHeliocentricCartesianCoordinateSystem hc;
    private HeliocentricCartesian2000CoordinateSystem source;

    public HeliocentricCartesian2000ToTimedHeliocentricCartesianConversion(HeliocentricCartesian2000CoordinateSystem source, TimedHeliocentricCartesianCoordinateSystem  hc) {
        this.hc = hc;
        this.source = source;
    }
	@Override
	public CoordinateSystem getSourceCoordinateSystem() {
		return this.source;
	}

	@Override
	public CoordinateSystem getTargetCoordinateSystem() {
		return (CoordinateSystem)(this.hc);
	}

	@Override
	public CoordinateVector convert(CoordinateVector vector) {
        double x = vector.getValue(TimedHeliocentricCartesianCoordinateSystem.X_COORDINATE);
        double y = vector.getValue(TimedHeliocentricCartesianCoordinateSystem.Y_COORDINATE);
        double z = vector.getValue(TimedHeliocentricCartesianCoordinateSystem.Z_COORDINATE);

        Calendar cal = new GregorianCalendar();
        cal.set(2000, 1, 1, 0, 0, 0);
        
        
        double b0 = Astronomy.getB0InRadians(cal);
        b0=0.0;

        double xheeq = z * Math.cos(b0) - y * Math.sin(b0);
        double yheeq = x;
        double zheeq = z * Math.sin(b0) + y * Math.cos(b0);
        
        ImmutableDateTime dt = hc.getDatetime();
        double timediff = (dt.getMillis()-cal.getTime().getTime())/1000.0;

        cal.setTime(dt.getTime());
        b0 = Astronomy.getB0InRadians(cal);
        b0=0.0;

        double rot = DifferentialRotation.calculateRotationInRadians(0, timediff);
        rot = 0.0;

        double xp = xheeq * Math.cos(rot) - yheeq * Math.sin(rot);
        double yp = xheeq * Math.sin(rot) + yheeq * Math.cos(rot);
        double zp = zheeq;
        
        double xf = yp;  
        double yf = zp * Math.cos(b0) - xp * Math.sin(b0);
        double zf = zp * Math.sin(b0) + xp * Math.cos(b0);

        return this.hc.createCoordinateVector(xf, yf, zf);
	}

}
