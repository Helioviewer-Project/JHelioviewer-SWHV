package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONObject;
import org.json.JSONTokener;

public class DataSourcesTask extends JHVWorker<Void, Void> {

    private final DataSourcesParser parser;
    private final String url;
    private final String schemaName;

    public DataSourcesTask(String server) {
        parser = new DataSourcesParser(server);
        url = DataSources.getServerSetting(server, "API.getDataSources");
        schemaName = DataSources.getServerSetting(server, "schema");
        setThreadName("MAIN--DataSources");
    }

    @Override
    protected Void backgroundWork() {
        while (true) {
            Schema schema = null;
            try (InputStream is = FileUtils.getResourceInputStream(schemaName)) {
                JSONObject rawSchema = new JSONObject(new JSONTokener(is));
                SchemaLoader schemaLoader = SchemaLoader.builder().schemaJson(rawSchema).addFormatValidator(new TimeUtils.SQLDateTimeFormatValidator()).build();
                schema = schemaLoader.load().build();
            } catch (Exception e) {
                Log.error("Could not load the JSON schema: ", e);
            }

            try {
                JSONObject json = JSONUtils.getJSONStream(new DownloadStream(url).getInput());
/*
                if (url.contains("helioviewer.org")) {
                    json.getJSONObject("PROBA2").getJSONObject("children").getJSONObject("SWAP").getJSONObject("children").remove("174");
                    JSONObject o = new JSONObject( "{\"sourceId\":32,\"layeringOrder\":1,\"name\":\"174\u205fÅ\",\"nickname\":\"SWAP 174\",\"start\":\"2010-01-04 17:00:50\",\"description\":\"174 Ångström extreme ultraviolet\",\"end\":\"2017-03-21 10:23:31\",\"label\":\"Measurement\"} ");
                    json.getJSONObject("PROBA2").getJSONObject("children").getJSONObject("SWAP").getJSONObject("children").put("174", o);
                }
*/
                if (schema != null)
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
