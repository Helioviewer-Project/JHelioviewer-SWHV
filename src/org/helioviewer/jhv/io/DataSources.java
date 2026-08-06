package org.helioviewer.jhv.io;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;

public class DataSources {

    public interface Listener {
        void setupSources(DataSourcesParser parser);
    }

    public record Server(String label, String catalogURL, String jp2URL, String jpxURL, @Nullable String availabilityURL) {}

    private static final String enabledDatasetsV2 = "[MLSO,TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2,SOLO,GOES-R,IRIS,GONG,ROB,Kanzelhoehe,RHESSI,GOES,PUNCH]";

    private static ImmutableMap<String, Server> servers;

    private static Server createServer(String api, String label, @Nullable String availability) {
        return new Server(label,
                api + "getDataSources/?verbose=true&enable=" + enabledDatasetsV2,
                api + "getJP2Image/?",
                api + "getJPX/?",
                availability);
    }

    private static void loadUserServers(JSONObject json, ImmutableMap.Builder<String, Server> builder) {
        JSONArray ja = json.optJSONArray("org.helioviewer.jhv.source.image");
        if (ja != null) {
            int len = ja.length();
            for (int i = 0; i < len; i++) {
                try {
                    JSONObject jo = ja.getJSONObject(i);
                    builder.put(jo.getString("name"), createServer(jo.getString("api"), jo.getString("label"), jo.optString("availability", null)));
                } catch (Exception e) {
                    Log.warn(e);
                }
            }
        }
    }

    public static void initSources() {
        ImmutableMap.Builder<String, Server> builder = new ImmutableMap.Builder<>();
        Path userSources = Path.of(Directories.SETTINGS.getPath(), "sources.json");
        if (Files.exists(userSources)) { // user servers
            try (BufferedReader reader = Files.newBufferedReader(userSources)) {
                loadUserServers(JSONUtils.get(reader), builder);
            } catch (Exception e) {
                Log.warn(e);
            }
        }

        builder.put("ROB", createServer("https://api.swhv.oma.be/hv_docpage/v2/", "Royal Observatory of Belgium", "https://swhv.oma.be/availability/?"))
                .put("IAS", createServer("https://helioviewer-api.ias.u-psud.fr/v2/", "Institut d'Astrophysique Spatiale", null))
                .put("GSFC", createServer("https://api.helioviewer.org/v2/", "Goddard Space Flight Center", null));
        servers = builder.buildKeepingLast(); // Avoid crash on duplicated server names
    }

    public static Set<String> getServers() {
        return servers.keySet();
    }

    @Nullable
    public static Server getServer(@Nullable String name) {
        return servers.get(name);
    }

    private static final ArrayList<Listener> listeners = new ArrayList<>();

    public static void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    private static int toLoad;
    private static boolean loadCommandLineRequest;

    public static void loadSources(boolean requestAfterLoad) {
        datasetMap.clear(); // clear stale datasets on reload of DataSources
        toLoad = servers.size();
        loadCommandLineRequest = requestAfterLoad;
        servers.keySet().forEach(serverName -> LoadSources.submit(serverName));
    }

    static void setupSources(@Nullable DataSourcesParser parser) {
        if (parser != null) // didn't fail
            listeners.forEach(listener -> listener.setupSources(parser));

        toLoad--;
        if (toLoad == 0 && loadCommandLineRequest) {
            loadCommandLineRequest = false;
            CommandLine.loadRequest();
        }
    }

    private record DatasetId(String server, int sourceId) {}

    private record Source(String observatory, String dataset) {}

    private static final Map<DatasetId, Source> datasetMap = new ConcurrentHashMap<>();

    static void insertDataset(int sourceId, @Nonnull String server, @Nonnull String observatory, @Nonnull String dataset) {
        datasetMap.put(new DatasetId(server, sourceId), new Source(observatory, dataset));
    }

    static int selectDataset(@Nonnull String server, @Nonnull String observatory, @Nonnull String dataset) {
        for (Map.Entry<DatasetId, Source> entry : datasetMap.entrySet()) {
            DatasetId key = entry.getKey();
            if (key.server().equals(server)) {
                Source value = entry.getValue();
                if (value.observatory().contains(observatory) && value.dataset().contains(dataset))
                    return key.sourceId();
            }
        }
        return -1;
    }
}
