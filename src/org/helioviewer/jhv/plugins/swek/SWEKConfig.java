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

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.events.SWEK;
import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.gui.IconBank;
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
        int len = sourcesArray.length();
        for (int i = 0; i < len; i++) {
            SWEK.Source source = parseSource(sourcesArray.getJSONObject(i));
            if (source != null) {
                sources.put(source.name(), source);
            }
        }
    }

    @Nullable
    private static SWEK.Source parseSource(JSONObject obj) {
        String name = obj.getString("name");
        return switch (name) {
            case "COMESEP" -> new SWEK.Source(name, parseParameters(obj.getJSONArray("general_parameters")), new ComesepHandler());
            //case "FHNW" -> new SWEK.Source(name, parseParameters(obj.getJSONArray("general_parameters")), new FHNWHandler());
            case "HEK" -> new SWEK.Source(name, parseParameters(obj.getJSONArray("general_parameters")), new HEKHandler());
            default -> null;
        };
    }

    private static List<SWEK.Parameter> parseParameters(JSONArray parameterArray) {
        int len = parameterArray.length();
        List<SWEK.Parameter> parameterList = new ArrayList<>(len);
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
        SWEKGroup group = new SWEKGroup(obj.getString("event_name"), parseParameters(obj.getJSONArray("parameter_list")), parseEventIcon(obj), dtm);
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
            return "iconbank".equals(eventIconURI.getScheme()) ? SWEKIconBank.getIcon(eventIconURI.getHost()) : SWEKIconBank.getIcon("Other");
        } catch (URISyntaxException e) {
            Log.warn(eventIconValue, e);
        }
        return IconBank.getBlank();
    }

    private static SWEKSupplier parseSupplier(JSONObject obj, SWEKGroup group) {
        return new SWEKSupplier(obj.getString("supplier_name"), obj.getString("supplier_display_name"), sources.get(obj.getString("source")), obj.getString("db"), group);
    }

    private static SWEK.Parameter parseParameter(JSONObject obj) {
        return new SWEK.Parameter(obj.getString("parameter_name"), obj.getString("parameter_display_name"), parseParameterFilter(obj), obj.optBoolean("default_visible"));
    }

    @Nullable
    private static SWEK.ParameterFilter parseParameterFilter(JSONObject obj) {
        JSONObject filterobj = obj.optJSONObject("filter");
        if (filterobj == null)
            return null;
        return new SWEK.ParameterFilter(filterobj.getString("filter_type"), filterobj.getDouble("min"), filterobj.getDouble("max"), filterobj.getDouble("start_value"), filterobj.getDouble("step_size"), filterobj.getString("units"), filterobj.getString("dbtype"));
    }

    private static List<SWEK.RelatedEvents> parseRelatedEvents(JSONObject obj) {
        JSONArray relatedEventsArray = obj.getJSONArray("related_events");
        int len = relatedEventsArray.length();
        List<SWEK.RelatedEvents> relatedEventsList = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            JSONObject relatedEvent = relatedEventsArray.getJSONObject(i);
            relatedEventsList.add(new SWEK.RelatedEvents(groups.get(relatedEvent.getString("event_name")), groups.get(relatedEvent.getString("related_with")), parseRelatedOnList(relatedEvent)));
        }
        return relatedEventsList;
    }

    private static List<SWEK.RelatedOn> parseRelatedOnList(JSONObject obj) {
        JSONArray relatedOnArray = obj.getJSONArray("related_on");
        int len = relatedOnArray.length();
        List<SWEK.RelatedOn> relatedOnList = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            JSONObject relatedOn = relatedOnArray.getJSONObject(i);
            String parameterFrom = relatedOn.getString("parameter_from");
            String parameterWith = relatedOn.getString("parameter_with");
            relatedOnList.add(new SWEK.RelatedOn(new SWEK.Parameter(parameterFrom, parameterFrom, null, false), new SWEK.Parameter(parameterWith, parameterWith, null, false), relatedOn.getString("dbtype")));
        }
        return relatedOnList;
    }

}
