package org.helioviewer.jhv.timelines.band;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.ac.starlink.hapi.HapiInfo;
import uk.ac.starlink.hapi.HapiParam;
import uk.ac.starlink.hapi.HapiTableReader;
import uk.ac.starlink.hapi.HapiVersion;
import uk.ac.starlink.hapi.Times;
import uk.ac.starlink.table.RowSequence;

public class BandReaderHapi {

    @FunctionalInterface
    public interface CatalogListener {
        void catalogsLoaded(Map<String, BandType[]> catalogs);
    }

    private static final String hapiFormat = "binary";
    private static final CatalogEndpoint[] catalogEndpoints = {
            new CatalogEndpoint("ROB", "https://hapi.swhv.oma.be/SWHV_Timelines/hapi/"),
            //new CatalogEndpoint("ROB Test", "http://swhv-test:4000/hapi/")
    };

    private static final HashMap<CatalogEndpoint, Catalog> catalogs = new HashMap<>();
    private static Future<?> catalogRequest;

    public static String[] getCatalogGroups() {
        String[] groups = new String[catalogEndpoints.length];
        for (int i = 0; i < catalogEndpoints.length; i++)
            groups[i] = catalogEndpoints[i].groupName;
        return groups;
    }

    public static void requestCatalog(CatalogListener listener) {
        if (catalogRequest != null)
            catalogRequest.cancel(true);
        catalogRequest = Task.submit(BandReaderHapi::loadCatalogs,
                loaded -> onSuccessCatalogs(loaded, listener), BandReaderHapi::onFailure);
    }

    static boolean hasCatalog(String url) {
        return findCatalog(url) != null;
    }

    static Callable<BandData> dataRequest(String url, long start, long end) {
        return new LoadHapiStream(findCatalog(url), url, start, end);
    }

    static BandData readUri(URI uri) throws Exception {
        return getHapiUri(uri);
    }

