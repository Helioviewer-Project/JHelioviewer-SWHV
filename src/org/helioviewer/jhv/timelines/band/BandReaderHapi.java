package org.helioviewer.jhv.timelines.band;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.thread.LatestWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.ac.starlink.hapi.HapiInfo;
import uk.ac.starlink.hapi.HapiParam;
import uk.ac.starlink.hapi.HapiTableReader;
import uk.ac.starlink.hapi.HapiType;
import uk.ac.starlink.hapi.HapiVersion;
import uk.ac.starlink.hapi.ParamReader;
import uk.ac.starlink.hapi.Times;
import uk.ac.starlink.table.RowSequence;

public class BandReaderHapi {

    @FunctionalInterface
    public interface CatalogListener {
        void catalogsLoaded(Map<String, BandDataset[]> catalogs);
    }

    private static final String hapiFormat = "binary";
    private static final CatalogEndpoint[] catalogEndpoints = {
            new CatalogEndpoint("ROB", "https://hapi.swhv.oma.be/SWHV_Timelines/hapi/"),
            //new CatalogEndpoint("ROB Test", "http://swhv-test:4000/hapi/")
    };

    private static final HashMap<CatalogEndpoint, Catalog> catalogs = new HashMap<>();
    private static final LatestWorker<Catalog[]> catalogWorker = new LatestWorker<>("HAPI-Catalog");

    public static String[] getCatalogGroups() {
        String[] groups = new String[catalogEndpoints.length];
        for (int i = 0; i < catalogEndpoints.length; i++)
            groups[i] = catalogEndpoints[i].groupName;
        return groups;
    }

    public static void requestCatalog(CatalogListener listener) {
        catalogWorker.submit(BandReaderHapi::loadCatalogs, (loaded, fresh) -> {
            if (fresh)
                onSuccessCatalogs(loaded, listener);
        });
    }

    static boolean hasCatalog(String url) {
        return findCatalog(url) != null;
    }

    static DatasetRef dataset(String url) {
        Dataset dataset = findDataset(url);
        return new DatasetRef(dataset.requestUrl, dataset.title);
    }

    static Callable<List<BandData>> dataRequest(List<BandType> types, long start, long end) {
        if (types.isEmpty())
            throw new IllegalArgumentException("No HAPI parameters requested");

        Dataset dataset = findDataset(types.getFirst().getBaseUrl());
        HashSet<BandType> requestedTypes = new HashSet<>(types);
        List<DatasetParameter> parameters = dataset.parameters.stream()
                .filter(parameter -> requestedTypes.contains(parameter.type))
                .toList();
        if (parameters.size() != requestedTypes.size())
            throw new IllegalArgumentException("HAPI parameters do not belong to one dataset");

        RequestSchema schema = createRequestSchema(dataset, parameters);
        return () -> getHapiStream(dataset, schema, start, end);
    }

    static List<BandData> readUri(URI uri) throws Exception {
        return getHapiUri(uri);
    }

    record DatasetRef(String key, String title) {}

    private static Catalog[] loadCatalogs() {
        return Arrays.stream(catalogEndpoints).parallel()
                .map(BandReaderHapi::loadCatalog)
                .toArray(Catalog[]::new);
    }

    @Nullable
    private static Catalog loadCatalog(CatalogEndpoint catalogEndpoint) {
        String server = catalogEndpoint.server;
        String endpoint = server.endsWith("/") ? server : server + '/';
        try {
            return getCatalog(endpoint);
        } catch (Exception e) {
            Log.error(endpoint, e);
            return null;
        }
    }

    private static void onSuccessCatalogs(Catalog[] loadedCatalogs, CatalogListener listener) {
        catalogs.clear();
        LinkedHashMap<String, BandDataset[]> datasets = new LinkedHashMap<>();
        for (int i = 0; i < loadedCatalogs.length; i++) {
            Catalog catalog = loadedCatalogs[i];
            CatalogEndpoint endpoint = catalogEndpoints[i];
            if (catalog != null) {
                catalogs.put(endpoint, catalog);
            }
            datasets.put(endpoint.groupName, catalog == null ? new BandDataset[0] : catalog.datasets);
        }
        listener.catalogsLoaded(datasets);
    }

