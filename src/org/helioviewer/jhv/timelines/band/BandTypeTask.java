package org.helioviewer.jhv.timelines.band;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.io.NetStream;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.TimelineSettings;
import org.json.JSONException;

public class BandTypeTask extends JHVWorker<Void, Void> {

    public BandTypeTask() {
        setThreadName("EVE--LoadSources");
    }

    @Override
    protected Void backgroundWork() {
       try (NetStream ns = new NetStream(TimelineSettings.baseURL)) {
            BandType.loadBandTypes(JSONUtils.getJSONStream(ns.getInput()).getJSONArray("objects"));
        } catch (UnknownHostException e) {
            Log.debug("Unknown host, network down?", e);
        } catch (IOException e) {
            Log.error("Error downloading the bandtypes", e);
        } catch (JSONException e) {
            Log.error("JSON parsing error", e);
        }
        return null;
    }

    @Override
    protected void done() {
        Timelines.td.getObservationPanel().setupDatasets();
    }

}
