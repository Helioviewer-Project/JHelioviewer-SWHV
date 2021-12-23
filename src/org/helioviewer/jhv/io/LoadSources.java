package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;

import org.everit.json.schema.Schema;
import org.everit.json.schema.Validator;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

class LoadSources {

    static void submit(@Nonnull String server, @Nonnull Validator validator) {
        EventQueueCallbackExecutor.pool.submit(new SourcesLoad(server, validator), new Callback(server));
    }

    private record SourcesLoad(String server, Validator validator) implements Callable<DataSourcesParser> {
        @Override
        public DataSourcesParser call() throws Exception {
            String serverUrl = DataSources.getServerSetting(server, "API.getDataSources");
            String schemaName = DataSources.getServerSetting(server, "schema");
            if (serverUrl == null || schemaName == null)
                throw new Exception("Unknown server: " + server);

            Schema schema;
            try (InputStream is = FileUtils.getResource(schemaName)) {
                JSONObject rawSchema = JSONUtils.get(is);
                SchemaLoader schemaLoader = SchemaLoader.builder().schemaJson(rawSchema).addFormatValidator(new TimeUtils.SQLDateTimeFormatValidator()).build();
                schema = schemaLoader.load().build();
            }

            URI uri = new URI(serverUrl);
            DataSourcesParser parser = new DataSourcesParser(server);
            while (true) {
                try {
                    JSONObject jo = JSONUtils.get(uri);
                    if (schema != null)
                        validator.performValidation(schema, jo);
                    parser.parse(jo);
                    break;
                } catch (IOException e) {
                    // Log.error(uri, e);
                    Thread.sleep(15000);
                }
            }
            return parser;
        }
    }

    private record Callback(String server) implements FutureCallback<DataSourcesParser> {

        @Override
        public void onSuccess(DataSourcesParser result) {
            DataSources.setupSources(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(server, t);
            if (t instanceof ValidationException) {
                ((ValidationException) t).getCausingExceptions().stream().map(ValidationException::getMessage).forEach(Log::error);
            }
        }

    }

}
