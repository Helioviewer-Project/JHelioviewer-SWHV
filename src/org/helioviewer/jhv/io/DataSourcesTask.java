package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;

import org.everit.json.schema.Schema;
import org.everit.json.schema.Validator;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

public class DataSourcesTask extends JHVWorker<Void, Void> {

    private final Validator validator;
    private final DataSourcesParser parser;
    private final String url;
    private final String schemaName;

    public DataSourcesTask(String server, Validator _validator) {
        validator = _validator;
        parser = new DataSourcesParser(server);
        url = DataSources.getServerSetting(server, "API.getDataSources");
        schemaName = DataSources.getServerSetting(server, "schema");
        setThreadName("MAIN--DataSources");
    }

    @Nullable
    @Override
    protected Void backgroundWork() {
        while (true) {
            Schema schema = null;
            try (InputStream is = FileUtils.getResource(schemaName)) {
                JSONObject rawSchema = JSONUtils.get(is);
                SchemaLoader schemaLoader = SchemaLoader.builder().schemaJson(rawSchema).addFormatValidator(new TimeUtils.SQLDateTimeFormatValidator()).build();
                schema = schemaLoader.load().build();
            } catch (Exception e) {
                Log.error("Could not load the JSON schema: ", e);
            }

            try {
                JSONObject jo = JSONUtils.get(url);
/*
                if (url.contains("helioviewer.org")) {
                    jo.getJSONObject("PROBA2").getJSONObject("children").getJSONObject("SWAP").getJSONObject("children").remove("174");
                    JSONObject o = new JSONObject( "{\"sourceId\":32,\"layeringOrder\":1,\"name\":\"174\u205fÅ\",\"nickname\":\"SWAP 174\",\"start\":\"2010-01-04 17:00:50\",\"description\":\"174 Ångström extreme ultraviolet\",\"end\":\"2017-03-21 10:23:31\",\"label\":\"Measurement\"} ");
                    jo.getJSONObject("PROBA2").getJSONObject("children").getJSONObject("SWAP").getJSONObject("children").put("174", o);
                }
*/
                if (schema != null)
                    validator.performValidation(schema, jo);
                parser.parse(jo);
                return null;
            } catch (ValidationException e) {
                Log.error("Server " + url + ' ' + e);
                e.getCausingExceptions().stream().map(ValidationException::getMessage).forEach(Log::error);
                break;
            } catch (IOException e) {
                try {
                    // Log.error("Server " + url + " " + e);
                    Thread.sleep(15000);
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
        DataSources.setupSources(parser);
    }

}
