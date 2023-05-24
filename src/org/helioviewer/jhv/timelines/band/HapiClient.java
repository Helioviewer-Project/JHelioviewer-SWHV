package org.helioviewer.jhv.timelines.band;

import java.io.BufferedReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.common.util.concurrent.FutureCallback;

public class HapiClient {

    public static void submit() {
        String server = "https://cdaweb.gsfc.nasa.gov/hapi";
        String id = "SOLO_L2_MAG-RTN-NORMAL-1-MINUTE";
        String parameters = "B_RTN";
        String startTime = "2022-01-01T00:00:00";
        String endTime = "2022-01-02T00:00:00";

        EventQueueCallbackExecutor.pool.submit(new LoadHapi(server, id, parameters, startTime, endTime), new Callback(server, new HapiReceiver()));
    }

    private record LoadHapi(String server, String id, String parameters, String startTime,
                            String endTime) implements Callable<DatesValues> {
        @Override
        public DatesValues call() throws Exception {
            ArrayList<Long> dates = new ArrayList<>();
            ArrayList<float[]> values = new ArrayList<>();

            URI dataURI = new URI(server + "/data?include=header&id=" + id + "&parameters=" + parameters + "&time.min=" + startTime + "&time.max=" + endTime);
            try (NetClient nc = NetClient.of(dataURI); BufferedReader reader = new BufferedReader(nc.getReader())) {
                StringBuilder sb = new StringBuilder();
                String line;
                reader.mark(0);
                while ((line = reader.readLine()) != null && line.startsWith("#")) {
                    sb.append(line.substring(1));
                    reader.mark(0);
                }
                reader.reset();

                HapiParameter[] pars = parseInfo(new JSONObject(sb.toString()));
                String timeFill = pars[0].fill();
                String valueFill = pars[1].fill();
                int valueDim = pars[1].size[0];

                for (CSVRecord rec : CSVFormat.DEFAULT.parse(reader)) {
                    String time = rec.get(0);
                    if (timeFill.equals(time))
                        continue;

                    long milli = TimeUtils.parseZ(time);
                    if (milli > TimeUtils.MINIMAL_TIME.milli && milli < TimeUtils.MAXIMAL_TIME.milli) {
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
            return new DatesValues(dates.stream().mapToLong(i -> i).toArray(), values.toArray(float[][]::new));
        }
    }

    private static HapiParameter[] parseInfo(JSONObject jo) throws Exception {
        try {
            if (!"2.0".equals(jo.getString("HAPI")))
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
                    new HapiParameter(par1.getString("name"), par1.getString("type"), par1.getString("units"), par1.optString("fill", "null"), getSize(par1)),
                    new HapiParameter(par2.getString("name"), par2.getString("type"), par2.getString("units"), par2.optString("fill", "null"), getSize(par2)),
            };
            if (!"Time".equals(pars[0].name()) || !"isotime".equals(pars[0].type()) || !"UTC".equals(pars[0].units()))
                throw new Exception("time parameter");
            if (1 != pars[1].size().length)
                throw new Exception("parameter dimension");

            return pars;
        } catch (Exception e) {
            throw new Exception("HAPI Info: " + e.getMessage() + ":\n" + jo);
        }
    }

    private static final int[] size1 = new int[]{1};

    private static int[] getSize(JSONObject par) {
        JSONArray ja = par.optJSONArray("size");
        if (ja == null)
            return size1;

        int length = ja.length();
        int[] ret = new int[length];
        for (int i = 0; i < length; i++)
            ret[i] = ja.getInt(i);
        return ret;
    }

    private record HapiParameter(String name, String type, String units, String fill, int[] size) {
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
