package org.helioviewer.jhv.io;

import java.io.IOException;

import org.everit.json.schema.Schema;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONObject;

public class DataSourcesTask extends JHVWorker<Void, Void> {

    private final DataSourcesParser parser;
    private final Schema schema;
    private final String url;

    public DataSourcesTask(String server, Schema _schema) {
        parser = new DataSourcesParser(server);
        schema = _schema;
        url = DataSources.getServerSetting(server, "API.dataSources.path");
        setThreadName("MAIN--DataSources");
    }

    @Override
    protected Void backgroundWork() throws Exception {
        while (true) {
            try {
                JSONObject json = JSONUtils.getJSONStream(new DownloadStream(url).getInput());
                schema.validate(json);
                parser.parse(json);
                return null;
            } catch (IOException e) {
                try {
                    // Log.error(e);
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    Log.error(e1);
                    break;
                }
            }
        }
        return null;
    }

    @Override
    protected void done() {
        try {
            get(); // recover background exceptions
        } catch (Exception e) {
            e.printStackTrace();
        }
        ObservationDialog.getInstance().getObservationPanel().setupSources(parser);
    }

}
