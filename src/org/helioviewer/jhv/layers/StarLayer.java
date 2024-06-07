package org.helioviewer.jhv.layers;

import java.awt.Component;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class StarLayer extends AbstractLayer {

private static final Vec3[] coords_2008 = new Vec3[]{
     new Vec3(224.64701172, -19.72610652, 602.30078904),
     new Vec3(220.68759886, -19.3116723 , 173.98566361),
     new Vec3(222.96374337, -18.35533235, 293.28953543),
     new Vec3(222.89561147, -17.79067819,  54.15533919),
     new Vec3(221.47099553, -17.69855125, 222.70722915),
     new Vec3(225.37888626, -18.80324316, 440.91710761),
     new Vec3(225.77693646, -17.63304722, 126.93254806),
     new Vec3(226.70426099, -16.48440059, 116.25744049),
     new Vec3(226.6565611 , -16.25687666, 174.43787397),
     new Vec3(224.52180885, -17.36420314, 166.93654744),
     new Vec3(222.67136857, -15.99738342,  23.36847173),
     new Vec3(222.52657745, -15.5258249 , 176.27979129),
     new Vec3(221.49057001, -15.45954526, 172.99541563),
     new Vec3(220.13401174, -14.04676651, 397.94659557),
     new Vec3(222.32921688, -14.14906056,  75.67960284),
     new Vec3(222.32926951, -14.14852483,  75.42331335),
     new Vec3(221.97909131, -12.84023643, 119.7060021 )
     };

private static final Vec3[] coords_2014 = new Vec3[]{
     new Vec3(207.58688524, -13.11027568, 529.82939494),
     new Vec3(207.89595212, -13.01509019, 162.79751247),
     new Vec3(209.61238178, -12.05988275,  80.0826453 ),
     new Vec3(208.53301213, -11.69530537, 537.89468023),
     new Vec3(206.48459512, -12.42649977, 106.62231179),
     new Vec3(207.70450116, -11.35879114, 506.55995137),
     new Vec3(210.88048424, -10.73131901, 486.40498079),
     new Vec3(212.25244065, -10.33446273, 127.83636945),
     new Vec3(211.67786176,  -9.31349923,  57.13045168),
     new Vec3(212.15703222,  -8.86191022, 549.87352909),
     new Vec3(208.97134435,  -9.75523577,  58.06560252),
     new Vec3(208.94972471,  -9.55994304, 225.91722393),
     new Vec3(211.0892857 ,  -9.25771511, 133.75063532),
     new Vec3(211.5740458 ,  -8.8915382 , 156.61461841),
     new Vec3(211.11227065, -14.97178315, 130.85237235),
     new Vec3(211.30388025, -14.8572468 , 382.33607348),
     new Vec3(211.67349293, -14.20495789, 241.14398708),
     new Vec3(209.62438834, -14.1220675 , 126.63517672),
     new Vec3(213.29056726, -13.86002064, 333.60021351),
     new Vec3(213.42917467, -11.83845288, 193.14713949),
     new Vec3(212.12068861, -11.82947545, 116.73242593),
     new Vec3(212.83002743, -10.47904703, 460.19328118),
     new Vec3(213.22396434, -10.27314978,  70.62246642)
     };

private static final Vec3[] coords_2017 = new Vec3[]{
     new Vec3(311.5417325 , -21.51412483,  89.24029735),
     new Vec3(309.60631399, -20.99431395, 113.76305431),
     new Vec3(309.11038481, -20.58555298, 348.32282559),
     new Vec3(312.0572052 , -19.26041634,  86.81761356),
     new Vec3(312.10836312, -18.20175757, 196.69938433),
     new Vec3(312.33560237, -18.03602195, 247.79462781),
     new Vec3(307.67099414, -20.70979146, 227.35023303),
     new Vec3(305.11866072, -19.64739039, 178.85248247),
     new Vec3(306.2685349 , -19.43370785, 161.66319091),
     new Vec3(304.84837716, -19.1185776 , 326.701297  ),
     new Vec3(305.25515349, -18.32400246, 409.23228024),
     new Vec3(307.46929069, -18.58675525,  71.11060543),
     new Vec3(307.47477155, -18.58357763,  71.62348964),
     new Vec3(306.83009241, -18.21177807, 201.55601241),
     new Vec3(307.25308155, -17.87597802, 129.67477564),
     new Vec3(307.21497614, -17.81373387,  30.01822106),
     new Vec3(307.21489173, -17.81419122,  29.49260915),
     new Vec3(310.01215174, -18.1387647 , 223.70363742),
     new Vec3(310.3297736 , -17.37848733, 308.7944664 ),
     new Vec3(307.83449847, -17.13863353, 498.25610364),
     new Vec3(308.88482949, -16.52601508,  75.41193771),
     new Vec3(307.24581695, -17.43487231, 154.9186677 ),
     new Vec3(310.13515384, -16.12381214,  97.74120084),
     new Vec3(309.33863353, -15.14756657, 225.64704287),
     new Vec3(307.18166535, -15.74149766, 202.99622427),
     new Vec3(307.76775399, -15.05641603, 162.32975666),
     new Vec3(312.05362371, -16.52160736, 242.13661346),
     new Vec3(311.31279032, -15.79822385,  62.75179156)
     };

private static final Vec3[] coords_2021 = new Vec3[]{
     new Vec3(22.5480558 ,  6.14354781, 101.56202373),
     new Vec3(24.1718373 ,  6.71787008, 229.07408255),
     new Vec3(24.18138543,  7.83158213, 290.56252905),
     new Vec3(22.26274992,  7.29376075,  74.27764985),
     new Vec3(22.09616054,  7.96131053, 119.33031821),
     new Vec3(23.32625763,  8.20854526, 155.29638314),
     new Vec3(23.71907026,  8.27479898, 235.22770041),
     new Vec3(26.42725571,  8.55910617, 162.92482648),
     new Vec3(25.14591735,  8.76082604,  53.24388361),
     new Vec3(25.35523114,  9.07303703, 393.84033712),
     new Vec3(26.34886764,  9.15806949,  76.32888589),
     new Vec3(22.92816115, 10.88950952, 185.55285471),
     new Vec3(24.42106154, 12.078417  ,  25.46356418),
     new Vec3(24.27426291, 12.1418582 ,  33.29338127),
     new Vec3(23.13886357, 11.88253279, 411.45490446),
     new Vec3(20.72361502, 11.36622778,  92.09884047),
     new Vec3(20.33061612, 11.53674268, 113.66994793),
     new Vec3(20.49312187, 12.604146  ,  81.71937566)
     };

    private final JHVTime time_2008 = new JHVTime("2008-09-28T22:07:43.016");
    private final JHVTime time_2014 = new JHVTime("2014-05-15T07:54:13.006");
    private final JHVTime time_2017 = new JHVTime("2017-06-12T13:54:13.006");
    private final JHVTime time_2021 = new JHVTime("2021-06-04T20:23:43.009");

    private static final Vec3[] coords = coords_2017;
    private final JHVTime time = time_2017;

    private final double[][] stars;

    private static final float SIZE_POINT = 0.10f;
    private final BufVertex pointsBuf = new BufVertex(96 * GLSLShape.stride);
    private final GLSLShape points = new GLSLShape(true);

    @Override
    public void serialize(JSONObject jo) {
    }

    public StarLayer(JSONObject jo) {
        stars = computeAngles("STEREO AHEAD");
    }

    // https://celestialscenes.com/alma/convert/coordconvert.pdf
    private static void calcProj(double P, double alpha, double delta, double alpha0, double delta0, double[] theta) {
        double phi = Math.atan2(Math.sin(alpha - alpha0), Math.tan(delta) * Math.cos(delta0) - Math.sin(delta0) * Math.cos(alpha - alpha0));

        double num = Math.hypot(Math.cos(delta) * Math.sin(alpha - alpha0), Math.cos(delta0) * Math.sin(delta) - Math.sin(delta0) * Math.cos(delta) * Math.cos(alpha - alpha0));
        double den = Math.sin(delta) * Math.sin(delta0) + Math.cos(delta) * Math.cos(delta0) * Math.cos(alpha - alpha0);
        double rho = Math.atan2(num, den);

        theta[0] = Math.atan(-num/den * Math.sin(phi - P));
        theta[1] = Math.asin(Math.sin(rho) * Math.cos(phi - P));
    }

    private final double[][] planets = new double[2][];

    private double[][] computeAngles(String spacecraft) {
        double[][] mat = Spice.twovecSun(spacecraft, time);

        double[] planet;

        planet = Spice.posRad(spacecraft, "VENUS", time, mat);
        planet[1] = Math.tan(planet[1]);
        planet[2] = Math.tan(planet[2]);
        planets[0] = planet;

        planet = Spice.posRad(spacecraft, "MARS BARYCENTER", time, mat);
        planet[1] = Math.tan(planet[1]);
        planet[2] = Math.tan(planet[2]);
        planets[1] = planet;

        int num = coords.length;
        double[][] ret = new double[num][];
        for (int i = 0; i < num; i++) {
            double ra  = Math.toRadians(coords[i].x);
            double dec = Math.toRadians(coords[i].y);

            double[] a = Spice.radRotate(ra, dec, mat);
            a[1] = Math.tan(a[1]);
            a[2] = Math.tan(a[2]);
            ret[i] = a;
        }
        return ret;
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        Position viewpoint = camera.getViewpoint();
        double dist = viewpoint.distance;
        for (double[] a : stars) {
            double x = -dist * a[1];
            double y =  dist * a[2];
            pointsBuf.putVertex((float) x, (float) y, 0, 2 * SIZE_POINT, Colors.Blue);
        }

        for (double[] a : planets) {
            double x = -dist * a[1];
            double y =  dist * a[2];
            pointsBuf.putVertex((float) x, (float) y, 0, 2 * SIZE_POINT, Colors.Green);
        }

        points.setVertex(gl, pointsBuf);

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());
        points.renderPoints(gl, CameraHelper.getPixelFactor(camera, vp));
        Transform.popView();
    }

    @Override
    public void init(GL2 gl) {
        points.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        points.dispose(gl);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return "Stars";
    }

}
