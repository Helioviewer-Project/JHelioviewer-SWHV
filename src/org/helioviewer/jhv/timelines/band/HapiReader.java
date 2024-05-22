package org.helioviewer.jhv.timelines.band;

import java.io.BufferedInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.starlink.hapi.HapiInfo;
import uk.ac.starlink.hapi.HapiParam;
import uk.ac.starlink.hapi.HapiTableReader;
import uk.ac.starlink.hapi.HapiVersion;
import uk.ac.starlink.table.RowSequence;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;

public class HapiReader {

    private static final String ROBserver = "https://hapi.swhv.oma.be/SWHV_Timelines/hapi/";

    private static Catalog theCatalog; //!

    public static void requestCatalog() {
        EDTCallbackExecutor.pool.submit(new LoadCatalog(ROBserver), new CallbackCatalog());
    }

    private record DataRequest(String id, long start, long end) {
    }

    private static final HashMap<DataRequest, Future<List<Band.Data>>> requestMap = new HashMap<>();

    // avoid duplication of requests
    static Future<List<Band.Data>> requestData(String id, long start, long end) {
        return requestMap.computeIfAbsent(new DataRequest(id, start, end), r ->
                EDTCallbackExecutor.pool.submit(new LoadData(theCatalog, id, start, end), new CallbackData()));
    }

    private record Catalog(HapiVersion version, Map<String, Dataset> datasets) {
    }

    private record Dataset(String id, HapiTableReader reader, List<BandType> types, long start, long stop) {
    }

    private record Parameter(String name, String units, String scale, JSONArray range) {
    }

    private record LoadCatalog(String server) implements Callable<Catalog> {
        @Override
        public Catalog call() throws Exception {
            JSONObject joCatalog = verifyResponse(JSONUtils.get(new URI(server + "catalog")));
            HapiVersion version = HapiVersion.fromText(joCatalog.optString("HAPI", null));

            JSONArray jaCatalog = joCatalog.optJSONArray("catalog");
            if (jaCatalog == null)
                throw new Exception("Missing catalog object");

            int numIds = jaCatalog.length();
            ArrayList<JSONObject> ids = new ArrayList<>(numIds);
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
                String uri = new UriTemplate(server + "info").expand(vars);
                try {
                    JSONObject joInfo = verifyResponse(JSONUtils.get(new URI(uri)));
                    return getDataset(server, id, title, joInfo);
                } catch (Exception e) {
                    Log.error(uri, e);
                }
                return null;
            }).filter(Objects::nonNull).toList();
            if (datasets.isEmpty())
                throw new Exception("Empty catalog");

