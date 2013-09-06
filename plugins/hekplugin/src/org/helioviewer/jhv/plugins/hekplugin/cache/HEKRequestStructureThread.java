package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.Vector;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKSettings;
import org.json.JSONException;
import org.json.JSONObject;

public class HEKRequestStructureThread extends HEKRequest implements Runnable {

    private HEKCacheController cacheController;

    public HEKRequestStructureThread(HEKCacheController cacheController, Interval<Date> interval) {
        this.cacheController = cacheController;
        this.interval = interval;
    }

    public void cancel() {
        cancel = true;
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // ignore
            }
        }

        // we are not loading anymore
        this.finishRequest();
    }

    protected void finishRequest() {
        cacheController.setState(cacheController.getRootPath(), HEKCacheLoadingModel.PATH_NOTHING);
        cacheController.expandToLevel(1, true, false);
        cacheController.fireEventsChanged(cacheController.getRootPath());
    }

    public void run() {

        if (cancel)
            return;

        cacheController.setState(cacheController.getRootPath(), HEKCacheLoadingModel.PATH_LOADING);
        cacheController.fireEventsChanged(cacheController.getRootPath());

        requestStructure(interval);

        if (!cancel) {
            this.finishRequest();
        }

    }

    public void requestStructure(Interval<Date> interval) {

        int page = 1;
        boolean hasMorePages = true;

        try {

            while (hasMorePages && page < (HEKSettings.REQUEST_STRUCTURE_MAXPAGES - 1)) {

                // return if the current operation was canceled
                if (cancel)
                    return;

                String startDate = HEKConstants.getSingletonInstance().getDateFormat().format(interval.getStart());
                String endDate = HEKConstants.getSingletonInstance().getDateFormat().format(interval.getEnd());

                String fields = "";

                for (String field : HEKSettings.DOWNLOADER_DOWNLOAD_STRUCTURE_FIELDS) {
                    fields = fields + field + ",";
                }

                fields = fields.substring(0, fields.length() - 1);

                URL url = new URL("http://www.lmsal.com/hek/her?cosec=2&cmd=search&type=column&event_type=**&event_starttime=" + startDate + "&event_endtime=" + endDate + "&event_coordsys=helioprojective&x1=-1200&x2=1200&y1=-1200&y2=1200&return=" + fields + "&temporalmode=overlap&result_limit=" + HEKSettings.REQUEST_STRUCTURE_PAGESIZE + "&page=" + page);

                Log.debug("Requesting Page " + page + " of Max " + HEKSettings.REQUEST_STRUCTURE_MAXPAGES + " HEK Event Structure: " + url);

                // this might take a while
                DownloadStream ds = new DownloadStream(url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());

                // return if the current operation was canceled
                if (cancel)
                    return;

                inputStream = ds.getInput();

                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sb = new StringBuilder();
                String str;
                String all = "";

                while ((str = in.readLine()) != null) {

                    // return if the current operation was canceled
                    if (cancel)
                        return;

                    sb.append(str);

                }

                in.close();

                all = sb.toString();

                JSONObject json = new JSONObject(all);

                parseFeedAndUpdateGUI(json, interval);

                hasMorePages = json.getBoolean("overmax");
                page++;

            }

        } catch (IOException e) {
            Log.error("Error Parsing the HEK Response.");
            Log.error("", e);
        } catch (JSONException e) {
            Log.error("Error Parsing the HEK Response.");
            Log.error("", e);
        }

    }

    public void parseFeedAndUpdateGUI(JSONObject json, Interval<Date> timeRange) {
        // this code might need to change if requestStructure changes
        Vector<HEKPath> paths = HEKEventFactory.getSingletonInstance().parseStructure(json);
        cacheController.feedStructure(paths, timeRange);
    }

}
