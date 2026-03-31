package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.threads.Tasks;
import org.json.JSONArray;
import org.json.JSONObject;

public class LoadSynoptic {

    public static void submit(@Nonnull String server, @Nonnull String query, @Nonnull JSONObject jo) {
        Tasks.submit(server, new SynopticLoad(server, query, jo), Load::getAllImage, "Synoptic request failed");
    }

    private record SynopticLoad(String baseUri, String queryPath, JSONObject jo) implements Callable<List<URI>> {
        @Override
        public List<URI> call() throws Exception {
            URI server = new URI(baseUri);
            JSONArray result = JSONUtils.post(server.resolve(queryPath), jo);
            return extractFitsFiles(server, result);
        }
    }

    private static List<URI> extractFitsFiles(URI server, JSONArray array) {
        List<URI> fitsFiles = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null)
                continue;
            JSONObject resources = item.optJSONObject("_resources");
            if (resources == null)
                continue;
            String fitsFile = resources.optString("fits_file", null);
            if (fitsFile != null) {
                fitsFiles.add(server.resolve(fitsFile));
                // System.out.println(">>> " + fitsFile);
            }
        }
        return fitsFiles;
    }

}