    public static Map<String, List<BandType>> getPredefinedGroups() {
        if (catalogs.isEmpty())
            return Map.of();
        if (catalogs.size() == 1)
            return catalogs.values().iterator().next().predefinedGroups;

        LinkedHashMap<String, List<BandType>> groups = new LinkedHashMap<>();
        for (CatalogEndpoint endpoint : catalogEndpoints) {
            Catalog catalog = catalogs.get(endpoint);
            if (catalog != null) {
                catalog.predefinedGroups.forEach((name, bandTypes) ->
                        groups.computeIfAbsent(name, k -> new ArrayList<>()).addAll(bandTypes));
            }
        }
        return finishPredefinedGroups(groups);
    }

    private static Map<String, List<BandType>> createPredefinedGroups(BandType[] types) {
        LinkedHashMap<String, List<BandType>> groups = new LinkedHashMap<>();
        for (BandType type : types) {
            BandType.PredefinedEntry[] entries = type.getPredefinedEntries();
            for (BandType.PredefinedEntry entry : entries)
                groups.computeIfAbsent(entry.name(), k -> new ArrayList<>()).add(type);
        }
        return finishPredefinedGroups(groups);
    }

    private static Map<String, List<BandType>> finishPredefinedGroups(LinkedHashMap<String, List<BandType>> groups) {
        for (Map.Entry<String, List<BandType>> e : groups.entrySet())
            e.getValue().sort(Comparator.comparingInt(type -> orderFor(type, e.getKey())));
        groups.replaceAll((name, bandTypes) -> List.copyOf(bandTypes));
        return Collections.unmodifiableMap(groups);
    }

    @Nullable
    private static Catalog findCatalog(String baseUrl) {
        for (Catalog catalog : catalogs.values()) {
            if (catalog.datasetsByParameter.containsKey(baseUrl))
                return catalog;
        }
        return null;
    }

    private static Dataset findDataset(String url) {
        Catalog catalog = findCatalog(url);
        Dataset dataset = catalog == null ? null : catalog.datasetsByParameter.get(url);
        if (dataset == null)
            throw new IllegalArgumentException("Unknown HAPI parameter: " + url);
        return dataset;
    }

    private static int orderFor(BandType type, String groupName) {
        BandType.PredefinedEntry[] entries = type.getPredefinedEntries();
        for (BandType.PredefinedEntry entry : entries) {
            if (groupName.equals(entry.name()))
                return entry.order();
        }
        return 0;
    }

    private record CatalogEndpoint(String groupName, String server) {}

    private record Catalog(Map<String, Dataset> datasetsByParameter, BandDataset[] datasets,
                           Map<String, List<BandType>> predefinedGroups) {}

    private record Dataset(HapiVersion version, String title, String requestUrl, HapiParam timeParameter,
                           List<DatasetParameter> parameters, long start, long stop) {}

    private record DatasetParameter(BandType type, HapiParam hapiParameter) {}

    private record RequestSchema(String url, HapiTableReader tableReader, List<BandDecoder> decoders) {}

    private record BandDecoder(BandType type, int valueColumn) {}

