package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Vector;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.CartesianCoord;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.math.IntervalComparison;
import org.helioviewer.base.math.SphericalCoord;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector3dDouble;
import org.helioviewer.base.math.VectorUtils;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.plugins.hekplugin.math.HEKCoordinateTransform;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
import org.json.JSONException;
import org.json.JSONObject;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

/**
 * The class represents a solar event and manages all the associated
 * information. The current implementation is build around a
 * {@link org.json.JSONObject} object.
 * 
 * @author Malte Nuhn
 * */
public class HEKEvent implements IntervalComparison<Date> {

    public class GenericTriangle<Coordinate> {

        public Coordinate A;
        public Coordinate B;
        public Coordinate C;

        public GenericTriangle(Coordinate a, Coordinate b, Coordinate c) {
            this.A = a;
            this.B = b;
            this.C = c;
        }

    }

    /**
     * Flag to indicate if the event is currently being displayed in any event popup
     */
    private boolean showEventInfo = false;
    
    /**
     * Flag to indicate if the cached triangled have already been calculated
     */
    private boolean cacheValid = false;
    
    /**
     * Cache boundary triangulation
     */
    private Vector<GenericTriangle<SphericalCoord>> cachedTriangles = null;

    /**
     * This field is used as a unique identifier of the event
     */
    private String id = "";

    /**
     * Stores where the event is store in the cache
     */
    private HEKPath path = null;

    /**
     * Stores the duration of the event
     */
    private Interval<Date> duration;

    /**
     * A raw JSONObject to store additional information
     */
    public JSONObject eventObject;

    /**
     * Constructor for an event containing a minimum of information The
     * JSONObject will be empty.
     * 
     * @param id
     *            - id of the new event
     * @param duration
     *            - duration of the event
     */
    public HEKEvent(String id, Interval<Date> duration) {
        this.duration = new Interval<Date>(duration);
        this.id = id;
        eventObject = new JSONObject();
    }

    /**
     * Default constructor. Creates a non valid object, with all member
     * variables ==null.
     */
    public HEKEvent() {
        this.id = null;
        this.setPath(null);
        this.duration = null;
        this.eventObject = null;
    }

    /**
     * Get the id of the event.
     * 
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id of the event.
     * 
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the duration of the event.
     * 
     * @return
     */
    public Interval<Date> getDuration() {
        return duration;
    }

    /**
     * Set the duration of the event.
     * 
     * @param duration
     */
    public void setDuration(Interval<Date> duration) {
        this.duration = duration;
    }

    /**
     * Returns the beginning of the solar event period.
     * 
     * @return time stamp of the beginning of the solar event period.
     */
    public Date getStart() {
        if (this.duration != null) {
            return this.duration.getStart();
        } else {
            return null;
        }
    }

    /**
     * Returns the end of the solar event period.
     * 
     * @return time stamp of the end of the solar event period.
     */
    public Date getEnd() {
        if (this.duration != null) {
            return this.duration.getEnd();
        } else {
            return null;
        }
    }

    public JSONObject getEventObject() {
        return eventObject;
    }

    public void setEventObject(JSONObject eventObject) {
        this.eventObject = eventObject;
    }

    /**
     * Returns an array which contains all available field names of the solar
     * event.
     * 
     * @return array with all available field names of the solar event.
     */
    public String[] getFields() {
        return getNames();
    }

