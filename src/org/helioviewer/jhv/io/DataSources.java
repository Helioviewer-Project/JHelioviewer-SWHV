package org.helioviewer.jhv.io;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.everit.json.schema.Validator;
import com.google.common.collect.ImmutableMap;

public class DataSources {

    static final Set<String> SupportedObservatories = Set.of(
            "SOHO", "SDO", "STEREO_A", "STEREO_B", "PROBA2",
            "ROB-USET", "ROB-Humain", "NSO-GONG", "NSO-SOLIS", "Kanzelhoehe",
            "NRH", "Yohkoh", "Hinode", "TRACE", "MLSO", "SOLO", "GOES");

    private static final ImmutableMap<String, Map<String, String>> serverSettings = new ImmutableMap.Builder<String, Map<String, String>>().
/*            put("Local", new ImmutableMap.Builder<String, String>().
                    put("API.getDataSources", "http://hv.local/api/docroot/index.php/?action=getDataSources&verbose=true&enable=[SOLO]").
                    put("API.getJP2Image", "http://hv.local/api/docroot/index.php/?action=getJP2Image&").
                    put("API.getJPX", "http://hv.local/api/docroot/index.php/?action=getJPX&").
                    put("label", "Local").
                    put("schema", "/data/sources_v1.0.json").
                    put("availability.images", "http://swhv.oma.be/availability/images/availability/availability.html").
                    build()).*/
            put("ROB", new ImmutableMap.Builder<String, String>().
                    put("API.getDataSources", "http://swhv.oma.be/hv/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]").
                    put("API.getJP2Image", "http://swhv.oma.be/hv/api/index.php?action=getJP2Image&").
                    put("API.getJPX", "http://swhv.oma.be/hv/api/index.php?action=getJPX&").
                    put("label", "Royal Observatory of Belgium").
                    put("schema", "/data/sources_v1.0.json").
                    put("availability.images", "http://swhv.oma.be/availability/images/availability/availability.html").
                    build()).
/*          put("ROB Test", new ImmutableMap.Builder<String, String>().
                    put("API.getDataSources", "http://swhv2.oma.be:8083/index.php?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]").
                    put("API.getJP2Image", "http://swhv2.oma.be:8083/index.php?action=getJP2Image&").
                    put("API.getJPX", "http://swhv2.oma.be:8083/index.php?action=getJPX&").
                    put("label", "Royal Observatory of Belgium").
                    put("schema", "/data/sources_v1.0.json").
                    put("availability.images", "http://swhv2.oma.be/availability/images/availability/availability.html").
                    build()). */
        put("IAS", new ImmutableMap.Builder<String, String>().
        put("API.getDataSources", "https://helioviewer-api.ias.u-psud.fr/v2/getDataSources/?verbose=true&enable=[MLSO,TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]").
        put("API.getJP2Image", "https://helioviewer-api.ias.u-psud.fr/v2/getJP2Image/?").
        put("API.getJPX", "https://helioviewer-api.ias.u-psud.fr/v2/getJPX/?").
        put("label", "Institut d'Astrophysique Spatiale").
        put("schema", "/data/sources_v1.0.json").
        build()).
/*          put("IAS Test", new ImmutableMap.Builder<String, String>().
                    put("API.getDataSources", "https://inf-helio-test-api.ias.u-psud.fr/v2/getDataSources/?verbose=true&enable=[MLSO,TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]").
                    put("API.getJP2Image", "https://inf-helio-test-api.ias.u-psud.fr/v2/getJP2Image/?").
                    put("API.getJPX", "https://inf-helio-test-api.ias.u-psud.fr/v2/getJPX/?").
                    put("label", "Institut d'Astrophysique Spatiale").
                    put("schema", "/data/sources_v1.0.json").
                    build()). */
        put("GSFC", new ImmutableMap.Builder<String, String>().
        put("API.getDataSources", "https://api.helioviewer.org/v2/getDataSources/?verbose=true&enable=[MLSO,TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]").
        put("API.getJP2Image", "https://api.helioviewer.org/v2/getJP2Image/?").
        put("API.getJPX", "https://api.helioviewer.org/v2/getJPX/?").
        put("label", "Goddard Space Flight Center").
        put("schema", "/data/sources_v1.0.json").
        build()).
/*          put("GSFC SCI Test", new ImmutableMap.Builder<String, String>().
                    put("API.getDataSources", "http://helioviewer.sci.gsfc.nasa.gov/api.php?action=getDataSources&verbose=true&enable=[MLSO,TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]").
                    put("API.getJP2Image", "http://helioviewer.sci.gsfc.nasa.gov/api.php?action=getJP2Image&").
                    put("API.getJPX", "http://helioviewer.sci.gsfc.nasa.gov/api.php?action=getJPX&").
                    put("label", "Goddard Space Flight Center SCI Test").
                    put("schema", "/data/sources_v1.0.json").
                    build()). */
/*          put("GSFC NDC Test", new ImmutableMap.Builder<String, String>().
                    put("API.getDataSources", "http://gs671-heliovw7.ndc.nasa.gov/api.php?action=getDataSources&verbose=true&enable=[MLSO,TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]").
                    put("API.getJP2Image", "http://gs671-heliovw7.ndc.nasa.gov/api.php?action=getJP2Image&").
                    put("API.getJPX", "http://gs671-heliovw7.ndc.nasa.gov/api.php?action=getJPX&").
                    put("label", "Goddard Space Flight Center NDC Test");.
                    put("schema", "/data/sources_v1.0.json").
                    build()). */
        build();

    public static Set<String> getServers() {
        return serverSettings.keySet();
    }

    @Nullable
    public static String getServerSetting(@Nonnull String server, @Nonnull String setting) {
        Map<String, String> settings = serverSettings.get(server);
        return settings == null ? null : settings.get(setting);
    }

    private static int toLoad = serverSettings.size();

    public static void loadSources() {
        Validator validator = Validator.builder().failEarly().build();
        serverSettings.keySet().forEach(serverName -> LoadSources.submit(serverName, validator));
    }

    private static final ArrayList<DataSourcesListener> listeners = new ArrayList<>();

    public static void addListener(DataSourcesListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    static void setupSources(DataSourcesParser parser) {
        listeners.forEach(listener -> listener.setupSources(parser));

        toLoad--;
        if (toLoad == 0)
            CommandLine.loadRequest();
    }

}