    private static Catalog getCatalog(String server) throws Exception {
        String urlCatalog = server + "catalog";
        String urlInfo = server + "info";
        String urlData = server + "data";

        JSONObject joCatalog = verifyResponse(JSONUtils.get(new URI(urlCatalog)));
        HapiVersion version = HapiVersion.fromText(joCatalog.optString("HAPI", null));

        JSONArray jaCatalog = joCatalog.optJSONArray("catalog");
        if (jaCatalog == null)
            throw new Exception("Missing catalog object");

        int numIds = jaCatalog.length();
        List<JSONObject> ids = new ArrayList<>(numIds);
        for (Object o : jaCatalog) {
            if (o instanceof JSONObject jo)
                ids.add(jo);
        }

        List<Dataset> datasets = ids.parallelStream().map(item -> {
                    String id = item.optString("id", null);
                    if (id == null)
                        return null;
                    String title = item.optString("title", id);

                    UriTemplate.Variables vars = UriTemplate.vars().set(version.getDatasetRequestParam(), id);
                    String uri = new UriTemplate(urlInfo).expand(vars);
                    try {
                        JSONObject joInfo = verifyResponse(JSONUtils.get(new URI(uri)));
                        return getDataset(version, urlData, id, title, joInfo);
                    } catch (Exception e) {
                        Log.error(uri, e);
                    }
                    return null;
                }).filter(Objects::nonNull)
                .filter(dataset -> !dataset.parameters.isEmpty())
                .toList();
        if (datasets.isEmpty())
            throw new Exception("Empty catalog");

        LinkedHashMap<String, Dataset> datasetsByParameter = new LinkedHashMap<>();
        for (Dataset dataset : datasets) {
            for (DatasetParameter parameter : dataset.parameters)
                datasetsByParameter.put(parameter.type.getBaseUrl(), dataset);
        }
        if (datasetsByParameter.isEmpty())
            throw new Exception("Catalog contains no supported parameters");

        BandType[] typeArray = datasets.stream()
                .flatMap(dataset -> dataset.parameters.stream())
                .map(DatasetParameter::type)
                .toArray(BandType[]::new);
        BandDataset[] datasetArray = datasets.stream()
                .map(dataset -> new BandDataset(dataset.title,
                        dataset.parameters.stream().map(DatasetParameter::type).toList()))
                .toArray(BandDataset[]::new);
        return new Catalog(datasetsByParameter, datasetArray, createPredefinedGroups(typeArray));
    }

    private static Dataset getDataset(HapiVersion version, String urlData, String id, String title, JSONObject jo) throws Exception {
        long start = TimeUtils.MINIMAL_TIME.milli;
        long stop = TimeUtils.MAXIMAL_TIME.milli;
        String startDate = jo.optString("startDate", null);
        String stopDate = jo.optString("stopDate", null);
        if (startDate != null && stopDate != null) {
            start = Math.max(start, toMillis(startDate));
            stop = Math.min(stop, toMillis(stopDate));
        }

        HapiParam[] params = getParameters(jo);
        JSONArray jaParameters = jo.getJSONArray("parameters");

        List<DatasetParameter> parameters = new ArrayList<>(params.length - 1);
        for (int i = 1; i < params.length; i++) {
            HapiParam valueParam = params[i];
            if (isUnsupportedValueParameter(valueParam))
                continue;

            String name = valueParam.getName();
            if (name == null || name.isEmpty())
                continue;

            JSONObject joParameter = jaParameters.getJSONObject(i);
            UriTemplate.Variables request = UriTemplate.vars()
                    .set(version.getDatasetRequestParam(), id)
                    .set("format", hapiFormat)
                    .set("parameters", name);
            String baseUrl = new UriTemplate(urlData).expand(request);
            BandType type = createBandType(baseUrl, id, title, joParameter, valueParam);
            parameters.add(new DatasetParameter(type, valueParam));
        }

        UriTemplate.Variables datasetRequest = UriTemplate.vars()
                .set(version.getDatasetRequestParam(), id)
                .set("format", hapiFormat);
        String requestUrl = new UriTemplate(urlData).expand(datasetRequest);
        return new Dataset(version, title, requestUrl, params[0], parameters, start, stop);
    }

    private static RequestSchema createRequestSchema(Dataset dataset, List<DatasetParameter> parameters) {
        List<HapiParam> hapiParameters = new ArrayList<>(parameters.size() + 1);
        hapiParameters.add(dataset.timeParameter);
        List<BandDecoder> decoders = new ArrayList<>(parameters.size());
        List<String> parameterNames = new ArrayList<>(parameters.size());
        int valueColumn = ParamReader.createReader(dataset.timeParameter).getColumnCount();
        for (DatasetParameter parameter : parameters) {
            decoders.add(new BandDecoder(parameter.type, valueColumn));
            hapiParameters.add(parameter.hapiParameter);
            parameterNames.add(parameter.hapiParameter.getName());
            valueColumn += ParamReader.createReader(parameter.hapiParameter).getColumnCount();
        }

        String url = dataset.requestUrl + UriTemplate.vars()
                .set("parameters", String.join(",", parameterNames))
                .expand("");
        return new RequestSchema(url, new HapiTableReader(hapiParameters.toArray(HapiParam[]::new)), decoders);
    }

