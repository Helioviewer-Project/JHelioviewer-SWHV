package org.helioviewer.jhv.plugins.swek;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKParameter;
import org.helioviewer.jhv.events.SWEKParameterFilter;
import org.helioviewer.jhv.events.SWEKRelatedEvents;
import org.helioviewer.jhv.events.SWEKRelatedOn;
import org.helioviewer.jhv.events.SWEKSource;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.gui.IconBank;
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

    static DefaultTreeModel load() {
        SWEKIconBank.init();
        try (InputStream in = FileUtils.getResource("/settings/SWEK.json")) {
            JSONObject jo = JSONUtils.get(in);
            EventDatabase.config_hash = Arrays.hashCode(jo.toString().toCharArray());
            parseSources(jo);

            DefaultTreeModel dtm = parseGroups(jo);
            SWEKGroup.setSwekRelatedEvents(parseRelatedEvents(jo));
            return dtm;
        } catch (Exception e) {
            Log.error("Configuration file could not be parsed: " + e);
            return new DefaultTreeModel(new DefaultMutableTreeNode(""));
        }
    }

    private static void parseSources(JSONObject obj) {
        JSONArray sourcesArray = obj.getJSONArray("sources");
        int len = sourcesArray.length();
        for (int i = 0; i < len; i++) {
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

    private static DefaultTreeModel parseGroups(JSONObject obj) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        DefaultTreeModel dtm = new DefaultTreeModel(root);

        JSONArray eventJSONArray = obj.getJSONArray("events_types");
        int len = eventJSONArray.length();
        for (int i = 0; i < len; i++) {
            SWEKGroup group = parseGroup(eventJSONArray.getJSONObject(i), dtm);
            root.add(group);
            groups.put(group.getName(), group);
        }
        return dtm;
    }

    private static SWEKGroup parseGroup(JSONObject obj, DefaultTreeModel dtm) {
        SWEKGroup group = new SWEKGroup(parseEventName(obj), parseParameterList(obj), parseEventIcon(obj), dtm);
        JSONArray suppliersArray = obj.getJSONArray("suppliers");
        int len = suppliersArray.length();
        for (int i = 0; i < len; i++) {
            group.add(parseSupplier(suppliersArray.getJSONObject(i), group));
        }
        return group;
    }

    @Nonnull
    private static ImageIcon parseEventIcon(JSONObject obj) {
        String eventIconValue = obj.getString("icon");
        try {
            URI eventIconURI = new URI(eventIconValue);
            return eventIconURI.getScheme().equals("iconbank") ? SWEKIconBank.getIcon(eventIconURI.getHost()) : SWEKIconBank.getIcon("Other");
        } catch (URISyntaxException e) {
            Log.info("Could not parse the URI " + eventIconValue);
        }
        return IconBank.getBlank();
    }

    private static String parseEventName(JSONObject obj) {
        return obj.getString("event_name");
    }

    private static SWEKSupplier parseSupplier(JSONObject obj, SWEKGroup group) {
        return new SWEKSupplier(parseSupplierName(obj), parseSupplierDisplayName(obj), parseSupplierSource(obj), parseDbName(obj), group);
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
