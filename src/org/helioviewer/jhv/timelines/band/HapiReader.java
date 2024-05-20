package org.helioviewer.jhv.timelines.band;

import java.io.BufferedInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.starlink.hapi.HapiInfo;
import uk.ac.starlink.hapi.HapiParam;
import uk.ac.starlink.hapi.HapiTableReader;
import uk.ac.starlink.hapi.HapiVersion;
import uk.ac.starlink.table.RowSequence;

public class HapiReader {

    private record Catalog(HapiVersion version, Map<String, Dataset> datasets) {
    }

    private record DatasetReader(HapiTableReader hapiReader, List<Parameter> parameters) {
    }

    private record Dataset(String title, DatasetReader reader) {
    }

    private record Parameter(String name, String units, String scale, JSONArray range) {
    }

    public static Catalog getCatalog(String server) throws Exception {
        JSONObject joCatalog = verifyResponse(JSONUtils.get(new URI(server + "catalog")));
        HapiVersion version = HapiVersion.fromText(joCatalog.optString("HAPI", null));

        JSONArray jaCatalog = joCatalog.optJSONArray("catalog");
        if (jaCatalog == null)
            throw new Exception("Missing catalog object");

        LinkedHashMap<String, Dataset> datasets = new LinkedHashMap<>();
        for (int i = 0; i < jaCatalog.length(); i++) {
            JSONObject joDataset = jaCatalog.optJSONObject(i);
            if (joDataset == null)
                continue;
            String id = joDataset.optString("id", null);
            if (id == null)
                continue;
            String title = joDataset.optString("title", id);

            Dataset dataset = readDataset(server, version, id, title);
            if (dataset == null)
                continue;
            datasets.put(id, dataset);
        }
        if (datasets.isEmpty())
            throw new Exception("Empty catalog");

        return new Catalog(version, datasets);
    }

    private static Dataset readDataset(String server, HapiVersion version, String id, String title) throws Exception {
        UriTemplate.Variables vars = UriTemplate.vars().set(version.getDatasetRequestParam(), id);
        URI uri = new URI(new UriTemplate(server + "info").expand(vars));
        JSONObject joInfo = verifyResponse(JSONUtils.get(uri));
        return new Dataset(title, getDatasetReader(joInfo));
    }

    @Nullable
    private static DatasetReader getDatasetReader(JSONObject jo) throws Exception {
        JSONArray jaParameters = jo.optJSONArray("parameters");
        if (jaParameters == null)
            return null;

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

        HapiTableReader reader = new HapiTableReader(HapiInfo.fromJson(jo));
        return new DatasetReader(reader, parameters);
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

    public static void getData(String server, Catalog catalog, String id, String start, String stop) throws Exception {
        Dataset dataset = catalog.datasets.get(id);
        if (dataset == null)
            return;

        HapiVersion version = catalog.version;
        UriTemplate.Variables requestVars = UriTemplate.vars()
                .set(version.getDatasetRequestParam(), id);
        UriTemplate.Variables rangeVars = UriTemplate.vars()
                .set(version.getStartRequestParam(), start)
                .set(version.getStopRequestParam(), stop);
        UriTemplate uriTemplate = new UriTemplate(server + "data", requestVars);

        URI uri = new URI(uriTemplate.expand(rangeVars));
        try (NetClient nc = NetClient.of(uri); BufferedInputStream is = new BufferedInputStream(nc.getStream())) {
            RowSequence rseq = dataset.reader.hapiReader.createRowSequence(is, null, "csv");
            int numParameters = dataset.reader.parameters.size();

            ArrayList<Long> dates = new ArrayList<>();
            ArrayList<float[]> values = new ArrayList<>();
            while (rseq.next()) {
                String time = (String) rseq.getCell(0);
                if (time == null) // fill
                    continue;
                long milli = TimeUtils.parseZ(time);
                if (milli < TimeUtils.MINIMAL_TIME.milli || milli > TimeUtils.MAXIMAL_TIME.milli)
                    continue;
                dates.add(milli);

                float[] valueArray = new float[numParameters - 1];
                for (int i = 1; i < numParameters; i++) {
                    Object o = rseq.getCell(i);
                    valueArray[i] = o == null ? YAxis.BLANK : ((Double) o).floatValue(); // fill
                }
                values.add(valueArray);
            }
            DatesValues datesValues = new DatesValues(dates.stream().mapToLong(i -> i).toArray(), values.toArray(float[][]::new));
        }
    }

    private static JSONObject verifyResponse(JSONObject jo) throws Exception {
        JSONObject status = jo.optJSONObject("status");
        if (status == null)
            throw new Exception("Malformed HAPI status: " + jo);
        if (1200 != status.optInt("code", -1) || !"OK".equals(status.optString("message", null)))
            throw new Exception("HAPI status not OK: " + status);
        return jo;
    }

}
