package org.helioviewer.jhv.plugins.swek;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.event.SWEKParameter;
import org.helioviewer.jhv.data.event.SWEKParameterFilter;
import org.helioviewer.jhv.data.event.SWEKRelatedEvents;
import org.helioviewer.jhv.data.event.SWEKRelatedOn;
import org.helioviewer.jhv.data.event.SWEKSource;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.plugins.swek.sources.comesep.ComesepHandler;
import org.helioviewer.jhv.plugins.swek.sources.hek.HEKHandler;
import org.json.JSONArray;
import org.json.JSONObject;

class SWEKConfig {

    private static final HashMap<String, SWEKSource> sources = new HashMap<>();
    private static final HashMap<String, SWEKGroup> groups = new HashMap<>();
    private static final List<SWEKGroup> orderedGroups = new ArrayList<>();

    static List<SWEKGroup> load() {
        SWEKIconBank.init();
        try (InputStream in = FileUtils.getResource("/settings/SWEK.json")) {
            JSONObject jo = JSONUtils.get(in);
            EventDatabase.config_hash = Arrays.hashCode(jo.toString().toCharArray());
            parseSources(jo);
            parseGroups(jo);
            SWEKGroup.setSwekRelatedEvents(parseRelatedEvents(jo));
        } catch (Exception e) {
            Log.error("Configuration file could not be parsed: " + e);
        }
        return orderedGroups;
    }

