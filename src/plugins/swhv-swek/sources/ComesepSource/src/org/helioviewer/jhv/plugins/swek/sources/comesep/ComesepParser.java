package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelation;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.config.SWEKRelatedEvents;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;
import org.helioviewer.jhv.plugins.swek.sources.SWEKParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ComesepParser implements SWEKParser {

    private SWEKEventType eventType;
    private SWEKSource eventSource;
    private SWEKSupplier eventSupplier;
    private List<SWEKRelatedEvents> eventRelationRules;
    private final ComesepEventStream eventStream;
    private boolean parserStopped;
    private final HashMap<String, List<Association>> associationsMap;
    private final HashMap<String, ComesepEvent> associationEventsMap;

    /**
     * Creates a parser for the given event type and event source.
     * 
     * @param eventType
     *            the type of the event
     * @param source
     *            the source of the event
     * 
     */
    public ComesepParser() {
        parserStopped = false;
        eventStream = new ComesepEventStream();
        associationsMap = new HashMap<String, List<Association>>();
        associationEventsMap = new HashMap<String, ComesepEvent>();
    }

    @Override
    public void stopParser() {
        parserStopped = true;

    }

    @Override
    public SWEKEventStream parseEventStream(InputStream downloadInputStream, SWEKEventType eventType, SWEKSource swekSource,
            SWEKSupplier swekSupplier, List<SWEKRelatedEvents> relatedEvents) {
        this.eventType = eventType;
        eventSource = swekSource;
        eventSupplier = swekSupplier;
        eventRelationRules = relatedEvents;
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
                parseAssociation(eventJSON);
                parseEventJSON(eventJSON);
                return eventStream;
            } else {
                // TODO inform the user hek is probably death...
                Log.error("Download input stream was null. Probably the comesep server is down.");
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

    private void parseEventJSON(JSONObject eventJSON) throws JSONException {
        JSONArray results = eventJSON.getJSONArray("results");
        ComesepEventType comesepEventType = new ComesepEventType(eventType.getEventName(), eventSource.getSourceName(),
                eventSupplier.getSupplierName());
        for (int i = 0; i < results.length() && !parserStopped; i++) {
            ComesepEvent currentEvent = new ComesepEvent(eventType.getEventName(), eventType.getEventName(), "", comesepEventType,
                    eventType.getEventIcon(), eventType.getColor());
            JSONObject result = results.getJSONObject(i);
            parseResult(result, currentEvent);
            if (associationsMap.containsKey(currentEvent.getUniqueID())) {
                // There is an association with the current event
                associationEventsMap.put(currentEvent.getUniqueID(), currentEvent);
                List<Association> associationsList = associationsMap.get(currentEvent.getUniqueID());
                for (Association association : associationsList) {
                    if (association.getAssociationParent().equals(currentEvent.getUniqueID())) {
                        // current event is the parent of the association
                        if (associationEventsMap.containsKey(association.getAssociationParent())) {
                            // The parent event of the association is available
                            ComesepEvent associatedEvent = associationEventsMap.get(association.getAssociationParent());
                            // Is a sequence relation so associated event is
                            // follow-up of current event
                            if (currentEvent.getEventRelationShip().getPrecedingEvents().isEmpty()) {
                                currentEvent.getEventRelationShip().setRelationshipColor(ComesepColors.getNextColor());
                            }
                            associatedEvent.getEventRelationShip().getPrecedingEvents()
                                    .put(currentEvent.getUniqueID(), new JHVEventRelation(currentEvent.getUniqueID(), currentEvent));
                            currentEvent
                                    .getEventRelationShip()
                                    .getNextEvents()
                                    .put(associatedEvent.getUniqueID(),
                                            new JHVEventRelation(associatedEvent.getUniqueID(), associatedEvent));
                            associatedEvent.getEventRelationShip().setRelationshipColor(
                                    currentEvent.getEventRelationShip().getRelationshipColor());
                        } else {
                            // The associated event is not in the list so we
                            // start a
                            // new color and add an association in the list
                            // without
                            // event reference.
                            currentEvent.getEventRelationShip().setRelationshipColor(ComesepColors.getNextColor());
                            currentEvent.getEventRelationShip().getNextEvents()
                                    .put(association.getAssociationChild(), new JHVEventRelation(association.getAssociationChild()));

                        }
                    } else if (association.getAssociationChild().equals(currentEvent.getUniqueID())) {
                        // current event is the child event of the relationship
                        if (associationEventsMap.containsKey(association.getAssociationParent())) {
                            // the associated event is available
                            ComesepEvent associatedEvent = associationEventsMap.get(association.getAssociationParent());
                            // Is a sequence relation so the current event
                            // is
                            // the follow-up of the associated event
                            associatedEvent.getEventRelationShip().getNextEvents()
                                    .put(currentEvent.getUniqueID(), new JHVEventRelation(currentEvent.getUniqueID(), currentEvent));
                            currentEvent
                                    .getEventRelationShip()
                                    .getPrecedingEvents()
                                    .put(associatedEvent.getUniqueID(),
                                            new JHVEventRelation(associatedEvent.getUniqueID(), associatedEvent));
                            currentEvent.getEventRelationShip().setRelationshipColor(
                                    associatedEvent.getEventRelationShip().getRelationshipColor());

                        } else {
                            // The associated event is not in the list so we
                            // start a
                            // new color add we already add a reference to the
                            // previous event without event type.
                            currentEvent.getEventRelationShip().setRelationshipColor(ComesepColors.getNextColor());
                            currentEvent.getEventRelationShip().getPrecedingEvents()
                                    .put(association.getAssociationParent(), new JHVEventRelation(association.getAssociationParent()));

                        }
                    }
                }
            }
            eventStream.addJHVEvent(currentEvent);
        }
    }

    private void parseResult(JSONObject result, ComesepEvent currentEvent) throws JSONException {
        Iterator<?> keys = result.keys();
        while (keys.hasNext()) {
            parseParameter(result, keys.next(), currentEvent);
        }
    }

    private void parseParameter(JSONObject result, Object key, ComesepEvent currentEvent) throws JSONException {
        if (key instanceof String) {
            String keyString = (String) key;
            String value = result.getString((String) key);
            if (value.toLowerCase().equals("null")) {
                value = null;
            }
            // Event start time
            if (keyString.toLowerCase().equals("atearliest")) {
                currentEvent.setStartTime(parseDate(value));
            } else
            // Event end time
            if (keyString.toLowerCase().equals("atlatest")) {
                currentEvent.setEndTime(parseDate(value));
            } else
            // event unique ID
            if (keyString.toLowerCase().equals("alertid")) {
                currentEvent.setUniqueID(value);
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

    private Date parseDate(String value) {
        return new Date(Long.parseLong(value));
    }

    /**
     * 
     * 
     * @param eventJSON
     * @throws JSONException
     */
    private void parseAssociation(JSONObject eventJSON) throws JSONException {
        JSONArray associations = eventJSON.getJSONArray("associations");
        for (int i = 0; i < associations.length() && !parserStopped; i++) {
            Association currentAssociation = new Association();
            currentAssociation.setAssociationChild(parseAssociationChild(associations.getJSONObject(i)));
            currentAssociation.setAssociationParent(parseAssociationParent(associations.getJSONObject(i)));
            List<Association> associationsChildren = new ArrayList<Association>();
            List<Association> associationsParents = new ArrayList<Association>();
            if (associationsMap.containsKey(currentAssociation.getAssociationChild())) {
                associationsChildren = associationsMap.get(currentAssociation.getAssociationChild());
            }
            if (associationsMap.containsKey(currentAssociation.getAssociationParent())) {
                associationsParents = associationsMap.get(currentAssociation.getAssociationParent());
            }
            associationsChildren.add(currentAssociation);
            associationsParents.add(currentAssociation);
            associationsMap.put(currentAssociation.getAssociationChild(), associationsChildren);
            associationsMap.put(currentAssociation.getAssociationParent(), associationsParents);
        }
    }

    /**
     * 
     * 
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    private String parseAssociationParent(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("child");
    }

    /**
     * 
     * 
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    private String parseAssociationChild(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("parent");
    }

    private class Association {

        private String associationChild;
        private String associationParent;

        public Association() {
            associationChild = "";
            associationParent = "";
        }

        /**
         * @return the associationIvorn1
         */
        public String getAssociationChild() {
            return associationChild;
        }

        /**
         * @param associationChild
         *            the associationChild to set
         */
        public void setAssociationChild(String associationChild) {
            this.associationChild = associationChild;
        }

        /**
         * @return the associationParent
         */
        public String getAssociationParent() {
            return associationParent;
        }

        /**
         * @param associationParent
         *            the associationParent to set
         */
        public void setAssociationParent(String associationParent) {
            this.associationParent = associationParent;
        }

    }
}
