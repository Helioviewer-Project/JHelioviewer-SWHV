package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVDatabase;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;
import org.helioviewer.jhv.data.datatype.event.SWEKRelatedEvents;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;
import org.helioviewer.jhv.plugins.swek.sources.SWEKParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parser able to parse events coming from the HEK server.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class HEKParser implements SWEKParser {

    private boolean parserStopped;

    private SWEKEventType eventType;

    private SWEKSource eventSource;

    private SWEKSupplier eventSupplier;

    private List<Vec3> hgsBoundedBox;
    private List<Vec3> hgsBoundCC;
    private Vec3 hgsCentralPoint;
    private Double hgsX;
    private Double hgsY;

    private boolean overmax;

    private List<SWEKRelatedEvents> eventRelationRules;

    public HEKParser() {
        parserStopped = false;
        overmax = false;
    }

    @Override
    public void stopParser() {
        parserStopped = true;
    }

    @Override
    public SWEKEventStream parseEventStream(InputStream downloadInputStream, SWEKEventType eventType, SWEKSource eventSource, SWEKSupplier eventSupplier, List<SWEKRelatedEvents> relationEventRules, boolean todb) {
        HEKEventStream eventStream = new HEKEventStream();

        this.eventType = eventType;
        this.eventSource = eventSource;
        this.eventSupplier = eventSupplier;
        eventRelationRules = relationEventRules;
        try {
            StringBuilder sb = new StringBuilder();
            if (downloadInputStream != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(downloadInputStream, "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject eventJSON;
                String reply = sb.toString().trim().replaceAll("[\n\r\t]", "");
                eventJSON = new JSONObject(reply);
                parseOvermax(eventJSON);
                eventStream.setExtraDownloadNeeded(overmax);
                parseAssociation(eventJSON, eventStream, todb);
                parseEventJSON(eventJSON, eventStream, todb);
                return eventStream;
            } else {
                Log.error("Download input stream was null. Probably the hek is down.");
            }
        } catch (IOException e) {
            Log.error("Could not read the inputstream. " + e);
            e.printStackTrace();
        } catch (JSONException e) {
            Log.error("Could not create the JSON object from stream. " + e);
            e.printStackTrace();
        }
        return eventStream;
    }

    private void parseOvermax(JSONObject eventJSON) throws JSONException {
        overmax = eventJSON.getBoolean("overmax");
    }

    private void parseAssociation(JSONObject eventJSON, HEKEventStream eventStream, boolean todb) throws JSONException {
        JSONArray associations = eventJSON.getJSONArray("association");

        for (int i = 0; i < associations.length() && !parserStopped; i++) {
            Integer[] idlist = JHVDatabase.dump_association2db(parseFirstIvorn(associations.getJSONObject(i)), parseSecondIvorn(associations.getJSONObject(i)));
            JHVAssociation association = new JHVAssociation(idlist[0], idlist[1]);
            eventStream.addJHVAssociation(association);
        }
    }

    private String parseFirstIvorn(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("first_ivorn");
    }

    private String parseSecondIvorn(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("second_ivorn");
    }

    private void parseEventJSON(JSONObject eventJSON, HEKEventStream eventStream, boolean todb) throws JSONException {
        JSONArray results = eventJSON.getJSONArray("result");
        JHVEventType hekEventType = JHVEventType.getJHVEventType(eventType, eventSupplier);

        for (int i = 0; i < results.length() && !parserStopped; i++) {
            HEKEvent currentEvent = new HEKEvent(eventType.getEventName(), eventType.getEventName(), hekEventType);
            JSONObject result = results.getJSONObject(i);

            String uid = parseResult(result, currentEvent);
            handleCoordinates(currentEvent);

            if (currentEvent.getEndDate().getTime() - currentEvent.getStartDate().getTime() > 3 * 24 * 60 * 60 * 1000) {
                Log.error("Possible wrong parsing of a HEK event.");
                Log.error("Event start: " + currentEvent.getStartDate());
                Log.error("Event end: " + currentEvent.getEndDate());
                Log.error("Event JSON: ");
                Log.error(result.toString());
            }

            Integer id;
            if (todb) {
                id = JHVDatabase.dump_event2db(result.toString(), currentEvent, uid);
            } else {
                id = JHVDatabase.getEventId(uid);
            }
            currentEvent.setUniqueID(id);

            eventStream.addJHVEvent(currentEvent);
            reinitializeCoordinates();
        }
    }

    private String parseResult(JSONObject result, HEKEvent currentEvent) throws JSONException {
        Iterator<?> keys = result.keys();
        String uid = null;

        while (keys.hasNext()) {
            String ret = parseParameter(result, keys.next(), currentEvent);
            if (ret != null) {
                uid = ret;
            }
        }
        return uid;
    }

    private String parseParameter(JSONObject result, Object key, HEKEvent currentEvent) throws JSONException {
        String uid = null;
        if (key instanceof String) {
            String originalKeyString = (String) key;
            String keyString = originalKeyString.toLowerCase();
            if (keyString.equals("refs")) {
                parseRefs(currentEvent, result.getJSONArray((String) key));
            } else {
                String value = null;
                if (!result.isNull(keyString))
                    value = result.optString(keyString); // convert to string
                else
                    return uid;

                if (keyString.equals("event_starttime")) {
                    currentEvent.setStartTime(parseDate(value));
                } else if (keyString.equals("event_endtime")) {
                    currentEvent.setEndTime(parseDate(value));
                } else if (keyString.equals("kb_archivid")) {
                    uid = value;
                } else if (keyString.equals("hgs_bbox")) {
                    hgsBoundedBox = parsePolygon(value);
                } else if (keyString.equals("hgs_boundcc")) {
                    hgsBoundCC = parsePolygon(value);
                } else if (keyString.equals("hgs_coord")) {
                    hgsCentralPoint = parsePoint(value);
                } else if (keyString.equals("hgs_x")) {
                    hgsX = Double.valueOf(value);
                } else if (keyString.equals("hgs_y")) {
                    hgsY = Double.valueOf(value);
                } else {
                    boolean visible = false;
                    boolean configured = false;
                    String displayName;
                    SWEKParameter p = eventType.getParameter(originalKeyString);
                    if (p == null) {
                        p = eventSource.getParameter(originalKeyString);
                    }
                    if (p != null) {
                        configured = true;
                        visible = p.isDefaultVisible();
                        displayName = p.getParameterDisplayName();
                    }
                    else {
                        displayName = originalKeyString.replaceAll("_", " ").trim();
                    }
                    JHVEventParameter parameter = new JHVEventParameter(originalKeyString, displayName, value);
                    if (visible)// && configured)
                        currentEvent.addParameter(parameter, visible, configured);
                }
            }
        }
        return uid;
    }

    private void parseRefs(HEKEvent currentEvent, JSONArray refs) throws JSONException {
        for (int i = 0; i < refs.length() && !parserStopped; i++) {
            parseRef(currentEvent, refs.getJSONObject(i));
        }
    }

    private void parseRef(HEKEvent currentEvent, JSONObject ref) throws JSONException {
        Iterator<?> keys = ref.keys();
        String url = "", type = "";
        boolean ok = false;
        while (keys.hasNext()) {
            Object key = keys.next();
            if (key instanceof String) {
                String keyString = (String) key;
                String value = ref.getString(keyString);
                if (keyString.toLowerCase().equals("ref_type")) {
                    if (value.toLowerCase().equals("movie")) {
                        type = "Reference Movie";
                        ok = true;
                    } else if (value.toLowerCase().equals("image")) {
                        type = "Reference Image";
                        ok = true;
                    } else if (value.toLowerCase().equals("html")) {
                        type = "Reference Link";
                        ok = true;
                    }
                } else if (keyString.toLowerCase().equals("ref_url")) {
                    url = value;
                }
            }
        }
        if (ok) {
            currentEvent.addParameter(new JHVEventParameter(type, type, url), true, true);
        }
    }

    /**
     * Parses a date represented in the format yyyy-MM-dd'T'HH:mm:ss to a date.
     *
     * @param date
     *            the date to parse
     * @return the parsed date
     */
    private Date parseDate(String date) {
        try {
            return TimeUtils.utcDateFormat.parse(date);
        } catch (ParseException e) {
            Log.error("The date " + date + " could not be parsed.");
            return null;
        }
    }

    /**
     * Set all coordinate again to null.
     *
     */
    private void reinitializeCoordinates() {
        hgsBoundedBox = null;
        hgsBoundCC = null;
        hgsCentralPoint = null;
        hgsX = null;
        hgsY = null;
    }

    /**
     * Parse a string of the format
     * "POLYGON((0.745758 77.471192,0.667026 75.963757,...,0.691115 69.443955,0.767379 71.565051,0.745758 77.471192))"
     *
     * @param value
     *            the value to parse
     * @return a list of JHV points
     */
    private List<Vec3> parsePolygon(String value) {
        List<Vec3> polygonPoints = new ArrayList<Vec3>();
        if (value.toLowerCase().contains("polygon")) {
            String coordinatesString = value.substring(value.indexOf('(') + 1, value.lastIndexOf(')'));
            String coordinates = coordinatesString.substring(coordinatesString.indexOf('(') + 1, coordinatesString.lastIndexOf(')'));

            Scanner s = new Scanner(coordinates);
            s.useDelimiter(",");
            while (s.hasNext()) {
                String coordinateString = s.next();
                Vec3 tempPoint = parseCoordinates(coordinateString);
                if (tempPoint != null) {
                    polygonPoints.add(tempPoint);
                }
            }
            s.close();
        }
        return polygonPoints;
    }

    /**
     * Parses a point of the format POINT(0.716676950817756 73.6104596659652).
     *
     * @param value
     *            the point to parse
     * @return The GL3DVec3 or null if it could not be parsed.
     */
    private Vec3 parsePoint(String value) {
        if (value.toLowerCase().contains("point")) {
            String coordinates = value.substring(value.indexOf('(') + 1, value.indexOf(')'));
            return parseCoordinates(coordinates);
        }
        return null;
    }

    /**
     * Parses a string of the format "0.716676950817756 73.6104596659652" to a
     * GL3DVec3
     *
     * @param coordinateString
     *            the string to parse
     * @return the GL3DVec3 or null of it could not be parsed
     */
    private Vec3 parseCoordinates(String coordinateString) {
        double[] coordinate = new double[] { 0., 0., 0. };
        boolean notnull = false;

        Scanner coordinatesScanner = new Scanner(coordinateString);
        coordinatesScanner.useDelimiter(" ");

        for (int i = 0; i < 3; i++) {
            if (coordinatesScanner.hasNext()) {
                coordinate[i] = Double.valueOf(coordinatesScanner.next());
                notnull = true;
            }
        }

        coordinatesScanner.close();

        if (notnull) {
            return new Vec3(coordinate[0], coordinate[1], coordinate[2]);
        }
        return null;
    }

    /**
     * Handle the parsed information for positions
     *
     * @param currentEvent
     *            the current event being parsed
     */
    private void handleCoordinates(HEKEvent currentEvent) {
        checkAndFixBoundingBox();
        boolean found = false;
        found = handleHGSCoordinates(currentEvent);
        //if (!found)
        //    found = handleHGCCoordinates(currentEvent);
        //if (!found)
        //    found = handleHRCCoordinates(currentEvent);
        //if (!found)
        //    found = handleHPCCoordiantes(currentEvent);
    }

    private void checkAndFixBoundingBox() {
        if (hgsBoundedBox != null) {
            double minX = 0.0;
            double minY = 0.0;
            double maxX = 0.0;
            double maxY = 0.0;
            boolean first = true;
            for (Vec3 p : hgsBoundedBox) {
                if (first) {
                    minX = p.x;
                    maxX = p.x;
                    minY = p.y;
                    maxY = p.y;
                    first = false;
                } else {
                    minX = Math.min(minX, p.x);
                    maxX = Math.max(maxX, p.x);
                    minY = Math.min(minY, p.y);
                    maxY = Math.max(maxY, p.y);
                }
            }
            if ((maxX - minX) > 160 && (maxY - minY) > 160) {
                hgsBoundedBox = null;
            }
        }
    }

    /**
     * Handles the HGS coordinates. Checks if a coordinate of that format was
     * found and if it is the case it is added to the JHVPositionInformation
     * list of the current event.
     *
     * @param currentEvent
     *            the current event being parsed.
     */
    private boolean handleHGSCoordinates(HEKEvent currentEvent) {
        if (hgsBoundedBox != null || hgsCentralPoint != null || (hgsX != null && hgsY != null) || hgsBoundCC != null) {
            List<Vec3> localHGSBoundedBox;
            List<Vec3> localHGSBoundCC;
            Vec3 localHGSCentralPoint = null;

            if (hgsBoundedBox != null) {
                localHGSBoundedBox = hgsBoundedBox;
            } else {
                localHGSBoundedBox = new ArrayList<Vec3>();
            }

            if (hgsBoundCC != null) {
                localHGSBoundCC = hgsBoundCC;
            } else {
                localHGSBoundCC = new ArrayList<Vec3>();
            }

            if (hgsCentralPoint != null) {
                localHGSCentralPoint = hgsCentralPoint;
            } else {
                if (hgsX != null && hgsY != null) {
                    localHGSCentralPoint = new Vec3(hgsX, hgsY, 0);
                }
            }

            ArrayList<Vec3> jhvBoundedBox = new ArrayList<Vec3>(localHGSBoundedBox.size());
            for (Vec3 el : localHGSBoundedBox) {
                jhvBoundedBox.add(convertHGSJHV(el, currentEvent));
            }

            ArrayList<Vec3> jhvBoundCC = new ArrayList<Vec3>(localHGSBoundCC.size());
            for (Vec3 el : localHGSBoundCC) {
                jhvBoundCC.add(convertHGSJHV(el, currentEvent));
            }

            Vec3 jhvCentralPoint = null;
            if (localHGSCentralPoint != null) {
                jhvCentralPoint = convertHGSJHV(localHGSCentralPoint, currentEvent);
            }

            currentEvent.addJHVPositionInformation(new HEKPositionInformation(jhvBoundedBox, jhvBoundCC, jhvCentralPoint));
            return true;
        }
        return false;
    }

    private static Vec3 convertHGSJHV(Vec3 el, HEKEvent evt) {
        Position.L p = Sun.getEarth(new JHVDate((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2));
        double theta = Math.PI / 180 * el.y;
        double phi = Math.PI / 180 * el.x - p.lon;

        double x = Math.cos(theta) * Math.sin(phi);
        double z = Math.cos(theta) * Math.cos(phi);
        double y = -Math.sin(theta);
        return new Vec3(x, y, z);
    }

    /*
    private static HashMap<SWEKEventType, List<JHVEventRelationShipRule>> hmr = new HashMap<SWEKEventType, List<JHVEventRelationShipRule>>();

    private List<JHVEventRelationShipRule> getEventRelationShipRules() {
        if (!hmr.containsKey(this.eventType)) {
            List<JHVEventRelationShipRule> rules = new ArrayList<JHVEventRelationShipRule>();
            for (SWEKRelatedEvents er : eventRelationRules) {
                if (er.getEvent().equals(eventType)) {
                    if (!er.getRelatedOnList().isEmpty()) {
                        List<JHVRelatedOn> relatedOnList = new ArrayList<JHVRelatedOn>();
                        for (SWEKRelatedOn ro : er.getRelatedOnList()) {
                            if (ro.getParameterFrom() != null && ro.getParameterWith() != null) {
                                JHVEventParameter relatedOnFrom = new JHVEventParameter(ro.getParameterFrom().getParameterName(), ro.getParameterFrom().getParameterDisplayName(), "");
                                JHVEventParameter relatedOnWith = new JHVEventParameter(ro.getParameterWith().getParameterName(), ro.getParameterWith().getParameterDisplayName(), "");
                                JHVRelatedOn jhvRelatedOn = new JHVRelatedOn(relatedOnFrom, relatedOnWith);
                                relatedOnList.add(jhvRelatedOn);
                            }
                        }
                        JHVEventType relatedWith = JHVEventType.getJHVEventType(er.getRelatedWith(), eventSupplier);
                        JHVEventRelationShipRule rule = new JHVEventRelationShipRule(relatedWith, relatedOnList);
                        rules.add(rule);
                    }
                }
                if (er.getRelatedWith().equals(eventType)) {
                    List<JHVRelatedOn> relatedOnList = new ArrayList<JHVRelatedOn>();
                    for (SWEKRelatedOn ro : er.getRelatedOnList()) {
                        if (ro.getParameterFrom() != null && ro.getParameterWith() != null) {
                            JHVEventParameter relatedOnFrom = new JHVEventParameter(ro.getParameterWith().getParameterName(), ro.getParameterWith().getParameterDisplayName(), "");
                            JHVEventParameter relatedOnWith = new JHVEventParameter(ro.getParameterFrom().getParameterName(), ro.getParameterFrom().getParameterDisplayName(), "");
                            JHVRelatedOn jhvRelatedOn = new JHVRelatedOn(relatedOnFrom, relatedOnWith);
                            relatedOnList.add(jhvRelatedOn);
                        }
                    }
                    JHVEventType relatedWith = JHVEventType.getJHVEventType(er.getEvent(), eventSupplier);
                    JHVEventRelationShipRule rule = new JHVEventRelationShipRule(relatedWith, relatedOnList);
                    rules.add(rule);
                }
                hmr.put(this.eventType, rules);
            }
        }

        return hmr.get(this.eventType);
    }
    */

}
