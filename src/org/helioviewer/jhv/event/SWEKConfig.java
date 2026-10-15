package org.helioviewer.jhv.event;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public final class SWEKConfig {

    private SWEKConfig() {}

    public static SWEKCatalog load(Map<String, SWEKHandler> handlers) {
        try (InputStream in = FileUtils.getResource("/settings/SWEK.json")) {
            JSONObject jo = JSONUtils.get(in);
            int configHash = Arrays.hashCode(jo.toString().toCharArray());
            Map<String, SWEK.Source> sources = parseSources(jo, handlers);
            HashMap<String, SWEKGroup> groupsByName = new HashMap<>();
            List<SWEKSupplier> suppliers = new ArrayList<>();
            parseGroups(jo, sources, groupsByName, suppliers);
            return new SWEKCatalog(configHash, suppliers, parseRelations(jo, groupsByName));
        } catch (Exception e) {
            Log.error(e);
            return new SWEKCatalog(0, List.of(), List.of());
        }
    }

    private static Map<String, SWEK.Source> parseSources(JSONObject obj, Map<String, SWEKHandler> handlers) {
        HashMap<String, SWEK.Source> sources = new HashMap<>();
        JSONArray sourcesArray = obj.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.length(); i++) {
            SWEK.Source source = parseSource(sourcesArray.getJSONObject(i), handlers);
            if (source != null)
                sources.put(source.name(), source);
        }
        return sources;
    }

    @Nullable
    private static SWEK.Source parseSource(JSONObject obj, Map<String, SWEKHandler> handlers) {
        String name = obj.getString("name");
        SWEKHandler handler = handlers.get(name);
        return handler == null ? null : new SWEK.Source(name, parseParameters(obj.getJSONArray("general_parameters")), handler, parseNumericParameters(obj));
    }

    private static Map<String, SWEK.NumericType> parseNumericParameters(JSONObject source) {
        JSONObject definitions = source.getJSONObject("numeric_parameters");
        Map<String, SWEK.NumericType> types = new HashMap<>();
        for (String name : definitions.keySet())
            types.put(name, SWEK.NumericType.valueOf(definitions.getString(name).toUpperCase()));
        return types;
    }

    private static List<SWEK.Parameter> parseParameters(JSONArray parameterArray) {
        List<SWEK.Parameter> parameterList = new ArrayList<>(parameterArray.length());
        for (int i = 0; i < parameterArray.length(); i++) {
            JSONObject parameter = parameterArray.getJSONObject(i);
            parameterList.add(new SWEK.Parameter(parameter.getString("parameter_name"), parameter.getString("parameter_display_name"), parseParameterFilter(parameter), parameter.optBoolean("default_visible")));
        }
        return parameterList;
    }

    private static List<SWEK.Parameter> parseSupplierParameters(JSONObject supplier, List<SWEK.Parameter> defaultParameters) {
        if (supplier.has("parameter_list"))
            return parseParameters(supplier.getJSONArray("parameter_list"));

        if (!supplier.has("extra_parameters"))
            return defaultParameters;

        List<SWEK.Parameter> parameters = new ArrayList<>(defaultParameters);
        parameters.addAll(parseParameters(supplier.getJSONArray("extra_parameters")));
        return parameters;
    }

    private static void parseGroups(JSONObject obj, Map<String, SWEK.Source> sources, Map<String, SWEKGroup> groupsByName, List<SWEKSupplier> suppliers) {
        JSONArray eventJSONArray = obj.getJSONArray("events_types");
        for (int i = 0; i < eventJSONArray.length(); i++)
            addGroup(eventJSONArray.getJSONObject(i), sources, groupsByName, suppliers);
    }

    private static void addGroup(JSONObject obj, Map<String, SWEK.Source> sources, Map<String, SWEKGroup> groupsByName, List<SWEKSupplier> suppliers) {
        List<SWEK.Parameter> defaultParameters = parseParameters(obj.getJSONArray("parameter_list"));
        SWEKGroup group = new SWEKGroup(obj.getString("event_name"), parseEventIconKey(obj));

        JSONArray suppliersArray = obj.getJSONArray("suppliers");
        for (int i = 0; i < suppliersArray.length(); i++) {
            JSONObject supplier = suppliersArray.getJSONObject(i);

            String supplierName = supplier.getString("supplier_name");
            String sourceName = supplier.getString("source");
            SWEK.Source source = sources.get(sourceName);
            if (source == null)
                continue;

            List<SWEK.Parameter> parameters = parseSupplierParameters(supplier, defaultParameters);
            SWEKSupplier supplierObj = new SWEKSupplier(group, supplierName, supplier.getString("supplier_display_name"), source, supplier.getString("id"), parameters);
            suppliers.add(supplierObj);
        }
        if (suppliers.stream().noneMatch(supplier -> supplier.group() == group))
            return;

        groupsByName.put(group.getName(), group);
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
        return new SWEK.ParameterFilter(filter.getString("filter_type"), filter.getDouble("min"), filter.getDouble("max"), filter.getDouble("start_value"),
                filter.getDouble("step_size"), filter.getString("units"));
    }

    private static List<SWEK.Relation> parseRelations(JSONObject obj, Map<String, SWEKGroup> groupsByName) {
        JSONArray relationsArray = obj.getJSONArray("related_events");
        List<SWEK.Relation> relations = new ArrayList<>(relationsArray.length());
        for (int i = 0; i < relationsArray.length(); i++) {
            JSONObject relation = relationsArray.getJSONObject(i);
            SWEKGroup group = groupsByName.get(relation.getString("event_name"));
            SWEKGroup relatedWith = groupsByName.get(relation.getString("related_with"));
            if (group != null && relatedWith != null)
                relations.add(new SWEK.Relation(group, relatedWith, parseRelatedOnList(relation)));
        }
        return relations;
    }

    private static List<SWEK.RelatedOn> parseRelatedOnList(JSONObject obj) {
        JSONArray relatedOnArray = obj.getJSONArray("related_on");
        List<SWEK.RelatedOn> relatedOnList = new ArrayList<>(relatedOnArray.length());
        for (int i = 0; i < relatedOnArray.length(); i++) {
            JSONObject relatedOn = relatedOnArray.getJSONObject(i);
            String parameterFrom = relatedOn.getString("parameter_from").toLowerCase();
            String parameterWith = relatedOn.getString("parameter_with").toLowerCase();
            relatedOnList.add(new SWEK.RelatedOn(parameterFrom, parameterWith));
        }
        return relatedOnList;
    }

}
