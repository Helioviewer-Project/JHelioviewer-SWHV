package org.helioviewer.jhv.timelines.band;

import java.io.PushbackInputStream;
import java.net.URI;
import java.util.ArrayList;
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
import uk.ac.starlink.hapi.Times;
import uk.ac.starlink.table.RowSequence;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;

public class BandReaderHapi {

    private static final String groupName = "HAPI";
    private static final String hapiFormat = "binary";
    private static final String ROBserver = "https://hapi.swhv.oma.be/SWHV_Timelines/hapi/";

    private static Catalog theCatalog; //!

    public static void requestCatalog() {
        EDTCallbackExecutor.pool.submit(new LoadHapiCatalog(ROBserver), new CallbackCatalog());
    }

    static Future<Band.Data> requestData(String url, long start, long end) {
        return EDTCallbackExecutor.pool.submit(new LoadHapiStream(theCatalog, url, start, end), new CallbackData());
    }

    public static void loadUri(URI uri) {
        EDTCallbackExecutor.pool.submit(new LoadHapiUri(uri), new CallbackData());
    }

    private record Catalog(HapiVersion version, Map<String, BandParameter> parameters, BandType[] types) {
    }

    private record Dataset(String id, List<BandReader> readers, long start, long stop) {
    }

    private record BandParameter(BandReader reader, long start, long stop) {
    }

    private record BandReader(BandType type, HapiTableReader tableReader) {
    }

    private record Parameter(String name, String units, String scale, JSONArray range) {
    }

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
        return new Catalog(version, parameters, types.toArray(BandType[]::new));
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

        HapiParam[] typeParams = new HapiParam[2];
        typeParams[0] = params[0];

        int numAxes = parameters.size() - 1;
        List<BandReader> readers = new ArrayList<>(numAxes);
        for (int i = 1; i <= numAxes; i++) {
            Parameter p = parameters.get(i);
            UriTemplate.Variables request = UriTemplate.vars()
                    .set(version.getDatasetRequestParam(), id)
                    .set("format", hapiFormat)
                    .set("parameters", p.name);
            JSONObject jobt = new JSONObject().
                    put("baseUrl", new UriTemplate(urlData).expand(request)).
                    put("unitLabel", p.units).
                    put("name", id + ' ' + p.name).
                    put("range", p.range).
                    put("scale", p.scale).
                    put("label", title + ' ' + p.name);

            typeParams[1] = params[i];
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

        String scale = null;
        JSONArray range = null;
        JSONObject jhvparams = jo.optJSONObject("jhvparams");
        if (jhvparams != null) {
            scale = jhvparams.optString("scale", null);
            range = jhvparams.optJSONArray("range");
        }

        return new Parameter(name, units, scale, range);
    }

    private static Band.Data getHapiStream(Catalog catalog, String baseUrl, long startTime, long endTime) throws Exception {
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
            RowSequence sequence = parameter.reader.tableReader.createRowSequence(nc.getStream(), null, hapiFormat);

            ArrayList<Long> dateList = new ArrayList<>();
            ArrayList<Float> valueList = new ArrayList<>();
            while (sequence.next()) {
                String time = (String) sequence.getCell(0);
                if (time == null) // fill
                    continue;
                dateList.add(toMillis(time));

                Number value = (Number) sequence.getCell(1);
                float f = value == null ? YAxis.BLANK : value.floatValue();
                valueList.add(Float.isFinite(f) ? f : YAxis.BLANK); // fill
            }
            int numPoints = dateList.size();
            if (numPoints == 0) // empty
                return null;

            long[] dates = longArray(numPoints, dateList);
            float[] values = floatArray(numPoints, valueList);
            DatesValues dvs = new DatesValues(dates, new float[][]{values}).rebin();

            return new Band.Data(parameter.reader.type, dvs.dates(), dvs.values()[0]);
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

    private static Band.Data getHapiUri(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri); PushbackInputStream pis = new PushbackInputStream(nc.getStream())) {
            String uriString = uri.toString();
            int[] overread1 = new int[1];
            String jsonText = HapiInfo.readCommentedText(pis, overread1);
            if (overread1[0] == -1)
                throw new Exception("Could not read HAPI info from " + uriString);
            pis.unread(overread1[0]);

            JSONObject jo = new JSONObject(jsonText);
            Dataset dataset = getDataset(HapiVersion.ASSUMED, uriString, uriString, uriString, jo);

            List<Long> dateList = new ArrayList<>();
            List<Float> valueList = new ArrayList<>();

            BandReader reader = dataset.readers.get(0);
            HapiTableReader tableReader = reader.tableReader;
            RowSequence rseq = tableReader.createRowSequence(pis, null, "csv");

            try {
                while ( rseq.next() ) {
                    String time = (String) rseq.getCell(0);
                    if (time == null) // fill
                        continue;
                    dateList.add(toMillis(time));
    
                    Number value = (Number) rseq.getCell(1);
                    float f = value == null ? YAxis.BLANK : value.floatValue();
                    valueList.add(Float.isFinite(f) ? f : YAxis.BLANK); // fill
                }
            }
            finally {
                rseq.close();
            }

            int numPoints = dateList.size();
            if (numPoints == 0) // empty
                return null;

            long[] dates = longArray(numPoints, dateList);
            float[] values = floatArray(numPoints, valueList);
            DatesValues dvs = new DatesValues(dates, new float[][]{values}).rebin();

            return new Band.Data(reader.type, dvs.dates(), dvs.values()[0]);
        }
    }

    private record LoadHapiCatalog(String server) implements Callable<Catalog> {
        @Override
        public Catalog call() throws Exception {
            return getCatalog(server);
        }
    }

    private record LoadHapiStream(Catalog catalog, String url, long start, long end) implements Callable<Band.Data> {
        @Override
        public Band.Data call() throws Exception {
            return getHapiStream(catalog, url, start, end);
        }
    }

    private record LoadHapiUri(URI uri) implements Callable<Band.Data> {
        @Override
        public Band.Data call() throws Exception {
            return getHapiUri(uri);
        }
    }

    private static class CallbackData implements FutureCallback<Band.Data> {
        @Override
        public void onSuccess(Band.Data line) {
            if (line != null) {
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
            Timelines.td.setupDatasets(groupName, theCatalog.types);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(Throwables.getStackTraceAsString(t));
        }
    }

    private static long toMillis(String isoTime) throws Exception {
        double seconds = Times.isoToUnixSeconds(isoTime);
        if (Double.isFinite(seconds)) {
            return (long) (seconds * 1000 + 0.0005);
        } else {
            throw new Exception("Could not parse ISO-8601 string: " + isoTime);
        }
    }

}
