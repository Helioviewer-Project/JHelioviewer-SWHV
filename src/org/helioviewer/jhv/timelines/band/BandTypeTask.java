package org.helioviewer.jhv.timelines.band;

import java.io.IOException;
import java.net.UnknownHostException;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
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
       try (NetClient nc = new NetClient(TimelineSettings.baseURL)) {
            BandType.loadBandTypes(JSONUtils.decodeJSON(nc.getReader()).getJSONArray("objects"));
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
