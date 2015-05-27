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
import java.util.Map;
import java.util.Scanner;

import org.helioviewer.base.astronomy.Position;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.datetime.TimeUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelation;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelationShipRule;
import org.helioviewer.jhv.data.datatype.event.JHVPoint;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedOn;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.config.SWEKRelatedEvents;
import org.helioviewer.jhv.plugins.swek.config.SWEKRelatedOn;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;
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

    /** Is the parser stopped */
    private boolean parserStopped;

    /** The hek event stream */
    private final HEKEventStream eventStream;

    /** The event type for this parser */
    private SWEKEventType eventType;

    /** the event source for this parser */
    private SWEKSource eventSource;

    private SWEKSupplier eventSupplier;

    /** Standard coordinate system parameters */
    private String coordinateSystemString;
    private Double coordinate1;
    private Double coordinate2;
    private Double coordinate3;

    /** HGC coordinates */
    private List<JHVPoint> hgcBoundedBox;
    private List<JHVPoint> hgcBoundCC;
    private JHVPoint hgcCentralPoint;
    private Double hgcX;
    private Double hgcY;

    /** HGS coordinates */
    private List<JHVPoint> hgsBoundedBox;
    private List<JHVPoint> hgsBoundCC;
    private JHVPoint hgsCentralPoint;
    private Double hgsX;
    private Double hgsY;

    /** HPC coordinates */
    private List<JHVPoint> hpcBoundedBox;
    private List<JHVPoint> hpcBoundCC;
    private JHVPoint hpcCentralPoint;
    private Double hpcX;
    private Double hpcY;

    /** HRC coordinates */
    private List<JHVPoint> hrcBoundedBox;
    private List<JHVPoint> hrcBoundCC;
    private JHVPoint hrcCentralPoint;
    private Double hrcA;
    private Double hrcR;

    private final Map<String, List<Association>> associationsMap;
    private final Map<String, HEKEvent> associationEventsMap;

    private boolean overmax;

    private List<SWEKRelatedEvents> eventRelationRules;

    /**
     * Creates a parser for the given event type and event source.
     *
     * @param eventType
     *            the type of the event
     * @param source
     *            the source of the event
     *
     */
    public HEKParser() {
        parserStopped = false;
        eventStream = new HEKEventStream();
        associationsMap = new HashMap<String, List<Association>>();
        associationEventsMap = new HashMap<String, HEKEvent>();
        overmax = false;

    }

    @Override
    public void stopParser() {
        parserStopped = true;
    }

    @Override
    public SWEKEventStream parseEventStream(InputStream downloadInputStream, SWEKEventType eventType, SWEKSource eventSource, SWEKSupplier eventSupplier, List<SWEKRelatedEvents> relationEventRules) {
        this.eventType = eventType;
        this.eventSource = eventSource;
        this.eventSupplier = eventSupplier;
        eventRelationRules = relationEventRules;
        try {
            StringBuilder sb = new StringBuilder();
            if (downloadInputStream != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(downloadInputStream));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject eventJSON;
                String reply = sb.toString().trim().replaceAll("[\n\r\t]", "");
                // Log.debug("reply:");
                // Log.debug(reply.toString());
                eventJSON = new JSONObject(reply);
                parseOvermax(eventJSON);
                eventStream.setExtraDownloadNeeded(overmax);
                parseAssociation(eventJSON);
                parseEventJSON(eventJSON);
                return eventStream;
            } else {
                // TODO inform the user hek is probably death...
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

    /**
     *
     * @param eventJSON
     * @throws JSONException
     */
    private void parseAssociation(JSONObject eventJSON) throws JSONException {
        JSONArray associations = eventJSON.getJSONArray("association");
        // AssociationsPrinter.printNodeColor(c);
        // AssociationsPrinter.printEdgeColor(c);
        for (int i = 0; i < associations.length() && !parserStopped; i++) {
            Association currentAssociation = new Association();
            currentAssociation.setAssociationType(parseAssociationType(associations.getJSONObject(i)));
            currentAssociation.setAssociationIvorn1(parseFirstIvorn(associations.getJSONObject(i)));
            currentAssociation.setAssociationIvorn2(parseSecondIvorn(associations.getJSONObject(i)));
            // AssociationsPrinter.printAssociation(currentAssociation.associationIvorn1,
            // currentAssociation.associationIvorn2);
            // Log.debug("************************************************************************************************");
            // Log.debug("ivorn1 : " +
            // currentAssociation.getAssociationIvorn1());
            // Log.debug("ivorn2 : " +
            // currentAssociation.getAssociationIvorn2());
            // Log.debug("************************************************************************************************");
            List<Association> associationsIvorn1 = new ArrayList<Association>();
            List<Association> associationsIvorn2 = new ArrayList<Association>();
            if (associationsMap.containsKey(currentAssociation.getAssociationIvorn1())) {
                associationsIvorn1 = associationsMap.get(currentAssociation.getAssociationIvorn1());
            }
            if (associationsMap.containsKey(currentAssociation.getAssociationIvorn2())) {
                associationsIvorn2 = associationsMap.get(currentAssociation.getAssociationIvorn2());
            }
            associationsIvorn1.add(currentAssociation);
            associationsIvorn2.add(currentAssociation);
            associationsMap.put(currentAssociation.getAssociationIvorn1(), associationsIvorn1);
            associationsMap.put(currentAssociation.getAssociationIvorn2(), associationsIvorn2);
        }
    }

    /**
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    private String parseAssociationType(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("edge_type");
    }

    /**
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    private String parseFirstIvorn(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("first_ivorn");
    }

    /**
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    private String parseSecondIvorn(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("second_ivorn");
    }

    /**
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    private String parseEventType1(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("first_event_type");
    }

    /**
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    private String parseEventType2(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("second_event_type");
    }

    /**
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    private String parseId1(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("first_id");
    }

    /**
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    private String parseId2(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("second_id");
    }

    /**
     * Parses the event JSON returned by the server.
     *
     * @param eventJSON
     *            the JSON object
     * @throws JSONException
     *             if the json object could not be parsed
     */
    private void parseEventJSON(JSONObject eventJSON) throws JSONException {
        JSONArray results = eventJSON.getJSONArray("result");
        HEKEventType hekEventType = new HEKEventType(eventType.getEventName(), eventSource.getSourceName(), eventSupplier.getSupplierName());
        for (int i = 0; i < results.length() && !parserStopped; i++) {
            HEKEvent currentEvent = new HEKEvent(eventType.getEventName(), eventType.getEventName(), "", hekEventType, eventType.getEventIcon(), eventType.getColor());
            JSONObject result = results.getJSONObject(i);
            parseResult(result, currentEvent);
            handleCoordinates(currentEvent);
            if (associationsMap.containsKey(currentEvent.getUniqueID())) {
                // There is an association with the current event
                associationEventsMap.put(currentEvent.getUniqueID(), currentEvent);
                List<Association> associationsList = associationsMap.get(currentEvent.getUniqueID());
                for (Association association : associationsList) {
                    if (association.getAssociationIvorn1().equals(currentEvent.getUniqueID())) {
                        // current event is the first element of the association
                        if (associationEventsMap.containsKey(association.getAssociationIvorn2())) {
                            // The other event of the association is available
                            HEKEvent associatedEvent = associationEventsMap.get(association.getAssociationIvorn2());
                            if (association.getAssociationType().toLowerCase().equals("is_followed_by") || association.getAssociationType().toLowerCase().equals("splits_into") || association.getAssociationType().toLowerCase().equals("merges_into")) {
                                // Is a sequence relation so associated event is
                                // follow-up of current event
                                if (currentEvent.getEventRelationShip().getPrecedingEvents().isEmpty()) {
                                    currentEvent.getEventRelationShip().setRelationshipColor(HEKColors.getNextColor());
                                }
                                associatedEvent.getEventRelationShip().getPrecedingEvents().put(currentEvent.getUniqueID(), new JHVEventRelation(currentEvent.getUniqueID(), currentEvent));
                                currentEvent.getEventRelationShip().getNextEvents().put(associatedEvent.getUniqueID(), new JHVEventRelation(associatedEvent.getUniqueID(), associatedEvent));
                                associatedEvent.getEventRelationShip().setRelationshipColor(currentEvent.getEventRelationShip().getRelationshipColor());

                            } else {
                                // is not a sequence relationship just add the
                                // relation to the related events by rule
                                associatedEvent.getEventRelationShip().getRelatedEventsByRule().put(currentEvent.getUniqueID(), new JHVEventRelation(currentEvent.getUniqueID(), currentEvent));
                                currentEvent.getEventRelationShip().getRelatedEventsByRule().put(associatedEvent.getUniqueID(), new JHVEventRelation(associatedEvent.getUniqueID(), associatedEvent));
                            }
                        } else {
                            // The associated event is not in the list so we
                            // start a
                            // new color and add an association in the list
                            // without
                            // event reference.
                            currentEvent.getEventRelationShip().setRelationshipColor(HEKColors.getNextColor());
                            if (association.getAssociationType().toLowerCase().equals("is_followed_by") || association.getAssociationType().toLowerCase().equals("splits_into") || association.getAssociationType().toLowerCase().equals("merges_into")) {
                                currentEvent.getEventRelationShip().getNextEvents().put(association.getAssociationIvorn2(), new JHVEventRelation(association.getAssociationIvorn2()));
                            } else {
                                currentEvent.getEventRelationShip().getRelatedEventsByRule().put(association.getAssociationIvorn2(), new JHVEventRelation(association.getAssociationIvorn2()));
                            }
                        }
                    } else if (association.getAssociationIvorn2().equals(currentEvent.getUniqueID())) {
                        // current event is the second event of the relationship
                        if (associationEventsMap.containsKey(association.getAssociationIvorn1())) {
                            // the associated event is available
                            HEKEvent associatedEvent = associationEventsMap.get(association.getAssociationIvorn1());
                            if (association.getAssociationType().toLowerCase().equals("is_followed_by") || association.getAssociationType().toLowerCase().equals("splits_into") || association.getAssociationType().toLowerCase().equals("merges_into")) {
                                // Is a sequence relation so the current event
                                // is
                                // the follow-up of the associated event
                                associatedEvent.getEventRelationShip().getNextEvents().put(currentEvent.getUniqueID(), new JHVEventRelation(currentEvent.getUniqueID(), currentEvent));
                                currentEvent.getEventRelationShip().getPrecedingEvents().put(associatedEvent.getUniqueID(), new JHVEventRelation(associatedEvent.getUniqueID(), associatedEvent));
                                currentEvent.getEventRelationShip().setRelationshipColor(associatedEvent.getEventRelationShip().getRelationshipColor());
                            } else {
                                // it is not a sequence relationship just add
                                // the
                                // relation to the related events by rule.
                                associatedEvent.getEventRelationShip().getRelatedEventsByRule().put(currentEvent.getUniqueID(), new JHVEventRelation(currentEvent.getUniqueID(), currentEvent));
                                currentEvent.getEventRelationShip().getRelatedEventsByRule().put(associatedEvent.getUniqueID(), new JHVEventRelation(associatedEvent.getUniqueID(), associatedEvent));
                            }
                        } else {
                            // The associated event is not in the list so we
                            // start a
                            // new color add we already add a reference to the
                            // previous event without event type.
                            currentEvent.getEventRelationShip().setRelationshipColor(HEKColors.getNextColor());
                            if (association.getAssociationType().toLowerCase().equals("is_followed_by") || association.getAssociationType().toLowerCase().equals("splits_into") || association.getAssociationType().toLowerCase().equals("merges_into")) {
                                currentEvent.getEventRelationShip().getPrecedingEvents().put(association.getAssociationIvorn1(), new JHVEventRelation(association.getAssociationIvorn1()));
                            } else {
                                currentEvent.getEventRelationShip().getRelatedEventsByRule().put(association.getAssociationIvorn1(), new JHVEventRelation(association.getAssociationIvorn1()));
                            }
                        }
                    }
                }
            }
            currentEvent.getEventRelationShip().getRelationshipRules().addAll(getEventRelationShipRules());
            eventStream.addJHVEvent(currentEvent);
            reinitializeCoordinates();
        }
    }

    /**
     * Parses one result returned by the HEK server.
     *
     * @param result
     *            the result to be parsed.
     * @param currentEvent
     *            the current event the is parsed
     * @throws JSONException
     *             if the result could not be parsed
     */
    private void parseResult(JSONObject result, HEKEvent currentEvent) throws JSONException {
        Iterator<?> keys = result.keys();
        while (keys.hasNext()) {
            parseParameter(result, keys.next(), currentEvent);
        }
    }

    /**
     * Parses the parameter
     *
     * @param result
     *            the result from where to parse the parameter
     * @param key
     *            the key in the json
     * @param currentEvent
     *            the event currently parsed
     * @throws JSONException
     *             if the parameter could not be parsed
     */
    private void parseParameter(JSONObject result, Object key, HEKEvent currentEvent) throws JSONException {
        if (key instanceof String) {
            String keyString = (String) key;
            String value = result.getString((String) key);
            if (value.toLowerCase().equals("null")) {
                value = null;
            }
            // Event start time
            if (keyString.toLowerCase().equals("event_starttime")) {
                currentEvent.setStartTime(parseDate(value));
            } else
            // Event end time
            if (keyString.toLowerCase().equals("event_endtime")) {
                currentEvent.setEndTime(parseDate(value));
            } else
            // event unique ID
            if (keyString.toLowerCase().equals("kb_archivid")) {
                currentEvent.setUniqueID(value);
            } else
            // event positions (Standard position)
            if (keyString.toLowerCase().equals("event_coordsys")) {
                coordinateSystemString = value;
            } else if (keyString.toLowerCase().equals("event_coord1")) {
                if (value != null) {
                    coordinate1 = Double.parseDouble(value);
                }
            } else if (keyString.toLowerCase().equals("event_coord2")) {
                if (value != null) {
                    coordinate2 = Double.parseDouble(value);
                }
            } else if (keyString.toLowerCase().equals("event_coord3")) {
                if (value != null) {
                    coordinate3 = Double.parseDouble(value);
                }
            }
            // event positions (Not standard)
            if (keyString.toLowerCase().equals("hgc_bbox")) {
                hgcBoundedBox = parsePolygon(value);
            } else if (keyString.toLowerCase().equals("hgc_boundcc")) {
                hgcBoundCC = parsePolygon(value);
            } else if (keyString.toLowerCase().equals("hgc_coord")) {
                hgcCentralPoint = parsePoint(value);
            } else if (keyString.toLowerCase().equals("hgc_x")) {
                if (value != null) {
                    hgcX = Double.parseDouble(value);
                }
            } else if (keyString.toLowerCase().equals("hgc_y")) {
                if (value != null) {
                    hgcY = Double.parseDouble(value);
                }
            } else if (keyString.toLowerCase().equals("hgs_bbox")) {
                hgsBoundedBox = parsePolygon(value);
            } else if (keyString.toLowerCase().equals("hgs_boundcc")) {
                hgsBoundCC = parsePolygon(value);
            } else if (keyString.toLowerCase().equals("hgs_coord")) {
                hgsCentralPoint = parsePoint(value);
            } else if (keyString.toLowerCase().equals("hgs_x")) {
                if (value != null) {
                    hgsX = Double.parseDouble(value);
                }
            } else if (keyString.toLowerCase().equals("hgs_y")) {
                if (value != null) {
                    hgsY = Double.parseDouble(value);
                }
            } else if (keyString.toLowerCase().equals("hpc_bbox")) {
                hpcBoundedBox = parsePolygon(value);
            } else if (keyString.toLowerCase().equals("hpc_boundcc")) {
                hpcBoundCC = parsePolygon(value);
            } else if (keyString.toLowerCase().equals("hpc_coord")) {
                hpcCentralPoint = parsePoint(value);
            } else if (keyString.toLowerCase().equals("hpc_x")) {
                if (value != null) {
                    hpcX = Double.parseDouble(value);
                }
            } else if (keyString.toLowerCase().equals("hpc_y")) {
                if (value != null) {
                    hpcY = Double.parseDouble(value);
                }
            } else if (keyString.toLowerCase().equals("hrc_bbox")) {
                hrcBoundedBox = parsePolygon(value);
            } else if (keyString.toLowerCase().equals("hrc_boundcc")) {
                hrcBoundCC = parsePolygon(value);
            } else if (keyString.toLowerCase().equals("hrc_coord")) {
                hrcCentralPoint = parsePoint(value);
            } else if (keyString.toLowerCase().equals("hrc_a")) {
                if (value != null) {
                    hrcA = Double.parseDouble(value);
                }
            } else if (keyString.toLowerCase().equals("hrc_r")) {
                if (value != null) {
                    hrcR = Double.parseDouble(value);
                }
            } else {
                boolean visible = false;
                boolean configured = false;
                JHVEventParameter parameter = new JHVEventParameter(keyString, keyString, value);
                if (!eventType.containsParameter(keyString)) {
                    if (eventSource.containsParameter(keyString)) {
                        configured = true;
                        SWEKParameter p = eventSource.getParameter(keyString);
                        if (p != null) {
                            visible = p.isDefaultVisible();
                        }
                    }
                } else {
                    configured = true;
                    SWEKParameter p = eventType.getParameter(keyString);
                    if (p != null) {
                        visible = p.isDefaultVisible();
                    }
                }
                currentEvent.addParameter(parameter, visible, configured);
            }
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
        coordinateSystemString = null;
        coordinate1 = null;
        coordinate2 = null;
        coordinate3 = null;

        hgcBoundedBox = null;
        hgcBoundCC = null;
        hgcCentralPoint = null;
        hgcX = null;
        hgcY = null;

        hgsBoundedBox = null;
        hgsBoundCC = null;
        hgsCentralPoint = null;
        hgsX = null;
        hgsY = null;

        hrcBoundedBox = null;
        hrcBoundCC = null;
        hrcCentralPoint = null;
        hrcA = null;
        hrcR = null;

        hpcBoundedBox = null;
        hpcBoundCC = null;
        hpcCentralPoint = null;
        hpcX = null;
        hpcY = null;
    }

    /**
     * Parse a string of the format
     * "POLYGON((0.745758 77.471192,0.667026 75.963757,...,0.691115 69.443955,0.767379 71.565051,0.745758 77.471192))"
     *
     * @param value
     *            the value to parse
     * @return a list of JHV points
     */
    private List<JHVPoint> parsePolygon(String value) {
        List<JHVPoint> polygonPoints = new ArrayList<JHVPoint>();
        if (value.toLowerCase().contains("polygon")) {
            String coordinatesString = value.substring(value.indexOf('(') + 1, value.lastIndexOf(')'));
            String coordinates = coordinatesString.substring(coordinatesString.indexOf('(') + 1, coordinatesString.lastIndexOf(')'));

            Scanner s = new Scanner(coordinates);
            s.useDelimiter(",");
            while (s.hasNext()) {
                String coordinateString = s.next();
                JHVPoint tempPoint = parseCoordinates(coordinateString);
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
     * @return The JHVpoint or null if it could not be parsed.
     */
    private JHVPoint parsePoint(String value) {
        if (value.toLowerCase().contains("point")) {
            String coordinates = value.substring(value.indexOf('(') + 1, value.indexOf(')'));
            return parseCoordinates(coordinates);
        }
        return null;
    }

    /**
     * Parses a string of the format "0.716676950817756 73.6104596659652" to a
     * JHVPoint
     *
     * @param coordinateString
     *            the string to parse
     * @return the JHVPoint or null of it could not be parsed
     */
    private JHVPoint parseCoordinates(String coordinateString) {
        Double coordinate1 = 0.0;
        Double coordinate2 = 0.0;
        Double coordinate3 = 0.0;
        boolean coordinate1OK = false;
        boolean coordinate2OK = false;
        boolean coordinate3OK = false;

        Scanner coordinatesScanner = new Scanner(coordinateString);
        coordinatesScanner.useDelimiter(" ");

        if (coordinatesScanner.hasNext()) {
            coordinate1 = coordinatesScanner.nextDouble();
            coordinate1OK = true;
        }
        if (coordinatesScanner.hasNext()) {
            coordinate2 = coordinatesScanner.nextDouble();
            coordinate2OK = true;
        }
        if (coordinatesScanner.hasNext()) {
            coordinate3 = coordinatesScanner.nextDouble();
            coordinate3OK = true;
        }

        coordinatesScanner.close();

        if (coordinate1OK && coordinate2OK && coordinate3OK) {
            return new JHVPoint(coordinate1, coordinate2, coordinate3);
        } else if (coordinate1OK && coordinate2OK) {
            return new JHVPoint(coordinate1, coordinate2, null);
        } else if (coordinate1OK) {
            return new JHVPoint(coordinate1, null, null);
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
        handleStandardPosition(currentEvent);
        handleHGSCoordinates(currentEvent);
        handleHGCCoordinates(currentEvent);
        handleHRCCoordinates(currentEvent);
        handleHPCCoordiantes(currentEvent);
    }

    private void checkAndFixBoundingBox() {
        if (hgsBoundedBox != null) {
            double minX = 0.0;
            double minY = 0.0;
            double maxX = 0.0;
            double maxY = 0.0;
            boolean first = true;
            for (JHVPoint p : hgsBoundedBox) {
                if (first) {
                    minX = p.getCoordinate1();
                    maxX = p.getCoordinate1();
                    minY = p.getCoordinate2();
                    maxY = p.getCoordinate2();
                    first = false;
                } else {
                    minX = Math.min(minX, p.getCoordinate1());
                    maxX = Math.max(maxX, p.getCoordinate1());
                    minY = Math.min(minY, p.getCoordinate2());
                    maxY = Math.max(maxY, p.getCoordinate2());
                }
            }
            if ((maxX - minX) > 160 && (maxY - minY) > 160) {
                hgsBoundedBox = null;
                hgcBoundedBox = null;
                hpcBoundedBox = null;
                hrcBoundedBox = null;
            }
        }
    }

    /**
     * Handles the standard event position.
     *
     * @param currentEvent
     *            the current event being parsed
     */
    private void handleStandardPosition(HEKEvent currentEvent) {
        if (coordinateSystemString != null && coordinate1 != null) {
            JHVCoordinateSystem coorSys = parseCoordinateSystemString();
            if (coorSys != null) {
                JHVPoint centralPoint = new JHVPoint(coordinate1, coordinate2, coordinate3);
                currentEvent.addJHVPositionInformation(coorSys, new HEKPositionInformation(coorSys, new ArrayList<JHVPoint>(), new ArrayList<JHVPoint>(), centralPoint));
            }
        }
    }

    /**
     * Extract the used coordinate system from the coordinate system string.
     *
     * @return the correct JHVCoordinateSystem or null if the coordinate system
     *         could not be parsed.
     */
    private JHVCoordinateSystem parseCoordinateSystemString() {
        if (coordinateSystemString.toLowerCase().contains("hgc")) {
            return JHVCoordinateSystem.HGC;
        } else if (coordinateSystemString.toLowerCase().contains("hgs")) {
            return JHVCoordinateSystem.HGS;
        } else if (coordinateSystemString.toLowerCase().contains("hpc")) {
            return JHVCoordinateSystem.HPC;
        } else if (coordinateSystemString.toLowerCase().contains("hrc")) {
            return JHVCoordinateSystem.HRC;
        } else {
            return null;
        }
    }

    /**
     * Handles the HGC coordinates. Checks if a coordinate of that format was
     * found and if it is the case it is added to the JHVPositionInformation
     * list of the current event.
     *
     * @param currentEvent
     *            the current event being parsed.
     */
    private void handleHGCCoordinates(HEKEvent currentEvent) {
        if (hgcBoundedBox != null || hgcCentralPoint != null || (hgcX != null && hgcY != null) || hgcBoundCC != null) {
            List<JHVPoint> localHGCBoundedBox = new ArrayList<JHVPoint>();
            JHVPoint localHGCCentralPoint = null;
            List<JHVPoint> localHGCBoundCC = new ArrayList<JHVPoint>();
            if (hgcBoundedBox != null) {
                localHGCBoundedBox = hgcBoundedBox;
            }
            if (hgcBoundCC != null) {
                localHGCBoundCC = hgcBoundCC;
            }
            if (hgcCentralPoint != null) {
                localHGCCentralPoint = hgcCentralPoint;
            } else {
                if (hgcX != null && hgcY != null) {
                    localHGCCentralPoint = new JHVPoint(hgcX, hgcY, null);
                }
            }
            currentEvent.addJHVPositionInformation(JHVCoordinateSystem.HGC, new HEKPositionInformation(JHVCoordinateSystem.HGC, localHGCBoundedBox, localHGCBoundCC, localHGCCentralPoint));
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
    private void handleHGSCoordinates(HEKEvent currentEvent) {
        if (hgsBoundedBox != null || hgsCentralPoint != null || (hgsX != null && hgsY != null) || hgsBoundCC != null) {
            List<JHVPoint> localHGSBoundedBox = new ArrayList<JHVPoint>();
            List<JHVPoint> localHGSBoundCC = new ArrayList<JHVPoint>();
            JHVPoint localHGSCentralPoint = null;
            if (hgsBoundedBox != null) {
                localHGSBoundedBox = hgsBoundedBox;
            }
            if (hgsBoundCC != null) {
                localHGSBoundCC = hgsBoundCC;
            }
            if (hgsCentralPoint != null) {
                localHGSCentralPoint = hgsCentralPoint;
            } else {
                if (hgsX != null && hgsY != null) {
                    localHGSCentralPoint = new JHVPoint(hgsX, hgsY, null);
                }
            }
            currentEvent.addJHVPositionInformation(JHVCoordinateSystem.HGS, new HEKPositionInformation(JHVCoordinateSystem.HGS, localHGSBoundedBox, localHGSBoundCC, localHGSCentralPoint));
            ArrayList<JHVPoint> jhvBoundedBox = new ArrayList<JHVPoint>();
            for (JHVPoint el : localHGSBoundedBox) {
                jhvBoundedBox.add(convertHGSJHV(el, currentEvent));
            }
            ArrayList<JHVPoint> jhvBoundCC = new ArrayList<JHVPoint>();
            for (JHVPoint el : localHGSBoundCC) {
                jhvBoundCC.add(convertHGSJHV(el, currentEvent));
            }
            JHVPoint jhvCentralPoint = null;
            if (localHGSCentralPoint != null) {
                jhvCentralPoint = convertHGSJHV(localHGSCentralPoint, currentEvent);
            }
            currentEvent.addJHVPositionInformation(JHVCoordinateSystem.JHV, new HEKPositionInformation(JHVCoordinateSystem.JHV, jhvBoundedBox, jhvBoundCC, jhvCentralPoint));

            ArrayList<JHVPoint> jhvBoundedBox2D = new ArrayList<JHVPoint>();

            for (JHVPoint el : localHGSBoundedBox) {
                jhvBoundedBox2D.add(convertHGSJHV2D(el, currentEvent));
            }
            ArrayList<JHVPoint> jhvBoundCC2D = new ArrayList<JHVPoint>();
            for (JHVPoint el : localHGSBoundCC) {
                jhvBoundCC2D.add(convertHGSJHV2D(el, currentEvent));
            }
            JHVPoint jhvCentralPoint2D = null;
            if (localHGSCentralPoint != null) {
                jhvCentralPoint2D = convertHGSJHV2D(localHGSCentralPoint, currentEvent);
            }
            currentEvent.addJHVPositionInformation(JHVCoordinateSystem.JHV2D, new HEKPositionInformation(JHVCoordinateSystem.JHV2D, jhvBoundedBox2D, jhvBoundCC2D, jhvCentralPoint2D));

        }
    }

    public JHVPoint convertHGSJHV2D(JHVPoint el, HEKEvent evt) {
        double theta = el.getCoordinate2() / MathUtils.radeg;
        double phi = el.getCoordinate1() / MathUtils.radeg;
        double x = Math.cos(theta) * Math.sin(phi);
        double z = Math.cos(theta) * Math.cos(phi);
        double y = -Math.sin(theta);
        return new JHVPoint(x, y, z);
    }

    public JHVPoint convertHGSJHV(JHVPoint el, HEKEvent evt) {
        double theta = el.getCoordinate2() / MathUtils.radeg;

        Date date = new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2);
        Position.Latitudinal p = Sun.getRBL(date);
        double phi = el.getCoordinate1() / MathUtils.radeg - p.lon;

        double x = Math.cos(theta) * Math.sin(phi);
        double z = Math.cos(theta) * Math.cos(phi);
        double y = -Math.sin(theta);
        return new JHVPoint(x, y, z);
    }

    /**
     * Handles the HRC coordinates. Checks if a coordinate of that format was
     * found and if it is the case it is added to the JHVPositionInformation
     * list of the current event.
     *
     * @param currentEvent
     *            the current event being parsed.
     */
    private void handleHRCCoordinates(HEKEvent currentEvent) {
        if (hrcBoundedBox != null || hrcCentralPoint != null || (hrcA != null && hrcR != null) || hrcBoundCC != null) {
            List<JHVPoint> localHRCBoundedBox = new ArrayList<JHVPoint>();
            List<JHVPoint> localHRCBoundCC = new ArrayList<JHVPoint>();
            JHVPoint localHRCCentralPoint = null;
            if (hrcBoundedBox != null) {
                localHRCBoundedBox = hrcBoundedBox;
            }
            if (hrcBoundCC != null) {
                localHRCBoundCC = hrcBoundCC;
            }
            if (hrcCentralPoint != null) {
                localHRCCentralPoint = hrcCentralPoint;
            } else {
                if (hrcA != null && hrcR != null) {
                    localHRCCentralPoint = new JHVPoint(hrcA, hrcR, null);
                }
            }
            currentEvent.addJHVPositionInformation(JHVCoordinateSystem.HRC, new HEKPositionInformation(JHVCoordinateSystem.HRC, localHRCBoundedBox, localHRCBoundCC, localHRCCentralPoint));
        }

    }

    /**
     * Handles the HPC coordinates. Checks if a coordinate of that format was
     * found and if it is the case it is added to the JHVPositionInformation
     * list of the current event.
     *
     * @param currentEvent
     *            the current event being parsed.
     */
    private void handleHPCCoordiantes(HEKEvent currentEvent) {
        if (hpcBoundedBox != null || hpcCentralPoint != null || (hpcX != null && hpcY != null) || hpcBoundCC != null) {
            List<JHVPoint> localHPCBoundedBox = new ArrayList<JHVPoint>();
            List<JHVPoint> localHPCBoundCC = new ArrayList<JHVPoint>();
            JHVPoint localHPCCentralPoint = null;
            if (hpcBoundedBox != null) {
                localHPCBoundedBox = hpcBoundedBox;
            }
            if (hpcBoundCC != null) {
                localHPCBoundCC = hpcBoundCC;
            }
            if (hpcCentralPoint != null) {
                localHPCCentralPoint = hpcCentralPoint;
            } else {
                if (hpcX != null && hpcY != null) {
                    localHPCCentralPoint = new JHVPoint(hpcX, hpcY, null);
                }
            }
            currentEvent.addJHVPositionInformation(JHVCoordinateSystem.HPC, new HEKPositionInformation(JHVCoordinateSystem.HPC, localHPCBoundedBox, localHPCBoundCC, localHPCCentralPoint));
        }

    }

    private List<JHVEventRelationShipRule> getEventRelationShipRules() {
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
                    HEKEventType relatedWith = new HEKEventType(er.getRelatedWith().getEventName(), eventSource.getSourceName(), eventSupplier.getSupplierName());
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
                HEKEventType relatedWith = new HEKEventType(er.getEvent().getEventName(), eventSource.getSourceName(), eventSupplier.getSupplierName());
                JHVEventRelationShipRule rule = new JHVEventRelationShipRule(relatedWith, relatedOnList);
                rules.add(rule);
            }
        }
        return rules;
    }

    private class Association {

        private String associationType;
        private String associationIvorn1;
        private String associationIvorn2;

        public Association() {
            associationType = "";
            associationIvorn1 = "";
            associationIvorn2 = "";
        }

        /**
         * @return the associationType
         */
        public String getAssociationType() {
            return associationType;
        }

        /**
         * @param associationType
         *            the associationType to set
         */
        public void setAssociationType(String associationType) {
            this.associationType = associationType;
        }

        /**
         * @return the associationIvorn1
         */
        public String getAssociationIvorn1() {
            return associationIvorn1;
        }

        /**
         * @param associationIvorn1
         *            the associationIvorn1 to set
         */
        public void setAssociationIvorn1(String associationIvorn1) {
            this.associationIvorn1 = associationIvorn1;
        }

        /**
         * @return the associationIvorn2
         */
        public String getAssociationIvorn2() {
            return associationIvorn2;
        }

        /**
         * @param associationIvorn2
         *            the associationIvorn2 to set
         */
        public void setAssociationIvorn2(String associationIvorn2) {
            this.associationIvorn2 = associationIvorn2;
        }
    }

}