    private record LoadHapiStream(Catalog catalog, String url, long start, long end) implements Callable<BandData> {
        @Override
        public BandData call() throws Exception {
            return getHapiStream(catalog, url, start, end);
        }
    }

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
        LinkedHashMap<String, BandType[]> bandTypes = new LinkedHashMap<>();
        for (int i = 0; i < loadedCatalogs.length; i++) {
            Catalog catalog = loadedCatalogs[i];
            if (catalog != null) {
                CatalogEndpoint endpoint = catalogEndpoints[i];
                catalogs.put(endpoint, catalog);
                bandTypes.put(endpoint.groupName, catalog.types);
            }
        }
        listener.catalogsLoaded(bandTypes);
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
            if (catalog.parameters.containsKey(baseUrl))
                return catalog;
        }
        return null;
    }

    private static int orderFor(BandType type, String groupName) {
        BandType.PredefinedEntry[] entries = type.getPredefinedEntries();
        for (BandType.PredefinedEntry entry : entries) {
            if (groupName.equals(entry.name()))
                return entry.order();
        }
        return 0;
    }

    private static void onFailure(Throwable t) {
        if (!(t instanceof CancellationException))
            Log.errorStack(t);
    }

    private record CatalogEndpoint(String groupName, String server) {}

    private record Catalog(HapiVersion version, Map<String, BandParameter> parameters, BandType[] types,
                           Map<String, List<BandType>> predefinedGroups) {}

    private record Dataset(String id, List<BandReader> readers, long start, long stop) {}

    private record BandParameter(BandReader reader, long start, long stop) {}

    private record BandReader(BandType type, HapiTableReader tableReader) {}

    private record Parameter(String name, String units, JSONObject jhvparams) {}

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
        }).filter(Objects::nonNull).toList();
        if (datasets.isEmpty())
            throw new Exception("Empty catalog");

        LinkedHashMap<String, BandParameter> parameters = new LinkedHashMap<>();
        for (Dataset dataset : datasets) {
            long start = dataset.start;
            long stop = dataset.stop;
            for (BandReader reader : dataset.readers) {
                parameters.put(reader.type.getBaseUrl(), new BandParameter(reader, start, stop));
            }
        }
        ArrayList<BandType> types = new ArrayList<>();
        parameters.values().forEach(parameter -> types.add(parameter.reader.type));
        BandType[] typeArray = types.toArray(BandType[]::new);
        return new Catalog(version, parameters, typeArray, createPredefinedGroups(typeArray));
    }

    private static Dataset getDataset(HapiVersion version, String urlData, @Nullable String id, @Nullable String title, JSONObject jo) throws Exception {
        long start = TimeUtils.MINIMAL_TIME.milli;
        long stop = TimeUtils.MAXIMAL_TIME.milli;
        String startDate = jo.optString("startDate", null);
        String stopDate = jo.optString("stopDate", null);
        if (startDate != null && stopDate != null) {
            start = Math.max(start, toMillis(startDate));
            stop = Math.min(stop, toMillis(stopDate));
        }

        JSONArray jaParameters = jo.optJSONArray("parameters");
        if (jaParameters == null)
            throw new Exception("Missing parameters object");

        int numParameters = jaParameters.length();
        List<Parameter> parameters = new ArrayList<>(numParameters);
        for (int j = 0; j < numParameters; j++) {
            JSONObject joParameter = jaParameters.optJSONObject(j);
            if (joParameter == null)
                continue;
            parameters.add(getParameter(joParameter));
        }
        if (parameters.size() < 2)
            throw new Exception("At least two parameters should be present");
        if (!"time".equalsIgnoreCase(parameters.getFirst().name))
            throw new Exception("First parameter should be time");

        HapiInfo info = HapiInfo.fromJson(jo);
        HapiParam[] params = info.getParameters();

        int numAxes = parameters.size() - 1;
        List<BandReader> readers = new ArrayList<>(numAxes);
        for (int i = 1; i <= numAxes; i++) {
            Parameter p = parameters.get(i);
            UriTemplate.Variables request = UriTemplate.vars();
            if (id != null)
                request.set(version.getDatasetRequestParam(), id)
                        .set("format", hapiFormat)
                        .set("parameters", p.name);
            JSONObject jobt = getBandOptions(p.jhvparams).
                    put("baseUrl", new UriTemplate(urlData).expand(request)).
                    put("unitLabel", p.units).
                    put("name", id == null ? p.name : id + ' ' + p.name).
                    put("label", title == null ? p.name : title + ' ' + p.name);

            HapiParam[] typeParams = new HapiParam[]{params[0], params[i]};
            readers.add(new BandReader(new BandType(jobt), new HapiTableReader(typeParams)));
        }
        return new Dataset(id, readers, start, stop);
    }

    private static Parameter getParameter(JSONObject jo) throws Exception {
        if (jo.optJSONArray("bins") != null)
            throw new Exception("Bins not supported");
        if (jo.optJSONArray("size") != null)
            throw new Exception("Only scalars supported");
        String name = jo.optString("name", null);
        name = name == null ? "unknown" : name;
        String units = jo.optString("units", null);
        units = units == null ? "unknown" : units;

        return new Parameter(name, units, jo.optJSONObject("jhvparams"));
    }

    private static JSONObject getBandOptions(@Nullable JSONObject jhvparams) {
        if (jhvparams == null)
            return new JSONObject();

        JSONObject options = new JSONObject(jhvparams,
                "scale", "range", "plotType", "barWidth", "levels", "warningLevels");
        JSONArray predefined = jhvparams.optJSONArray("predefined");
        options.putOpt("predefined", predefined != null ? predefined : jhvparams.optJSONArray("groups"));
        return options;
    }

    private static BandData getHapiStream(Catalog catalog, String baseUrl, long startTime, long endTime) throws Exception {
        if (catalog == null) // we may be offline
            return null;
        BandParameter parameter = catalog.parameters.get(baseUrl);
        if (parameter == null)
            return null;

        startTime = Math.max(startTime, parameter.start);
        endTime = Math.min(endTime, parameter.stop);
        if (endTime <= startTime)
            return null;

        String start = TimeUtils.formatZ(startTime);
        String stop = TimeUtils.formatZ(endTime);

        HapiVersion version = catalog.version;
        UriTemplate.Variables request = UriTemplate.vars()
                .set(version.getStartRequestParam(), start)
                .set(version.getStopRequestParam(), stop);
        String uri = baseUrl + request.expand("");

        try (NetClient nc = NetClient.of(new URI(uri), false, NetClient.NetCache.NETWORK)) {
            return readBand(parameter.reader.type, parameter.reader.tableReader, nc.getStream(), null, hapiFormat);
        }
    }

    private static BandData getHapiLocalCSV(DataUri dataUri) throws Exception {
        URI uri = dataUri.uri();
        try (NetClient nc = NetClient.of(uri)) {
            InputStream in = nc.getStream();
            int[] overread1 = new int[1];

            String jsonText = HapiInfo.readCommentedText(in, overread1);
            if (overread1[0] == -1)
                throw new Exception("Could not read HAPI info from " + uri);
            JSONObject jo = new JSONObject(jsonText);
            String fmt = jo.optString("format", "csv");
            // not catalog, thus no 'id' nor 'title'
            Dataset dataset = getDataset(HapiVersion.ASSUMED, uri.toString(), null, null, jo);
            BandReader reader = dataset.readers.getFirst();

            return readBand(reader.type, reader.tableReader, in, (byte) overread1[0], fmt);
        }
    }

    private static BandData readBand(BandType type, HapiTableReader tableReader, InputStream in, Byte byte0, String fmt) throws Exception {
        List<Long> dateList = new ArrayList<>();
        List<Float> valueList = new ArrayList<>();
        try (RowSequence rseq = tableReader.createRowSequence(in, byte0, fmt)) {
            while (rseq.next()) {
                String time = (String) rseq.getCell(0);
                if (time == null) // fill
                    continue;
                dateList.add(toMillis(time));

                Number value = (Number) rseq.getCell(1);
                float f = value == null ? YAxis.BLANK : value.floatValue();
                valueList.add(Float.isFinite(f) ? f : YAxis.BLANK); // fill
            }
        }

        int numPoints = dateList.size();
        if (numPoints == 0) // empty
            return null;

        long[] dates = longArray(numPoints, dateList);
        float[] values = floatArray(numPoints, valueList);
        DatesValues dvs = type.isBarPlot()
                ? new DatesValues(dates, new float[][]{values})
                : new DatesValues(dates, new float[][]{values}).rebin();

        return new BandData(type, dvs.dates(), dvs.values()[0]);
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

    private static BandData getHapiUri(URI uri) throws Exception { // tbd
        DataUri dataUri = NetFileCache.get(uri);
        return switch (dataUri.format()) {
            case DataUri.Format.Image.ZIP -> loadZIP(dataUri);
            case DataUri.Format.Timeline.CSV -> getHapiLocalCSV(dataUri);
            default -> throw new Exception("Unknown image type");
        };
    }

    private static BandData loadZIP(DataUri dataUri) throws Exception {
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
