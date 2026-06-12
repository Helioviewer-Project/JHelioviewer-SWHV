package org.helioviewer.jhv.plugins.swek;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.plugins.swek.sources.ComesepHandler;
//import org.helioviewer.jhv.plugins.swek.sources.FHNWHandler;
import org.helioviewer.jhv.plugins.swek.sources.HEKHandler;

import org.json.JSONArray;
import org.json.JSONObject;

class SWEKConfig {

    private static final HashMap<String, SWEK.Source> sources = new HashMap<>();
    private static final HashMap<String, SWEKGroup> groups = new HashMap<>();

    static DefaultTreeModel load() {
        SWEKIconBank.init();
        try (InputStream in = FileUtils.getResource("/settings/SWEK.json")) {
            JSONObject jo = JSONUtils.get(in);
            EventDatabase.config_hash = Arrays.hashCode(jo.toString().toCharArray());
            parseSources(jo);

            DefaultTreeModel dtm = parseGroups(jo);
            SWEKGroup.setSWEKRelatedEvents(parseRelatedEvents(jo));
            return dtm;
        } catch (Exception e) {
            Log.error(e);
            return new DefaultTreeModel(new DefaultMutableTreeNode(""));
        }
    }

    private static void parseSources(JSONObject obj) {
        JSONArray sourcesArray = obj.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.length(); i++) {
            SWEK.Source source = parseSource(sourcesArray.getJSONObject(i));
            if (source != null)
                sources.put(source.name(), source);
        }
    }

    @Nullable
    private static SWEK.Source parseSource(JSONObject obj) {
        String name = obj.getString("name");
        return switch (name) {
            case "COMESEP" ->
                    new SWEK.Source(name, parseParameters(obj.getJSONArray("general_parameters")), new ComesepHandler());
            //case "FHNW" -> new SWEK.Source(name, parseParameters(obj.getJSONArray("general_parameters")), new FHNWHandler());
            case "HEK" ->
                    new SWEK.Source(name, parseParameters(obj.getJSONArray("general_parameters")), new HEKHandler());
            default -> null;
        };
    }

    private static List<SWEK.Parameter> parseParameters(JSONArray parameterArray) {
        List<SWEK.Parameter> parameterList = new ArrayList<>(parameterArray.length());
        for (int i = 0; i < parameterArray.length(); i++) {
            JSONObject parameter = parameterArray.getJSONObject(i);
            parameterList.add(new SWEK.Parameter(parameter.getString("parameter_name"), parameter.getString("parameter_display_name"), parseParameterFilter(parameter), parameter.optBoolean("default_visible")));
        }
        return parameterList;
    }

    private static DefaultTreeModel parseGroups(JSONObject obj) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        DefaultTreeModel dtm = new DefaultTreeModel(root);

        JSONArray eventJSONArray = obj.getJSONArray("events_types");
        for (int i = 0; i < eventJSONArray.length(); i++) {
            try {
                SWEKGroup group = parseGroup(eventJSONArray.getJSONObject(i), dtm);
                root.add(group);
                groups.put(group.getName(), group);
            } catch (Exception e) { // allow continuing when a source is disabled
                Log.error(e);
            }
        }
        return dtm;
    }

    private static SWEKGroup parseGroup(JSONObject obj, DefaultTreeModel dtm) {
        SWEKGroup group = new SWEKGroup(obj.getString("event_name"), parseParameters(obj.getJSONArray("parameter_list")), parseEventIconKey(obj), dtm);
        JSONArray suppliersArray = obj.getJSONArray("suppliers");
        for (int i = 0; i < suppliersArray.length(); i++) {
            JSONObject supplier = suppliersArray.getJSONObject(i);
            group.add(new SWEKSupplier(supplier.getString("supplier_name"), supplier.getString("supplier_display_name"), sources.get(supplier.getString("source")), supplier.getString("db")));
        }
        return group;
    }

    private static String parseEventIconKey(JSONObject obj) {
        String eventIconValue = obj.getString("icon");
        try {
            URI eventIconURI = new URI(eventIconValue);
            return "iconbank".equals(eventIconURI.getScheme()) ? eventIconURI.getHost() : "Other";
        } catch (URISyntaxException e) {
            Log.warn(eventIconValue, e);
        }
        return "Other";
    }

    @Nullable
    private static SWEK.ParameterFilter parseParameterFilter(JSONObject obj) {
        JSONObject filter = obj.optJSONObject("filter");
        if (filter == null)
            return null;
        return new SWEK.ParameterFilter(filter.getString("filter_type"), filter.getDouble("min"), filter.getDouble("max"), filter.getDouble("start_value"), filter.getDouble("step_size"), filter.getString("units"), filter.getString("dbtype"));
    }

    private static List<SWEK.RelatedEvents> parseRelatedEvents(JSONObject obj) {
        JSONArray relatedEventsArray = obj.getJSONArray("related_events");
        List<SWEK.RelatedEvents> relatedEventsList = new ArrayList<>(relatedEventsArray.length());
        for (int i = 0; i < relatedEventsArray.length(); i++) {
            JSONObject relatedEvent = relatedEventsArray.getJSONObject(i);
            relatedEventsList.add(new SWEK.RelatedEvents(groups.get(relatedEvent.getString("event_name")), groups.get(relatedEvent.getString("related_with")), parseRelatedOnList(relatedEvent)));
        }
        return relatedEventsList;
    }

    private static List<SWEK.RelatedOn> parseRelatedOnList(JSONObject obj) {
        JSONArray relatedOnArray = obj.getJSONArray("related_on");
        List<SWEK.RelatedOn> relatedOnList = new ArrayList<>(relatedOnArray.length());
        for (int i = 0; i < relatedOnArray.length(); i++) {
            JSONObject relatedOn = relatedOnArray.getJSONObject(i);
            String parameterFrom = relatedOn.getString("parameter_from");
            String parameterWith = relatedOn.getString("parameter_with");
            relatedOnList.add(new SWEK.RelatedOn(new SWEK.Parameter(parameterFrom, parameterFrom, null, false), new SWEK.Parameter(parameterWith, parameterWith, null, false), relatedOn.getString("dbtype")));
        }
        return relatedOnList;
    }

}
