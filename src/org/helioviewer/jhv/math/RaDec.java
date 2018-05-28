package org.helioviewer.jhv.math;

public class RaDec {

    public final double range;
    public final double ra;
    public final double dec;
    private Quat q;

    public RaDec(double _range, double _ra, double _dec) {
        range = _range;
        ra = _ra;
        dec = _dec;
    }

    public RaDec(Vec3 p) {
        range = p.length();
        dec = Math.asin(p.y / range);
        ra = Math.atan2(p.z, p.x);
    }

    public Vec3 toVec3() {
        return new Vec3(range * Math.cos(ra) * Math.cos(dec),
                        range * Math.sin(ra),
                        range * Math.cos(ra) * Math.sin(dec));
    }

    public Quat toQuat() {
        if (q == null)
            q = new Quat(dec, ra);
        return q;
    }

}
