package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;

import org.everit.json.schema.Schema;
import org.everit.json.schema.Validator;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

class LoadSources implements Callable<DataSourcesParser> {

    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    static void submit(@Nonnull String server, @Nonnull Validator validator) {
        EventQueueCallbackExecutor.pool.submit(new LoadSources(server, validator), new Callback(server));
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
                // LOGGER.log(Level.SEVERE, "Server " + url, e);
                Thread.sleep(15000);
            }
        }
        return parser;
    }

    private record Callback(String server) implements FutureCallback<DataSourcesParser> {

        @Override
        public void onSuccess(DataSourcesParser result) {
            DataSources.setupSources(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            LOGGER.log(Level.SEVERE, "LoadSources error: " + server, t);
            if (t instanceof ValidationException) {
                ((ValidationException) t).getCausingExceptions().stream().map(ValidationException::getMessage).forEach(msg -> LOGGER.log(Level.SEVERE, msg));
            }
        }

    }

}