    private static void parseSources(JSONObject obj) {
        JSONArray sourcesArray = obj.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.length(); i++) {
            SWEKSource source = parseSource(sourcesArray.getJSONObject(i));
            if (source != null) {
                sources.put(source.getName(), source);
            }
        }
    }

    @Nullable
    private static SWEKSource parseSource(JSONObject obj) {
        String name = obj.getString("name");
        switch (name) {
            case "HEK":
                return new SWEKSource(name, parseGeneralParameters(obj), new HEKHandler());
            case "COMESEP":
                return new SWEKSource(name, parseGeneralParameters(obj), new ComesepHandler());
            default:
                return null;
        }
    }

    private static List<SWEKParameter> parseGeneralParameters(JSONObject obj) {
        JSONArray parameterArray = obj.getJSONArray("general_parameters");
        int len = parameterArray.length();
        List<SWEKParameter> parameterList = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            parameterList.add(parseParameter(parameterArray.getJSONObject(i)));
        }
        return parameterList;
    }

    private static void parseGroups(JSONObject obj) {
        JSONArray eventJSONArray = obj.getJSONArray("events_types");
        for (int i = 0; i < eventJSONArray.length(); i++) {
            SWEKGroup group = parseGroup(eventJSONArray.getJSONObject(i));
            groups.put(group.getName(), group);
            orderedGroups.add(group);
        }
    }

    private static SWEKGroup parseGroup(JSONObject obj) {
        SWEKGroup group = new SWEKGroup(parseEventName(obj), parseParameterList(obj), parseEventIcon(obj));
        List<SWEKSupplier> suppliers = parseSuppliers(obj, group);
        group.setSuppliers(suppliers);
        return group;
    }

    @Nullable
    private static ImageIcon parseEventIcon(JSONObject obj) {
        String eventIconValue = obj.getString("icon");
        try {
            URI eventIconURI = new URI(eventIconValue);
            return eventIconURI.getScheme().equals("iconbank") ? SWEKIconBank.getIcon(eventIconURI.getHost()) : SWEKIconBank.getIcon("Other");
        } catch (URISyntaxException e) {
            Log.info("Could not parse the URI " + eventIconValue + ", null icon returned");
        }
        return null;
    }

    private static String parseEventName(JSONObject obj) {
        return obj.getString("event_name");
    }

    private static List<SWEKSupplier> parseSuppliers(JSONObject obj, SWEKGroup group) {
        JSONArray suppliersArray = obj.getJSONArray("suppliers");
        int len = suppliersArray.length();
        List<SWEKSupplier> suppliers = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            suppliers.add(parseSupplier(suppliersArray.getJSONObject(i), group));
        }
        return suppliers;
    }

    private static SWEKSupplier parseSupplier(JSONObject obj, SWEKGroup group) {
        return new SWEKSupplier(parseSupplierName(obj), parseSupplierDisplayName(obj), group, parseSupplierSource(obj), parseDbName(obj));
    }

    private static String parseSupplierName(JSONObject obj) {
        return obj.getString("supplier_name");
    }

    private static String parseSupplierDisplayName(JSONObject obj) {
        return obj.getString("supplier_display_name");
    }

    private static SWEKSource parseSupplierSource(JSONObject obj) {
        return sources.get(obj.getString("source"));
    }

    private static String parseDbName(JSONObject obj) {
        return obj.getString("db");
    }

    private static List<SWEKParameter> parseParameterList(JSONObject obj) {
        JSONArray parameterListArray = obj.getJSONArray("parameter_list");
        int len = parameterListArray.length();
        List<SWEKParameter> parameterList = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            parameterList.add(parseParameter((JSONObject) parameterListArray.get(i)));
        }
        return parameterList;
    }

    private static SWEKParameter parseParameter(JSONObject obj) {
        return new SWEKParameter(parseParameterName(obj), parseParameterDisplayName(obj), parseParameterFilter(obj), parseDefaultVisible(obj));
    }

    private static String parseParameterName(JSONObject obj) {
        return obj.getString("parameter_name");
    }

    private static String parseParameterDisplayName(JSONObject obj) {
        return obj.getString("parameter_display_name");
    }

    @Nullable
    private static SWEKParameterFilter parseParameterFilter(JSONObject obj) {
        JSONObject filterobj = obj.optJSONObject("filter");
        if (filterobj == null)
            return null;
        return new SWEKParameterFilter(parseFilterType(filterobj), parseMin(filterobj), parseMax(filterobj), parseStartValue(filterobj), parseStepSize(filterobj), parseUnits(filterobj), parseDbType(filterobj));
    }

    private static String parseDbType(JSONObject obj) {
        return obj.getString("dbtype");
    }

    private static String parseUnits(JSONObject obj) {
        return obj.getString("units");
    }

    private static String parseFilterType(JSONObject obj) {
        return obj.getString("filter_type");
    }

    private static double parseMin(JSONObject obj) {
        return obj.getDouble("min");
    }

    private static double parseMax(JSONObject obj) {
        return obj.getDouble("max");
    }

    private static double parseStepSize(JSONObject obj) {
        return obj.getDouble("step_size");
    }

    private static double parseStartValue(JSONObject obj) {
        return obj.getDouble("start_value");
    }

    private static boolean parseDefaultVisible(JSONObject obj) {
        return obj.optBoolean("default_visible");
    }

    private static SWEKRelatedEvents parseRelatedEvent(JSONObject obj) {
        return new SWEKRelatedEvents(parseRelatedEventName(obj), parseRelatedWith(obj), parseRelatedOnList(obj));
    }

    private static SWEKGroup parseRelatedEventName(JSONObject obj) {
        return groups.get(obj.getString("event_name"));
    }

    private static SWEKGroup parseRelatedWith(JSONObject obj) {
        return groups.get(obj.getString("related_with"));
    }

    private static List<SWEKRelatedEvents> parseRelatedEvents(JSONObject obj) {
        JSONArray relatedEventsArray = obj.getJSONArray("related_events");
        int len = relatedEventsArray.length();
        List<SWEKRelatedEvents> relatedEventsList = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            relatedEventsList.add(parseRelatedEvent(relatedEventsArray.getJSONObject(i)));
        }
        return relatedEventsList;
    }

    private static List<SWEKRelatedOn> parseRelatedOnList(JSONObject obj) {
        JSONArray relatedOnArray = obj.getJSONArray("related_on");
        int len = relatedOnArray.length();
        List<SWEKRelatedOn> relatedOnList = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            relatedOnList.add(parseRelatedOn(relatedOnArray.getJSONObject(i)));
        }
        return relatedOnList;
    }

    private static SWEKRelatedOn parseRelatedOn(JSONObject obj) {
        return new SWEKRelatedOn(parseParameterFrom(obj), parseParameterWith(obj), parseDbType(obj));
    }

    private static SWEKParameter parseParameterFrom(JSONObject obj) {
        String parameterName = obj.getString("parameter_from");
        return new SWEKParameter(parameterName, parameterName, null, false);
    }

    private static SWEKParameter parseParameterWith(JSONObject obj) {
        String parameterName = obj.getString("parameter_with");
        return new SWEKParameter(parameterName, parameterName, null, false);
    }

}