    private static HapiParam[] getParameters(JSONObject jo) throws Exception {
        if (jo.optJSONArray("parameters") == null)
            throw new Exception("Missing parameters object");

        HapiParam[] params = HapiInfo.fromJson(jo).getParameters();
        if (params.length < 2)
            throw new Exception("At least two parameters should be present");
        if (!"time".equalsIgnoreCase(params[0].getName()))
            throw new Exception("First parameter should be time");
        if (params[0].getType() != HapiType.ISOTIME)
            throw new Exception("Time parameter should have type isotime");
        return params;
    }

    private static boolean isUnsupportedValueParameter(HapiParam param) {
        HapiType<?, ?> type = param.getType();
        return (type != HapiType.DOUBLE && type != HapiType.INTEGER)
                || param.getSize() != null
                || param.getBins() != null;
    }

    private static String getUnit(HapiParam param) {
        String[] units = param.getUnits();
        return units == null || units.length == 0 || units[0] == null ? "unknown" : units[0];
    }

    private static BandType createBandType(String baseUrl, @Nullable String id, @Nullable String title,
                                           JSONObject joParameter, HapiParam param) {
        String name = Objects.requireNonNullElse(param.getName(), "unknown");
        JSONObject options = getBandOptions(joParameter.optJSONObject("jhvparams")).
                put("baseUrl", baseUrl).
                put("unitLabel", getUnit(param)).
                put("name", id == null ? name : id + ' ' + name).
                put("label", title == null ? name : title + ' ' + name);
        return new BandType(options);
    }

    private static JSONObject getBandOptions(@Nullable JSONObject jhvparams) {
        if (jhvparams == null)
            return new JSONObject();

        JSONObject options = new JSONObject(jhvparams, "scale", "range", "plotType", "barWidth", "levels", "warningLevels");
        options.putOpt("predefined", jhvparams.optJSONArray("predefined", jhvparams.optJSONArray("groups")));
        return options;
    }

    private static List<BandData> getHapiStream(Dataset dataset, RequestSchema schema,
                                                long startTime, long endTime) throws Exception {
        startTime = Math.max(startTime, dataset.start);
        endTime = Math.min(endTime, dataset.stop);
        if (endTime <= startTime)
            return List.of();

        String start = TimeUtils.formatZ(startTime);
        String stop = TimeUtils.formatZ(endTime);

        HapiVersion version = dataset.version;
        UriTemplate.Variables request = UriTemplate.vars()
                .set(version.getStartRequestParam(), start)
                .set(version.getStopRequestParam(), stop);
        String uri = schema.url + request.expand("");

        try (NetClient nc = NetClient.of(new URI(uri), false, NetClient.NetCache.NETWORK)) {
            return readBands(schema.decoders, schema.tableReader, nc.getStream(), null, hapiFormat);
        }
    }

    private static List<BandData> getHapiLocalCSV(DataUri dataUri) throws Exception {
        URI uri = dataUri.uri();
        try (NetClient nc = NetClient.of(uri)) {
            InputStream in = nc.getStream();
            int[] overread1 = new int[1];

            String jsonText = HapiInfo.readCommentedText(in, overread1);
            if (overread1[0] == -1)
                throw new Exception("Could not read HAPI info from " + uri);
            JSONObject jo = new JSONObject(jsonText);
            String fmt = jo.optString("format", "csv");
            HapiParam[] params = getParameters(jo);
            int parameterIndex = 1;
            while (parameterIndex < params.length && isUnsupportedValueParameter(params[parameterIndex]))
                parameterIndex++;
            if (parameterIndex == params.length)
                throw new Exception("No numeric scalar HAPI parameters");

            JSONObject joParameter = jo.getJSONArray("parameters").getJSONObject(parameterIndex);
            BandType type = createBandType(uri.toString(), null, null, joParameter, params[parameterIndex]);
            int valueColumn = 0;
            for (int i = 0; i < parameterIndex; i++)
                valueColumn += ParamReader.createReader(params[i]).getColumnCount();
            BandDecoder decoder = new BandDecoder(type, valueColumn);

            return readBands(List.of(decoder), new HapiTableReader(params), in, (byte) overread1[0], fmt);
        }
    }

