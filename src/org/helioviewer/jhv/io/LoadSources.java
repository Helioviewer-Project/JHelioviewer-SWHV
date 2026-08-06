package org.helioviewer.jhv.io;

import java.io.InputStream;
import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.Validator;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

class LoadSources {

    static void submit(@Nonnull String serverName, @Nonnull DataSources.Server server) {
        Task.submit(serverName, () -> load(serverName, server), DataSources::setupSources, LoadSources::onFailure);
    }

    private static DataSourcesParser load(String serverName, DataSources.Server server) throws Exception {
        Schema schema;
        try (InputStream is = FileUtils.getResource("/data/sources_v1.0.json")) { // off-load main thread
            JSONObject rawSchema = JSONUtils.get(is);
            SchemaLoader schemaLoader = SchemaLoader.builder().schemaJson(rawSchema).addFormatValidator(new TimeUtils.SQLDateTimeFormatValidator()).build();
            schema = schemaLoader.load().build();
        }

        JSONObject jo = JSONUtils.getUncached(new URI(server.catalogURL()));
        Validator.builder().failEarly().build().performValidation(schema, jo);

        return new DataSourcesParser(serverName).parse(jo);
    }

    private static void onFailure(String serverName, Throwable t) {
        DataSources.setupSources(null); // signal failure
        Log.error(serverName, t);
        if (t instanceof ValidationException ve) {
            ve.getCausingExceptions().stream().map(ValidationException::getMessage).forEach(Log::error);
        }
    }

}