            LinkedHashMap<String, Dataset> datasetMap = new LinkedHashMap<>();
            datasets.forEach(d -> datasetMap.put(d.id, d));
            return new Catalog(version, datasetMap);
        }
    }

    private static Dataset getDataset(String server, String id, String title, JSONObject jo) throws Exception {
        long start = TimeUtils.MINIMAL_TIME.milli;
        long stop = TimeUtils.MAXIMAL_TIME.milli;
        String startDate = jo.optString("startDate", null);
        String stopDate = jo.optString("stopDate", null);
        if (startDate != null && stopDate != null) {
            start = Math.max(start, TimeUtils.optParse(startDate.replace("Z", ""), start));
            stop = Math.min(stop, TimeUtils.optParse(stopDate.replace("Z", ""), stop));
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
        if (!"time".equalsIgnoreCase(parameters.get(0).name))
            throw new Exception("First parameter should be time");

        int numAxes = parameters.size() - 1;
        List<BandType> types = new ArrayList<>(numAxes);
        for (int i = 1; i <= numAxes; i++) {
            Parameter p = parameters.get(i);
            JSONObject jobt = new JSONObject().
                    put("baseUrl", (server + "data").intern()).
                    put("unitLabel", p.units).
                    put("name", id + ' ' + p.name).
                    put("range", p.range).
                    put("scale", p.scale).
                    put("label", title + ' ' + p.name).
                    //
                            put("dataset", id).
                    //
                            put("group", "HAPI");
            types.add(new BandType(jobt));
        }

        HapiTableReader reader = new HapiTableReader(HapiInfo.fromJson(jo));
        return new Dataset(id, reader, types, start, stop);
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

        String scale = null;
        JSONArray range = null;
        JSONObject jhvparams = jo.optJSONObject("jhvparams");
        if (jhvparams != null) {
            scale = jhvparams.optString("scale", null);
            range = jhvparams.optJSONArray("range");
        }

        return new Parameter(name, units, scale, range);
    }

    private static final String hapiFormat = "binary";
    private static final List<Band.Data> emptyList = new ArrayList<>();

    private static List<Band.Data> getData(Catalog catalog, String id, long startTime, long endTime) throws Exception {
        Dataset dataset = catalog.datasets.get(id);
        if (dataset == null)
            return emptyList;

        startTime = Math.max(startTime, dataset.start);
        endTime = Math.min(endTime, dataset.stop);
        if (endTime <= startTime)
            return emptyList;

        String start = TimeUtils.formatZ(startTime);
        String stop = TimeUtils.formatZ(endTime);

        HapiVersion version = catalog.version;
        UriTemplate.Variables request = UriTemplate.vars()
                .set(version.getDatasetRequestParam(), id)
                .set("format", hapiFormat)
                .set(version.getStartRequestParam(), start)
                .set(version.getStopRequestParam(), stop);
        String baseUrl = dataset.types.get(0).getBaseURL();
        String uri = new UriTemplate(baseUrl).expand(request);

        try (NetClient nc = NetClient.of(new URI(uri)); BufferedInputStream is = new BufferedInputStream(nc.getStream())) {
            RowSequence rseq = dataset.reader.createRowSequence(is, null, hapiFormat);
            int numAxes = dataset.types.size();

            ArrayList<Long> dateList = new ArrayList<>();
            ArrayList<float[]> valueList = new ArrayList<>();
            while (rseq.next()) {
                String time = (String) rseq.getCell(0);
                if (time == null) // fill
                    continue;
                dateList.add(TimeUtils.parseZ(time));

                float[] valueArray = new float[numAxes];
                for (int i = 0; i < numAxes; i++) {
                    Object o = rseq.getCell(i + 1);
                    valueArray[i] = o == null ? YAxis.BLANK : ((Number) o).floatValue(); // fill
                }
                valueList.add(valueArray);
            }
            int numPoints = dateList.size();
            if (numPoints == 0) // empty
                return emptyList;

            long[] dates = longArray(numPoints, dateList);
            float[][] values = transpose(numPoints, numAxes, valueList);
            List<Band.Data> lines = new ArrayList<>(numAxes);
            for (int i = 0; i < numAxes; i++) {
                lines.add(new Band.Data(dataset.types.get(i), dates, values[i]));
            }
            return lines;
        } catch (Exception e) {
            Log.error(uri, e);
            throw e;
        }
    }

    private static long[] longArray(int numPoints, List<Long> dateList) {
        long[] ret = new long[numPoints];
        for (int j = 0; j < numPoints; j++)
            ret[j] = dateList.get(j);
        return ret;
    }

    private static float[][] transpose(int numPoints, int numAxes, List<float[]> valueList) {
        float[][] ret = new float[numAxes][numPoints];
        for (int j = 0; j < numPoints; j++) {
            float[] v = valueList.get(j);
            for (int i = 0; i < numAxes; i++) {
                ret[i][j] = v[i];
            }
        }
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

    private record LoadData(Catalog catalog, String id, long start, long end) implements Callable<List<Band.Data>> {
        @Override
        public List<Band.Data> call() throws Exception {
            return getData(catalog, id, start, end);
        }
    }

    private static class CallbackData implements FutureCallback<List<Band.Data>> {
        @Override
        public void onSuccess(@Nonnull List<Band.Data> lines) {
            for (Band.Data line : lines) {
                Band band = Band.createFromType(line.bandType());
                band.addToCache(line.values(), line.dates());
                Timelines.getLayers().add(band);
            }
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(Throwables.getStackTraceAsString(t));
        }
    }

    private static class CallbackCatalog implements FutureCallback<Catalog> {
        @Override
        public void onSuccess(@Nonnull Catalog catalog) {
            theCatalog = catalog;

            for (Dataset dataset : theCatalog.datasets.values()) {
                BandType.loadBandTypes(dataset.types);
            }
            Timelines.td.setupDatasets();
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
        }
    }

}