    private static List<BandData> readBands(List<BandDecoder> decoders, HapiTableReader tableReader,
                                            InputStream in, Byte byte0, String fmt) throws Exception {
        List<Long> dateList = new ArrayList<>();
        List<List<Float>> valueLists = new ArrayList<>(decoders.size());
        decoders.forEach(ignored -> valueLists.add(new ArrayList<>()));
        try (RowSequence rseq = tableReader.createRowSequence(in, byte0, fmt)) {
            while (rseq.next()) {
                String time = (String) rseq.getCell(0);
                if (time == null) // fill
                    continue;
                dateList.add(toMillis(time));

                for (int i = 0; i < decoders.size(); i++) {
                    Number value = (Number) rseq.getCell(decoders.get(i).valueColumn);
                    float f = value == null ? YAxis.BLANK : value.floatValue();
                    valueLists.get(i).add(Float.isFinite(f) ? f : YAxis.BLANK); // fill
                }
            }
        }

        int numPoints = dateList.size();
        if (numPoints == 0) // empty
            return List.of();

        long[] dates = longArray(numPoints, dateList);
        float[][] values = new float[decoders.size()][];
        for (int i = 0; i < decoders.size(); i++) {
            values[i] = floatArray(numPoints, valueLists.get(i));
        }

        DatesValues raw = new DatesValues(dates, values);
        DatesValues rebinned = decoders.stream().anyMatch(decoder -> !decoder.type.isBarPlot())
                ? raw.rebin()
                : raw;
        List<BandData> result = new ArrayList<>(decoders.size());
        for (int i = 0; i < decoders.size(); i++) {
            DatesValues data = decoders.get(i).type.isBarPlot() ? raw : rebinned;
            result.add(new BandData(decoders.get(i).type, data.dates(), data.values()[i]));
        }
        return result;
    }

    private static long[] longArray(int numPoints, List<Long> dateList) {
        long[] ret = new long[numPoints];
        for (int j = 0; j < numPoints; j++)
            ret[j] = dateList.get(j);
        return ret;
    }

    private static float[] floatArray(int numPoints, List<Float> valueList) {
        float[] ret = new float[numPoints];
        for (int j = 0; j < numPoints; j++)
            ret[j] = valueList.get(j);
        return ret;
    }

    private static JSONObject verifyResponse(JSONObject jo) throws Exception {
        JSONObject status = jo.optJSONObject("status");
        if (status == null)
            throw new Exception("Malformed HAPI status: " + jo);
        if (1200 != status.optInt("code", -1) || !"OK".equals(status.optString("message", null)))
            throw new Exception("HAPI status not OK: " + status);
        return jo;
    }

    private static List<BandData> getHapiUri(URI uri) throws Exception { // tbd
        DataUri dataUri = NetFileCache.get(uri);
        return switch (dataUri.format()) {
            case DataUri.Format.Image.ZIP -> loadZIP(dataUri);
            case DataUri.Format.Timeline.CSV -> getHapiLocalCSV(dataUri);
            default -> throw new Exception("Unknown image type");
        };
    }

    private static List<BandData> loadZIP(DataUri dataUri) throws Exception {
        List<URI> uriList = FileUtils.unZip(dataUri.uri());
        if (uriList.size() != 1)
            throw new Exception("Only one CSV file per zip supported");
        return getHapiUri(uriList.getFirst());
    }

    private static long toMillis(String isoTime) throws Exception {
        double seconds = Times.isoToUnixSeconds(isoTime);
        if (Double.isFinite(seconds)) {
            return (long) (seconds * 1000 + 0.5);
        } else {
            throw new Exception("Could not parse ISO-8601 string: " + isoTime);
        }
    }

}
