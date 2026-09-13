package org.helioviewer.jhv.timelines.band;

import java.io.InputStream;
import java.net.URI;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.thread.AppThread;
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

public final class BandReaderHapi {

    public record CatalogData(Map<String, BandDataset[]> datasets,
                              Map<String, List<BandType>> predefinedGroups) {}

    // Separate catalog and data pools keep catalog requests from delaying timeline downloads.
    static final int REQUEST_THREADS = 8;

    private static final String hapiFormat = "binary";
    private static final LinkedHashMap<String, Catalog> catalogs = new LinkedHashMap<>();
    private static final LatestWorker<Map<String, Catalog>> catalogWorker = new LatestWorker<>("HAPI-Catalog");

    public static void requestCatalog(Consumer<CatalogData> listener) {
        Map<String, String> servers = DataSources.getHapiServers();
        catalogWorker.submit(() -> loadCatalogs(servers), (loaded, fresh) -> {
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

    static Callable<List<BandData>> dataRequest(Map<BandType, Boolean> resolutions, long start, long end) {
        if (resolutions.isEmpty())
            throw new IllegalArgumentException("No HAPI parameters requested");

        Dataset dataset = findDataset(resolutions.keySet().iterator().next().getBaseUrl());
        List<DatasetParameter> parameters = dataset.parameters.stream()
                .filter(parameter -> resolutions.containsKey(parameter.type))
                .toList();
        if (parameters.size() != resolutions.size())
            throw new IllegalArgumentException("HAPI parameters do not belong to one dataset");

        RequestSchema schema = createRequestSchema(dataset, parameters, resolutions);
        return () -> readRemoteData(dataset, schema, start, end);
    }

    record DatasetRef(String key, String title) {}

    private static Map<String, Catalog> loadCatalogs(Map<String, String> servers) throws InterruptedException {
        try (ExecutorService requests = AppThread.createIdleExecutor("HAPI-CatalogRequest", REQUEST_THREADS)) {
            Map<String, Catalog> loaded = new LinkedHashMap<>();
            for (Map.Entry<String, String> server : servers.entrySet()) {
                Catalog catalog = null;
                try {
                    catalog = loadCatalog(server.getValue(), requests);
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    Log.error(server.getValue(), e);
                }
                loaded.put(server.getKey(), catalog);
            }
            return loaded;
        }
    }

    private static void onSuccessCatalogs(Map<String, Catalog> loaded, Consumer<CatalogData> listener) {
        catalogs.clear();
        LinkedHashMap<String, BandDataset[]> datasets = new LinkedHashMap<>();
        loaded.forEach((name, catalog) -> {
            if (catalog != null)
                catalogs.put(name, catalog);
            datasets.put(name, catalog == null ? new BandDataset[0] : catalog.datasets);
        });
        listener.accept(new CatalogData(Collections.unmodifiableMap(datasets), createPredefinedGroups()));
    }

    private static Map<String, List<BandType>> createPredefinedGroups() {
        LinkedHashMap<String, List<BandType>> groups = new LinkedHashMap<>();
        for (Catalog catalog : catalogs.values()) {
            for (BandDataset dataset : catalog.datasets) {
                for (BandType type : dataset.bandTypes()) {
                    for (BandType.PredefinedEntry entry : type.getPredefinedEntries())
                        groups.computeIfAbsent(entry.name(), k -> new ArrayList<>()).add(type);
                }
            }
        }
        groups.replaceAll((name, types) -> {
            types.sort(Comparator.comparingInt(type -> orderFor(type, name)));
            return List.copyOf(types);
        });
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

    private record Catalog(Map<String, Dataset> datasetsByParameter, BandDataset[] datasets) {}

    private record Dataset(HapiVersion version, String title, String requestUrl, HapiParam timeParameter,
                           List<DatasetParameter> parameters, long start, long stop) {}

    private record DatasetParameter(BandType type, HapiParam hapiParameter) {}

    private record RequestSchema(String url, HapiTableReader tableReader, List<BandDecoder> decoders) {}

    private record BandDecoder(BandType type, int valueColumn, boolean rebin) {}

    private static Catalog loadCatalog(String server, ExecutorService requests) throws Exception {
        String urlCatalog = server + "catalog";

        JSONObject joCatalog = verifyResponse(JSONUtils.get(new URI(urlCatalog)));
        HapiVersion version = HapiVersion.fromText(joCatalog.optString("HAPI", null));

        JSONArray jaCatalog = joCatalog.optJSONArray("catalog");
        if (jaCatalog == null)
            throw new Exception("Missing catalog object");

        List<Callable<Dataset>> tasks = new ArrayList<>(jaCatalog.length());
        for (Object value : jaCatalog) {
            if (value instanceof JSONObject item)
                tasks.add(() -> loadDataset(server, version, item));
        }

        // invokeAll preserves submission order and cancels unfinished tasks on interruption.
        List<Dataset> datasets = new ArrayList<>();
        for (Future<Dataset> result : requests.invokeAll(tasks)) {
            Dataset dataset = result.get();
            if (dataset != null && !dataset.parameters.isEmpty())
                datasets.add(dataset);
        }
        if (datasets.isEmpty())
            throw new Exception("Empty catalog");

        LinkedHashMap<String, Dataset> datasetsByParameter = new LinkedHashMap<>();
        List<BandDataset> catalogDatasets = new ArrayList<>(datasets.size());
        for (Dataset dataset : datasets) {
            List<BandType> datasetTypes = new ArrayList<>(dataset.parameters.size());
            for (DatasetParameter parameter : dataset.parameters) {
                datasetsByParameter.put(parameter.type.getBaseUrl(), dataset);
                datasetTypes.add(parameter.type);
            }
            catalogDatasets.add(new BandDataset(dataset.title, datasetTypes));
        }
        return new Catalog(datasetsByParameter, catalogDatasets.toArray(BandDataset[]::new));
    }

    @Nullable
    private static Dataset loadDataset(String server, HapiVersion version, JSONObject item) {
        String id = item.optString("id", null);
        if (id == null)
            return null;
        String title = item.optString("title", id);
        UriTemplate.Variables vars = UriTemplate.vars().set(version.getDatasetRequestParam(), id);
        String uri = new UriTemplate(server + "info").expand(vars);
        try {
            JSONObject info = verifyResponse(JSONUtils.get(new URI(uri)));
            return parseDataset(version, server + "data", id, title, info);
        } catch (Exception e) {
            Log.error(uri, e);
            return null;
        }
    }

    private static Dataset parseDataset(HapiVersion version, String urlData, String id, String title, JSONObject jo) throws Exception {
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

    private static RequestSchema createRequestSchema(Dataset dataset, List<DatasetParameter> parameters, Map<BandType, Boolean> resolutions) {
        List<HapiParam> hapiParameters = new ArrayList<>(parameters.size() + 1);
        hapiParameters.add(dataset.timeParameter);
        List<BandDecoder> decoders = new ArrayList<>(parameters.size());
        List<String> parameterNames = new ArrayList<>(parameters.size());
        int valueColumn = ParamReader.createReader(dataset.timeParameter).getColumnCount();
        for (DatasetParameter parameter : parameters) {
            decoders.add(new BandDecoder(parameter.type, valueColumn, !parameter.type.isBarPlot() && !resolutions.get(parameter.type)));
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
        JSONObject jhvparams = joParameter.optJSONObject("jhvparams");
        JSONObject options = jhvparams == null ? new JSONObject() :
                new JSONObject(jhvparams, "scale", "range", "plotType", "barWidth", "levels", "warningLevels", "groups", "predefined");
        options.put("baseUrl", baseUrl).
                put("unitLabel", getUnit(param)).
                put("name", id == null ? name : id + ' ' + name).
                put("label", title == null ? name : title + ' ' + name);
        return new BandType(options);
    }

    private static List<BandData> readRemoteData(Dataset dataset, RequestSchema schema,
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

    static List<BandData> readUri(URI uri) throws Exception {
        DataUri dataUri = NetFileCache.get(uri);
        return switch (dataUri.format()) {
            case ZIP -> readZip(dataUri);
            case CSV -> readCsv(dataUri);
            default -> throw new Exception("Unsupported HAPI data format: " + dataUri.format());
        };
    }

    private static List<BandData> readZip(DataUri dataUri) throws Exception {
        List<URI> uriList = FileUtils.unZip(dataUri.uri());
        if (uriList.size() != 1)
            throw new Exception("Only one CSV file per zip supported");
        return readUri(uriList.getFirst());
    }

    private static List<BandData> readCsv(DataUri dataUri) throws Exception {
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
            BandDecoder decoder = new BandDecoder(type, valueColumn, !type.isBarPlot());

            return readBands(List.of(decoder), new HapiTableReader(params), in, (byte) overread1[0], fmt);
        }
    }

    private static List<BandData> readBands(List<BandDecoder> decoders, HapiTableReader tableReader,
                                            InputStream in, Byte byte0, String fmt) throws Exception {
        int numPoints = 0;
        long[] dates = new long[(int) (TimeUtils.DAY_IN_MILLIS / TimeUtils.MINUTE_IN_MILLIS)];
        float[][] values = new float[decoders.size()][dates.length];
        try (RowSequence rseq = tableReader.createRowSequence(in, byte0, fmt)) {
            while (rseq.next()) {
                String time = (String) rseq.getCell(0);
                if (time == null) // fill
                    continue;

                if (numPoints == dates.length) {
                    dates = Arrays.copyOf(dates, dates.length * 2);
                    for (int i = 0; i < values.length; i++)
                        values[i] = Arrays.copyOf(values[i], dates.length);
                }
                dates[numPoints] = toMillis(time);

                for (int i = 0; i < decoders.size(); i++) {
                    Number value = (Number) rseq.getCell(decoders.get(i).valueColumn);
                    float f = value == null ? YAxis.BLANK : value.floatValue();
                    values[i][numPoints] = Float.isFinite(f) ? f : YAxis.BLANK; // fill
                }
                numPoints++;
            }
        }

        if (numPoints == 0) // empty
            return List.of();

        if (numPoints != dates.length) {
            dates = Arrays.copyOf(dates, numPoints);
            for (int i = 0; i < values.length; i++)
                values[i] = Arrays.copyOf(values[i], numPoints);
        }

        DatesValues raw = new DatesValues(dates, values);
        DatesValues rebinned = decoders.stream().anyMatch(BandDecoder::rebin)
                ? raw.rebin()
                : raw;
        List<BandData> result = new ArrayList<>(decoders.size());
        for (int i = 0; i < decoders.size(); i++) {
            DatesValues data = decoders.get(i).rebin ? rebinned : raw;
            result.add(new BandData(decoders.get(i).type, data.dates(), data.values()[i]));
        }
        return result;
    }

    private static JSONObject verifyResponse(JSONObject jo) throws Exception {
        JSONObject status = jo.optJSONObject("status");
        if (status == null)
            throw new Exception("Malformed HAPI status: " + jo);
        if (1200 != status.optInt("code", -1) || !"OK".equals(status.optString("message", null)))
            throw new Exception("HAPI status not OK: " + status);
        return jo;
    }

    private static long toMillis(String isoTime) throws Exception {
        if (isCalendarTimestamp(isoTime)) {
            int year = Integer.parseInt(isoTime, 0, 4, 10);
            int month = Integer.parseInt(isoTime, 5, 7, 10);
            int day = Integer.parseInt(isoTime, 8, 10, 10);
            int hour = Integer.parseInt(isoTime, 11, 13, 10);
            int minute = Integer.parseInt(isoTime, 14, 16, 10);
            int second = Integer.parseInt(isoTime, 17, 19, 10);
            // Keep STIL's rounding before the epoch and its handling of nonstandard clock values.
            if (year >= 1970 && hour < 24 && minute < 60 && second < 60) {
                try {
                    long epochDay = LocalDate.of(year, month, day).toEpochDay();
                    int millis = isoTime.length() == 24 ? Integer.parseInt(isoTime, 20, 23, 10) : 0;
                    return epochDay * 86_400_000 + (hour * 3600L + minute * 60L + second) * 1000 + millis;
                } catch (DateTimeException e) {
                    // Let STIL handle calendar values outside LocalDate's valid range.
                }
            }
        }
        // Catalog bounds can be shortened dates; retain support for STIL's other accepted forms.
        double seconds = Times.isoToUnixSeconds(isoTime);
        if (Double.isFinite(seconds)) {
            return (long) (seconds * 1000 + 0.5);
        } else {
            throw new Exception("Could not parse ISO-8601 string: " + isoTime);
        }
    }

    private static boolean isCalendarTimestamp(String text) {
        int length = text.length();
        if ((length != 20 && length != 24) || text.charAt(length - 1) != 'Z'
                || (length == 24 && text.charAt(19) != '.') || text.charAt(4) != '-' || text.charAt(7) != '-'
                || text.charAt(10) != 'T' || text.charAt(13) != ':' || text.charAt(16) != ':')
            return false;
        for (int i = 0; i < length - 1; i++) {
            if (i == 4 || i == 7 || i == 10 || i == 13 || i == 16 || i == 19)
                continue;
            if (text.charAt(i) < '0' || text.charAt(i) > '9')
                return false;
        }
        return true;
    }

    private BandReaderHapi() {}

}
