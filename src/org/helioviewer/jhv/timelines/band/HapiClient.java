package org.helioviewer.jhv.timelines.band;

import java.io.BufferedReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.common.util.concurrent.FutureCallback;

import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.gui.components.MoviePanel;

public class HapiClient {

    public static void submit() {
/*
        UriTemplate template = new UriTemplate("https://cdaweb.gsfc.nasa.gov/hapi/data", UriTemplate.vars().set("include", "header").set("format", "csv"));
        String dataset = "SOLO_L2_MAG-RTN-NORMAL-1-MINUTE";
        String parameters = "B_RTN";
        String startTime = "2022-01-01T00:00:00";
        String endTime = "2022-01-02T00:00:00";
*/
        UriTemplate template = new UriTemplate("https://api.helioviewer.org/hapi/data", UriTemplate.vars().set("include", "header").set("format", "csv"));
        String dataset = "AIA_171";
        String parameters = "jp2_url";
        long end = MoviePanel.getInstance().getEndTime();
        String startTime = TimeUtils.format(end - 2 * 60 * TimeUtils.MINUTE_IN_MILLIS);
        String endTime = TimeUtils.format(end);

        String query = template.expand(UriTemplate.vars().set("id", dataset).set("parameters", parameters).set("time.min", startTime).set("time.max", endTime));
        EventQueueCallbackExecutor.pool.submit(new LoadHapi(query), new Callback(query, new HapiReceiver()));
    }

    private record LoadHapi(String query) implements Callable<DatesValues> {
        @Override
        public DatesValues call() throws Exception {
            try (NetClient nc = NetClient.of(new URI(query)); BufferedReader reader = new BufferedReader(nc.getReader())) {
                StringBuilder sb = new StringBuilder();
                String line;
                reader.mark(0);
                while ((line = reader.readLine()) != null && line.startsWith("#")) {
                    sb.append(line.substring(1));
                    reader.mark(0);
                }
                reader.reset();

                HapiParameter[] pars = parseInfo(new JSONObject(sb.toString()));
                String timeFill = pars[0].fill;
                String valueFill = pars[1].fill;
                int valueDim = pars[1].size[0];

                ArrayList<Long> dates = new ArrayList<>();
                ArrayList<float[]> values = new ArrayList<>();
                ArrayList<URI> uris = new ArrayList<>();
                for (CSVRecord rec : CSVFormat.DEFAULT.parse(reader)) {
                    String time = rec.get(0);
                    if (timeFill.equals(time))
                        continue;

                    long milli = TimeUtils.parseZ(time);
                    if (milli > TimeUtils.MINIMAL_TIME.milli && milli < TimeUtils.MAXIMAL_TIME.milli) {
                        if (pars[1].type == HapiType.STRING) {
                            String str = rec.get(1);
                            if (!valueFill.equals(str))
                                uris.add(new URI(str));
                        } else {
                            float[] valueArray = new float[valueDim];
                            for (int i = 0; i < valueDim; i++) {
                                String str = rec.get(1 + i);
                                if (valueFill.equals(str)) {
                                    Arrays.fill(valueArray, YAxis.BLANK);
                                    break;
                                }
                                valueArray[i] = Float.parseFloat(str);
                            }
                            dates.add(milli);
                            values.add(valueArray);
                        }
                    }
                }
                //System.out.println(">>> " + uris.size());
                Load.Image.getAll(uris);
                return new DatesValues(dates.stream().mapToLong(i -> i).toArray(), values.toArray(float[][]::new));
            }
        }
    }

    private static HapiParameter[] parseInfo(JSONObject jo) throws Exception {
        try {
            double version = jo.getDouble("HAPI");
            if (version > 3.1 || version < 2.0)
                throw new Exception("version");
            JSONObject status = jo.getJSONObject("status");
            if (1200 != status.getInt("code") && !"OK".equals(status.getString("message")))
                throw new Exception("status");
            JSONArray parameters = jo.getJSONArray("parameters");
            if (parameters.length() != 2)
                throw new Exception("parameters number");
            JSONObject par1 = parameters.getJSONObject(0);
            JSONObject par2 = parameters.getJSONObject(1);
            HapiParameter[] pars = new HapiParameter[]{
                    new HapiParameter(par1.getString("name"),
                            getHapiType(par1.getString("type")),
                            par1.getString("units"),
                            par1.optString("fill", "null"),
                            getHapiSize(par1.optJSONArray("size"))),
                    new HapiParameter(par2.getString("name"),
                            getHapiType(par2.getString("type")),
                            par2.getString("units"),
                            par2.optString("fill", "null"),
                            getHapiSize(par2.optJSONArray("size"))),
            };
            if (pars[0].type != HapiType.ISOTIME || !"UTC".equals(pars[0].units))
                throw new Exception("time parameter");
            if (1 != pars[1].size.length)
                throw new Exception("parameter dimension");

            return pars;
        } catch (Exception e) {
            throw new Exception("HAPI Info: " + e.getMessage() + ":\n" + jo);
        }
    }

    private static final int[] size1 = new int[]{1};

    private static int[] getHapiSize(JSONArray ja) {
        if (ja == null)
            return size1;

        int length = ja.length();
        int[] ret = new int[length];
        for (int i = 0; i < length; i++)
            ret[i] = ja.getInt(i);
        return ret;
    }

    private enum HapiType {ISOTIME, STRING, NUMBER}

    private static HapiType getHapiType(String type) throws Exception {
        return switch (type) {
            case "isotime" -> HapiType.ISOTIME;
            case "string" -> HapiType.STRING;
            case "integer", "double" -> HapiType.NUMBER;
            default -> throw new Exception("Unknown HAPI type: " + type);
        };
    }

    private record HapiParameter(String name, HapiType type, String units, String fill, int[] size) {
    }

    interface Receiver {
        void setHapiResponse(DatesValues dvs);
    }

    private record Callback(String server, Receiver receiver) implements FutureCallback<DatesValues> {

        @Override
        public void onSuccess(DatesValues dvs) {
            receiver.setHapiResponse(dvs);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            t.printStackTrace();
            Log.error(server, t);
        }

    }

    private static class HapiReceiver implements Receiver {
        @Override
        public void setHapiResponse(DatesValues dvs) {
            long[] dates = dvs.dates();
            float[][] values = dvs.values();
            for (int j = 0; j < dates.length; j++) {
                StringBuilder sb = new StringBuilder(TimeUtils.format(dates[j]));
                for (int i = 0; i < values[j].length; i++) {
                    sb.append(' ').append(values[j][i]);
                }
                System.out.println(sb);
            }
        }
    }

}
