package org.helioviewer.jhv.plugins.swek;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
//import org.helioviewer.jhv.plugins.swek.sources.ComesepHandler;
//import org.helioviewer.jhv.plugins.swek.sources.FHNWHandler;
import org.helioviewer.jhv.plugins.swek.sources.HEKHandler;

import org.json.JSONArray;
import org.json.JSONObject;

class SWEKConfig {

    static List<SWEKGroup> load() {
        SWEKCatalog.clear();
        try (InputStream in = FileUtils.getResource("/settings/SWEK.json")) {
            JSONObject jo = JSONUtils.get(in);
            EventDatabase.config_hash = Arrays.hashCode(jo.toString().toCharArray());
            parseSources(jo);

            List<SWEKGroup> groupList = parseGroups(jo);
            SWEKCatalog.setRelatedEvents(parseRelatedEvents(jo));
            return groupList;
        } catch (Exception e) {
            Log.error(e);
            return List.of();
        }
    }

    private static void parseSources(JSONObject obj) {
        JSONArray sourcesArray = obj.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.length(); i++) {
            SWEK.Source source = parseSource(sourcesArray.getJSONObject(i));
            if (source != null)
                SWEKCatalog.addSource(source);
        }
    }

    @Nullable
    private static SWEK.Source parseSource(JSONObject obj) {
        String name = obj.getString("name");
        //if ("COMESEP".equals(name))
        //    return new SWEK.Source(name, parseParameters(obj.getJSONArray("general_parameters")), new ComesepHandler());
        //if ("FHNW".equals(name))
        //    return new SWEK.Source(name, parseParameters(obj.getJSONArray("general_parameters")), new FHNWHandler());
        if ("HEK".equals(name))
            return new SWEK.Source(name, parseParameters(obj.getJSONArray("general_parameters")), new HEKHandler());
        return null;
    }

    private static List<SWEK.Parameter> parseParameters(JSONArray parameterArray) {
        List<SWEK.Parameter> parameterList = new ArrayList<>(parameterArray.length());
        for (int i = 0; i < parameterArray.length(); i++) {
            JSONObject parameter = parameterArray.getJSONObject(i);
            parameterList.add(new SWEK.Parameter(parameter.getString("parameter_name"), parameter.getString("parameter_display_name"), parseParameterFilter(parameter), parameter.optBoolean("default_visible")));
        }
        return parameterList;
    }

    private static List<SWEKGroup> parseGroups(JSONObject obj) {
        List<SWEKGroup> groupList = new ArrayList<>();
        JSONArray eventJSONArray = obj.getJSONArray("events_types");
        for (int i = 0; i < eventJSONArray.length(); i++) {
            try {
                addGroup(eventJSONArray.getJSONObject(i), groupList);
            } catch (Exception e) { // allow continuing when a source is disabled
                Log.error(e);
            }
        }
        return groupList;
    }

    private static void addGroup(JSONObject obj, List<SWEKGroup> groupList) {
        SWEKGroup group = new SWEKGroup(obj.getString("event_name"), parseParameters(obj.getJSONArray("parameter_list")), parseEventIconKey(obj));

        JSONArray suppliersArray = obj.getJSONArray("suppliers");
        for (int i = 0; i < suppliersArray.length(); i++) {
            JSONObject supplier = suppliersArray.getJSONObject(i);

            String supplierName = supplier.getString("supplier_name");
            String sourceName = supplier.getString("source");
            SWEK.Source source = SWEKCatalog.getSource(sourceName);
            if (source == null)
                continue;

            SWEKSupplier supplierObj = new SWEKSupplier(group, supplierName, supplier.getString("supplier_display_name"), source, supplier.getString("db"));
            SWEKCatalog.add(supplierObj);
        }
        if (SWEKCatalog.getSuppliers(group).isEmpty())
            return;

        groupList.add(group);
        SWEKCatalog.addGroup(group);
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
            SWEKGroup group = SWEKCatalog.getGroup(relatedEvent.getString("event_name"));
            SWEKGroup relatedWith = SWEKCatalog.getGroup(relatedEvent.getString("related_with"));
            if (group != null && relatedWith != null)
                relatedEventsList.add(new SWEK.RelatedEvents(group, relatedWith, parseRelatedOnList(relatedEvent)));
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