    /**
     * Returns the value of a property as String.
     * 
     * @param key
     *            read the value from this property.
     * @return value of passed property, null if property does not exist
     */
    public String getString(String key) {
        // name = name.toLowerCase();
        try {
            return eventObject.getString(key);
        } catch (JSONException e) {
            Log.fatal("HEKEvent.getString('" + key + "') >> " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the value of a property as Boolean.
     * 
     * @param key
     *            read the value from this property.
     * @return value of passed property, null if property does not exist
     */
    public Boolean getBoolean(String key) {
        // name = name.toLowerCase();
        try {
            return eventObject.getBoolean(key);
        } catch (JSONException e) {
            Log.fatal("HEKEvent.getBoolean('" + key + "') >> " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the value of a property as Double.
     * 
     * @param key
     *            read the value from this property.
     * @return value of passed property.
     * @throws SolarEventException
     */
    public double getDouble(String key) throws HEKEventException {
        try {
            return eventObject.getDouble(key);
        } catch (JSONException e) {
            throw new HEKEventException();
        }
    }

    /**
     * Returns all property names in alphabetic order
     * 
     * @return property names.
     */
    private String[] getNames() {
        String[] names = JSONObject.getNames(eventObject);
        java.util.Arrays.sort(names);
        return names;
    }

    /**
     * Do not change. Used as key for hashtables.
     */
    public String toString() {
        return this.id;
    }

    /**
     * Check whether two events are equal
     */
    public boolean equals(Object other) {
        if (other instanceof HEKEvent) {
            HEKEvent sother = (HEKEvent) other;
            return this.id.equals(sother.id);
        }
        return false;
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.base.math.Interval#contains
     */
    public boolean contains(Interval<Date> other) {
        return this.duration.contains(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.base.math.Interval#containsFully
     */
    public boolean containsFully(Interval<Date> other) {
        return this.duration.containsFully(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.base.math.Interval#containsInclusive
     */
    public boolean containsInclusive(Interval<Date> other) {
        return this.containsInclusive(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.base.math.Interval#containsPoint
     */
    public boolean containsPoint(Date other) {
        return this.duration.containsPoint(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.base.math.Interval#containsPointFully
     */
    public boolean containsPointFully(Date other) {
        return this.duration.containsPointFully(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.base.math.Interval#containsPointInclusive
     */
    public boolean containsPointInclusive(Date other) {
        return this.duration.containsPointInclusive(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.base.math.Interval#overlaps
     */
    public boolean overlaps(Interval<Date> other) {
        return this.duration.overlaps(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.base.math.Interval#overlapsInclusive
     */
    public boolean overlapsInclusive(Interval<Date> other) {
        return this.duration.overlapsInclusive(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.base.math.Interval#compareTo
     */
    public int compareTo(Interval<Date> arg0) {
        return this.duration.compareTo(arg0);
    }

    /**
     * Exception class for exceptions which can occur inside the solar event
     * class. Exceptions which occurred from the internal structure of the solar
     * event class should be mapped to this exception class.
     * 
     * @author Malte Nuhn
     */
    public class HEKEventException extends Exception {

        private static final long serialVersionUID = 1L;
    }

    /**
     * Return the event Coordinates in Heliocentric Stonyhurst coordinates. This
     * might need internal coordinate transformations.
     * 
     * @param now
     *            - point in time for which the coordinates are needed (e.g. for
     *            tracking the event)
     * @return - Heliocentric Stonyhurst spherical coordinates
     */
    public SphericalCoord getStony(Date now) {

        // how many seconds is NOW after the point in time that the coordinates
        // are valid for?
        int timeDifferenceInSeconds = (int) ((now.getTime() - this.getStart().getTime()) / 1000);

        SphericalCoord result = new SphericalCoord();
        try {
            result.phi = this.getDouble("hgs_x");
            result.theta = this.getDouble("hgs_y");
            result.r = Constants.SunRadius;

            // rotate
            return HEKCoordinateTransform.StonyhurstRotateStonyhurst(result, timeDifferenceInSeconds);

        } catch (HEKEventException e) {
            e.printStackTrace();
        }

        // if nothing worked, just return null
        return null;

        /*
         * THE CODE AT THE BOTTOM IS SLOWLY WORKING TOWARDS TRANSFORMING FROM
         * ALL POSSIBLE COORDINATE SYSTEMS PROVIDED... BUT IT IS NOT YET READY,
         * SO LETS USE THE HEK API FOR THIS...
         */

        /*
         * try {
         * 
         * String coordSys = this.getString("Event_CoordSys");
         * System.out.println(coordSys); if (coordSys.equals("UTC-HGS-TOPO")) {
         * double theta = this.getDouble("event_coord2"); double phi =
         * this.getDouble("event_coord1"); double timediff = (now.getTime() -
         * getStart().getTime())/1000.0;
         * 
         * SphericalCoord stony = new
         * SphericalCoord(theta,phi,Constants.SunRadius); SphericalCoord rotated
         * =
         * HEKCoordinateTransform.getSingletonInstance().StonyhurstRotateStonyhurst
         * (stony, timediff); return rotated; } else if
         * (coordSys.equals("UTC-HGC-TOPO")) { double theta =
         * this.getDouble("event_coord2"); double phi =
         * this.getDouble("event_coord1"); double timediff = (now.getTime() -
         * getStart().getTime())/1000.0;
         * 
         * SphericalCoord carrington = new
         * SphericalCoord(theta,phi,Constants.SunRadius); SphericalCoord stony =
         * HEKCoordinateTransform
         * .getSingletonInstance().CarringtonToStonyhurst(carrington,
         * this.getStart()); SphericalCoord rotated =
         * HEKCoordinateTransform.getSingletonInstance
         * ().StonyhurstRotateStonyhurst(stony, timediff); return rotated; }
         * else if (coordSys.equals("UTC-HPC-TOPO")) { double thetax =
         * this.getDouble("event_coord1"); double thetay =
         * this.getDouble("event_coord2"); double timediff = (now.getTime() -
         * getStart().getTime())/1000.0; System.out.println(thetax + " - " +
         * thetay); CartesianCoord heliocentricCart =
         * HEKCoordinateTransform.getSingletonInstance
         * ().HelioProjectiveCartesianToHelioCentricCartesian(thetax,thetay);
         * System.out.println(heliocentricCart + "!!"); SphericalCoord stony =
         * HEKCoordinateTransform
         * .getSingletonInstance().CartesianToSpherical(heliocentricCart);
         * SphericalCoord rotated =
         * HEKCoordinateTransform.getSingletonInstance()
         * .StonyhurstRotateStonyhurst(stony, 0);
         * 
         * System.out.println("----------------");
         * System.out.println(this.getDouble("hgs_y") + " vs " + rotated.theta);
         * System.out.println(this.getDouble("hgs_x") + " vs " + rotated.phi);
         * 
         * rotated.phi = this.getDouble("hgs_x"); rotated.theta =
         * this.getDouble("hgs_y"); rotated.r = Constants.SunRadius;
         * 
         * return rotated;
         * 
         * }
         * 
         * } catch (HEKEventException e) { e.printStackTrace(); } return null;
         */
    }

    /**
     * Check whether the event is on the visible side of the sun Internally
     * requests the events Stonyhurst coordinates and checks if the angle PHI is
     * in the visible range.
     * 
     * @see #getStony
     * @param now
     *            - point in time for which the coordinates are needed (e.g. for
     *            tracking the event)
     * @return
     */
    public boolean isVisible(Date now) {
        SphericalCoord stony = this.getStony(now);
        if (stony == null)
            return true;
        return HEKCoordinateTransform.stonyIsVisible(stony);
    }

    /**
     * Request the screencoordinates of this event
     * 
     * @param now
     *            - point in time for which the coordinates are needed (e.g. for
     *            tracking the event)
     * @return
     */
    public Vector2dDouble getScreenCoordinates(Date now) {
        SphericalCoord stony = this.getStony(now);
        return convertToScreenCoordinates(stony, now);
    }

    /**
     * Converts Stonyhurst coordinates to screencoordinates
     * 
     * @param stony
     *            - coordinates (stonyhurst) to be converted
     * @param now
     *            - time for which the transformation should be done
     * @return converted screen coordinates, (0.0,0.0) if an error occurs
     */
    public static Vector2dDouble convertToScreenCoordinates(SphericalCoord stony, Date now) {

        if (stony == null)
            return new Vector2dDouble(0, 0);

        GregorianCalendar c = new GregorianCalendar();
        c.setTime(now);
        double bzero = Astronomy.getB0InDegree(c);
        double phizero = 0.0; // do we have a value for this?
        CartesianCoord result = HEKCoordinateTransform.StonyhurstToHeliocentricCartesian(stony, bzero, phizero);

        // TODO: Malte Nuhn - Why does the sign of the y-coordinate need to be
        // flipped? However, it works like this
        return new Vector2dDouble(result.x, -result.y);
    }

    /**
     * Converts Stonyhurst coordinates to 3d scenecoordinates with a normalized
     * radius == 1
     * 
     * @param stony
     *            - coordinates (stonyhurst) to be converted
     * @param now
     *            - time for which the transformation should be done
     * @return converted screen coordinates, (0.0,0.0) if an error occurs
     */
    public static Vector3dDouble convertToSceneCoordinates(SphericalCoord stony, Date now) {

        if (stony == null)
            return new Vector3dDouble(0, 0, 0);

        GregorianCalendar c = new GregorianCalendar();
        c.setTime(now);
        double bzero = Astronomy.getB0InDegree(c);
        double phizero = 0.0; // do we have a value for this?
        SphericalCoord normalizedStony = new SphericalCoord(stony);
        normalizedStony.r = Constants.SunRadius;

        CartesianCoord result = HEKCoordinateTransform.StonyhurstToHeliocentricCartesian(normalizedStony, bzero, phizero);

        // TODO: Malte Nuhn - Why does the sign of the y-coordinate need to be
        // flipped? However, it works like this
        return new Vector3dDouble(result.x, -result.y, result.z);
    }

    public static Vector3dDouble convertToSceneCoordinates(SphericalCoord stony, Date now, double factor) {

        if (stony == null)
            return new Vector3dDouble(0, 0, 0);

        GregorianCalendar c = new GregorianCalendar();
        c.setTime(now);
        double bzero = Astronomy.getB0InDegree(c);
        double phizero = 0.0; // do we have a value for this?
        SphericalCoord normalizedStony = new SphericalCoord(stony);
        normalizedStony.r = Constants.SunRadius * factor;

        CartesianCoord result = HEKCoordinateTransform.StonyhurstToHeliocentricCartesian(normalizedStony, bzero, phizero);

        // TODO: Malte Nuhn - Why does the sign of the y-coordinate need to be
        // flipped? However, it works like this
        return new Vector3dDouble(result.x, -result.y, result.z);
    }

    /**
     * @param path
     *            the path to set
     */
    public void setPath(HEKPath path) {
        this.path = path;
    }

    /**
     * @return the path
     */
    public HEKPath getPath() {
        return path;
    }
    
    /**
     * @return true if the current event is currently being displayed in a popup window
     */
    public boolean getShowEventInfo() {
        return showEventInfo;
    }
    
    /**
     * update status: true if the current event is currently being displayed in a popup window
     */
    public void setShowEventInfo(boolean show) {
        this.showEventInfo = show;
    }
    

    private Vector<SphericalCoord> toStonyPolyon(String poly, Date now) {

        Vector<SphericalCoord> result = new Vector<SphericalCoord>();
        poly = poly.trim();

        if (!(poly.startsWith("POLYGON((") && poly.endsWith(")"))) {
            return null;
        }

        poly = poly.substring(9);
        poly = poly.substring(0, poly.length() - 2);

        Scanner s = new Scanner(poly);
        s.useLocale(Locale.ENGLISH);
        s.useDelimiter("[ ,]");

        int pos = 0;
        while (s.hasNextDouble()) {
            pos++;
            double firstCoordinate = s.nextDouble();
            if (!s.hasNextDouble()) {
                Log.fatal("Inconsistent polygon string...");
                break;
            }
            double secondCoordinate = s.nextDouble();

            SphericalCoord stony = new SphericalCoord(secondCoordinate, firstCoordinate, Constants.SunRadius);

            result.add(stony);
        }

        return result;
    }

    public Vector<SphericalCoord> getStonyBound(Date now) {

        try {
            if (this.eventObject.has("hgs_boundcc") && !this.eventObject.getString("hgs_boundcc").equals("")) {
                return this.toStonyPolyon(this.eventObject.getString("hgs_boundcc"), this.getStart());
            }
            // uncomment if we would like to draw rectangular bounds, too
            /*
             * else if (this.eventObject.has("hgs_bbox") &&
             * !this.eventObject.getString("hgs_bbox").equals("")) {
             * Log.info("Object has hgs_bbox"); return
             * this.toStonyPolyon(this.eventObject.getString("hgs_bbox"), now);
             * }
             */
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * Caluclates a triangulation once - for the original position. Then
     * interpolates this triangultion by moving the points
     * 
     * @param now
     * @return
     */
    public Vector<GenericTriangle<Vector3dDouble>> getTriangulation3D(Date now) {

        int timeDifferenceInSeconds = (int) ((now.getTime() - this.getStart().getTime()) / 1000);

        if (!cacheValid) {
            return null;
        }

        if (cachedTriangles != null) {

            Vector<GenericTriangle<Vector3dDouble>> result = new Vector<GenericTriangle<Vector3dDouble>>();

            for (GenericTriangle<SphericalCoord> triangle : cachedTriangles) {
            	
            	Vector3dDouble A = HEKEvent.convertToSceneCoordinates(triangle.A, now);
            	Vector3dDouble B = HEKEvent.convertToSceneCoordinates(triangle.B, now);
            	Vector3dDouble C = HEKEvent.convertToSceneCoordinates(triangle.C, now);
				
                result.add(new GenericTriangle<Vector3dDouble>(A, B, C));
            }
            return result;
        } else {
            return null;
        }
    }

    public Vector<GenericTriangle<Vector2dDouble>> getTriangulation(Date now) {

        int timeDifferenceInSeconds = (int) ((now.getTime() - this.getStart().getTime()) / 1000);

        if (!cacheValid) {
            return null;
        }

        if (cachedTriangles != null) {

            Vector<GenericTriangle<Vector2dDouble>> result = new Vector<GenericTriangle<Vector2dDouble>>();

            for (GenericTriangle<SphericalCoord> triangle : cachedTriangles) {

                SphericalCoord rotatedA = HEKCoordinateTransform.StonyhurstRotateStonyhurst(triangle.A, timeDifferenceInSeconds);
                SphericalCoord rotatedB = HEKCoordinateTransform.StonyhurstRotateStonyhurst(triangle.B, timeDifferenceInSeconds);
                SphericalCoord rotatedC = HEKCoordinateTransform.StonyhurstRotateStonyhurst(triangle.C, timeDifferenceInSeconds);
                
                // ignore triangles where any of the points is on the backside
                // of the Sun
                // if (Math.abs(rotatedA.phi) > 90 || Math.abs(rotatedB.phi) >
                // 90 || Math.abs(rotatedC.phi) > 90)
                // continue;

                Vector2dDouble A = HEKEvent.convertToScreenCoordinates(rotatedA, now);
                Vector2dDouble B = HEKEvent.convertToScreenCoordinates(rotatedB, now);
                Vector2dDouble C = HEKEvent.convertToScreenCoordinates(rotatedC, now);

                result.add(new GenericTriangle<Vector2dDouble>(A, B, C));
            }
            return result;
        } else {
            return null;
        }
    }
    
    private boolean inTriangles(Vector2dDouble point, List<DelaunayTriangle> triangles) {
        for (DelaunayTriangle triangle : triangles) {
            TriangulationPoint A = triangle.points[0];
            TriangulationPoint B = triangle.points[1];
            TriangulationPoint C = triangle.points[2];

            Vector2dDouble A2 = new Vector2dDouble(A.getX(), A.getY());
            Vector2dDouble B2 = new Vector2dDouble(B.getX(), B.getY());
            Vector2dDouble C2 = new Vector2dDouble(C.getX(), C.getY());

            if (VectorUtils.pointInTriangle(A2, B2, C2, point)) {
                return true;
            }
        }

        return false;
    }

    private void cacheTriangulation() {
        Date now = this.getStart();

        if (now != null) {

            // the central point of the event
            SphericalCoord projectionCenter = this.getStony(now);
            Vector<SphericalCoord> outerBound = this.getStonyBound(now);

            if (outerBound != null) {

                // needed to define the plane projection
                Vector3dDouble projectionCenterCartesian = HEKEvent.convertToSceneCoordinates(projectionCenter, now);

                SphericalCoord firstPoint = outerBound.get(0);
                Vector3dDouble firstPointCartesian = HEKEvent.convertToSceneCoordinates(firstPoint, now);
                Vector3dDouble projectionPlaneVectorA = VectorUtils.inPlaneShift(projectionCenterCartesian, firstPointCartesian).normalize();
                Vector3dDouble projectionPlaneVectorB = Vector3dDouble.cross(projectionPlaneVectorA, projectionCenterCartesian.normalize()).normalize();

                // if we have less than three points, do nothing
                if (outerBound.size() < 3) {
                    cacheValid = true;
                    return;
                }

                // if first and last point are the same, remove the last one
                if (outerBound.get(0).equals(outerBound.get(outerBound.size() - 1))) {
                    outerBound.remove(outerBound.size() - 1);
                }

                // Setup the Polygon Boundary (External Library)
                Vector<PolygonPoint> simplePolygonPoints = new Vector<PolygonPoint>();

                // needed to map back triangles
                Vector<Vector3dDouble> outerBoundCartesian = new Vector<Vector3dDouble>();

                for (SphericalCoord boundaryPoint : outerBound) {
                    Vector3dDouble boundaryPointCartesian = HEKEvent.convertToSceneCoordinates(boundaryPoint, now);
                    outerBoundCartesian.add(boundaryPointCartesian);

                    Vector2dDouble projected = VectorUtils.inPlaneCoord(projectionCenterCartesian, projectionPlaneVectorA, projectionPlaneVectorB, boundaryPointCartesian);
                    simplePolygonPoints.add(new PolygonPoint(projected.getX(), projected.getY()));
                }

                try {
                    // finally triangulate
                    Polygon simplePolygon = new Polygon(simplePolygonPoints);
                    Polygon advancedPolygon = new Polygon(simplePolygonPoints);

                    Poly2Tri.triangulate(simplePolygon);
                    List<DelaunayTriangle> simplePolygonTriangles = simplePolygon.getTriangles();

                    // add sun border points
                    Vector<SphericalCoord> sunBorder = generateSunBorder();

                    // needed to map back triangles
                    Vector<Vector3dDouble> sunBorderCartesian = new Vector<Vector3dDouble>();

                    for (SphericalCoord sunBoundaryPoint : sunBorder) {
                        Vector3dDouble sunBoundaryPointCartesian = HEKEvent.convertToSceneCoordinates(sunBoundaryPoint, now);
                        sunBorderCartesian.add(sunBoundaryPointCartesian);

                        Vector2dDouble projected = VectorUtils.inPlaneCoord(projectionCenterCartesian, projectionPlaneVectorA, projectionPlaneVectorB, sunBoundaryPointCartesian);
                        // if this point is contained in any of the triangles,
                        // add it as a steiner point
                        if (this.inTriangles(projected, simplePolygonTriangles)) {
                            advancedPolygon.addSteinerPoint(new PolygonPoint(projected.getX(), projected.getY()));
                        }
                    }

                    // triangulate advanced polygon
                    Poly2Tri.triangulate(advancedPolygon);

                    List<DelaunayTriangle> advancedPolygonTriangles = advancedPolygon.getTriangles();

                    // setup vector of triangles, after we know everything
                    // worked out
                    cachedTriangles = new Vector<GenericTriangle<SphericalCoord>>();

                    Vector<SphericalCoord> lookupSpherical = new Vector<SphericalCoord>();
                    Vector<Vector3dDouble> lookupCartesian = new Vector<Vector3dDouble>();

                    lookupSpherical.addAll(sunBorder);
                    lookupCartesian.addAll(sunBorderCartesian);

                    lookupSpherical.addAll(outerBound);
                    lookupCartesian.addAll(outerBoundCartesian);

                    for (DelaunayTriangle triangle : advancedPolygonTriangles) {
                        TriangulationPoint A = triangle.points[0];
                        TriangulationPoint B = triangle.points[1];
                        TriangulationPoint C = triangle.points[2];

                        Vector2dDouble A2 = new Vector2dDouble(A.getX(), A.getY());
                        Vector2dDouble B2 = new Vector2dDouble(B.getX(), B.getY());
                        Vector2dDouble C2 = new Vector2dDouble(C.getX(), C.getY());

                        Vector3dDouble A3 = VectorUtils.projectBack(projectionCenterCartesian, projectionPlaneVectorA, projectionPlaneVectorB, A2).normalize().scale(Constants.SunRadius);
                        Vector3dDouble B3 = VectorUtils.projectBack(projectionCenterCartesian, projectionPlaneVectorA, projectionPlaneVectorB, B2).normalize().scale(Constants.SunRadius);
                        Vector3dDouble C3 = VectorUtils.projectBack(projectionCenterCartesian, projectionPlaneVectorA, projectionPlaneVectorB, C2).normalize().scale(Constants.SunRadius);

                        // skip (party) hidden triangles
                        if (A3.getZ() < 0 || B3.getZ() < 0 || C3.getZ() < 0)
                            continue;

                        SphericalCoord A4 = findClosest(A3, lookupCartesian, lookupSpherical);
                        SphericalCoord B4 = findClosest(B3, lookupCartesian, lookupSpherical);
                        SphericalCoord C4 = findClosest(C3, lookupCartesian, lookupSpherical);

                        cachedTriangles.add(new GenericTriangle<SphericalCoord>(A4, B4, C4));

                    }

                    // loop over generated polygons
                } catch (Exception e) {
                    Log.warn("Error during Triangulation");
                    Log.debug("Error during Triangulation", e);
                    cachedTriangles = null;
                }
            }

        } else {
            Log.info("Event has no valid timing information");
        }

        cacheValid = true;

    }

    private SphericalCoord findClosest(Vector3dDouble toFind, Vector<Vector3dDouble> lookupCartesian, Vector<SphericalCoord> lookupSpherical) {
        double closest = Double.MAX_VALUE;
        int closest_index = 0;

        for (int i = 0; i < lookupCartesian.size(); i++) {
            double distance = toFind.subtract(lookupCartesian.get(i)).length();

            if (distance < closest) {
                closest_index = i;
                closest = distance;
            }

        }

        return lookupSpherical.get(closest_index);
    }

    public BufferedImage getIcon(boolean large) {

        boolean human = this.getBoolean("frm_humanflag");
        String type = this.getString("event_type");

        BufferedImage toDraw = HEKConstants.getSingletonInstance().acronymToBufferedImage(type, large);

        if (toDraw == null) {
            toDraw = HEKConstants.getSingletonInstance().acronymToBufferedImage(HEKConstants.ACRONYM_FALLBACK, large);
        }

        // overlay the human icon
        if (human) {
            BufferedImage stackImg = HEKConstants.getSingletonInstance().getOverlayBufferedImage("human", large);
            BufferedImage[] stack = { toDraw, stackImg };
            toDraw = IconBank.stackImages(stack, 1.0, 1.0);
        }

        return toDraw;
    }

    private Vector<SphericalCoord> generateSunBorder() {
        Vector<SphericalCoord> borderPoints = new Vector<SphericalCoord>();

        for (double theta = 0.0; theta < 90; theta += 2) {
            borderPoints.add(new SphericalCoord(theta, 89.8, Constants.SunRadius));
            borderPoints.add(new SphericalCoord(theta, 90.0, Constants.SunRadius));
            borderPoints.add(new SphericalCoord(theta, -89.8, Constants.SunRadius));
            borderPoints.add(new SphericalCoord(theta, -90.0, Constants.SunRadius));
        }

        return borderPoints;
    }

    public void prepareCache() {
        cacheTriangulation();
    }

    /* **************************************************************************
     * 
     * Some legacy code to add additional points in between the loaded polygon
     * points
     * 
     * **************************************************************************
     * 
     * SphericalCoord old = null;
     * 
     * for (SphericalCoord c : bound) { c =
     * HEKCoordinateTransform.getSingletonInstance
     * ().StonyhurstRotateStonyhurst(c, timeDifferenceInSeconds); if (old !=
     * null) { double dphi = c.phi - old.phi; double dtheta = c.theta -
     * old.theta; double dr = c.r - old.r;
     * 
     * dr /= 10.0; dphi /= 10.0; dtheta /= 10.0;
     * 
     * for (int i = 0; i < 10; i++) { double theta = old.theta + dtheta * i;
     * double phi = old.phi + dphi * i; double r = old.r + dr * i;
     * 
     * if (phi > 90) phi = 90; if (phi < -90) phi = -90;
     * 
     * SphericalCoord n = new SphericalCoord(theta, phi, r);
     * interpolated.add(n); }
     * 
     * } else { interpolated.add(c); } old = c; }
     * 
     * interpolated.add(old);
     */

    /* **************************************************************************
     * 
     * Some legacy code that generates additional points on the edge of the
     * visible side of the sun
     * 
     * **************************************************************************
     */

}
