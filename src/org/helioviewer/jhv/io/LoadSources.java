package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;

import org.everit.json.schema.Schema;
import org.everit.json.schema.Validator;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

class LoadSources implements Callable<DataSourcesParser> {

    static Future<DataSourcesParser> submit(@Nonnull String server, @Nonnull Validator validator) {
        return EventQueueCallbackExecutor.pool.submit(new LoadSources(server, validator), new Callback(server));
    }

    private final Validator validator;
    private final DataSourcesParser parser;
    private final String url;
    private final String schemaName;

    private LoadSources(String server, Validator _validator) {
        validator = _validator;
        parser = new DataSourcesParser(server);
        url = DataSources.getServerSetting(server, "API.getDataSources");
        schemaName = DataSources.getServerSetting(server, "schema");
    }

    @Override
    public DataSourcesParser call() throws Exception {
        Schema schema;
        try (InputStream is = FileUtils.getResource(schemaName)) {
            JSONObject rawSchema = JSONUtils.get(is);
            SchemaLoader schemaLoader = SchemaLoader.builder().schemaJson(rawSchema).addFormatValidator(new TimeUtils.SQLDateTimeFormatValidator()).build();
            schema = schemaLoader.load().build();
        }

        while (true) {
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
                break;
            } catch (IOException e) {
                // Log.error("Server " + url + " " + e);
                Thread.sleep(15000);
            }
        }
        return parser;
    }

    private static class Callback implements FutureCallback<DataSourcesParser> {

        private final String s;

        Callback(String _s) {
            s = _s;
        }

        @Override
        public void onSuccess(DataSourcesParser result) {
            DataSources.setupSources(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("LoadSources error: " + s, t);
            if (t instanceof ValidationException) {
                ((ValidationException) t).getCausingExceptions().stream().map(ValidationException::getMessage).forEach(Log::error);
            }
        }

    }

}
