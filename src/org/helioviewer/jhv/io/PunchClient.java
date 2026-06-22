package org.helioviewer.jhv.io;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;

// Lists and loads native FITS files from the PUNCH archive at the Solar Data
// Analysis Center (public, no auth). The archive is a plain directory tree:
// {BASE}/{level}/{product}/{YYYY}/{MM}/{DD}/PUNCH_L{lvl}_{code}_{YYYYMMDDhhmmss}_v{ver}.fits
public final class PunchClient {

    private static final String BASE_URL = "https://umbra.nascom.nasa.gov/punch";
    private static final Pattern FILE_PATTERN = Pattern.compile("href=\"(PUNCH_L[0-9A-Z]_[A-Z0-9]{3}_(\\d{14})_v[0-9A-Za-z]+\\.fits)\"");
    private static final Pattern DIR_PATTERN = Pattern.compile("href=\"([A-Z0-9]{2,4})/\"");
    private static final Pattern NUM_DIR_PATTERN = Pattern.compile("href=\"(\\d{2,4})/\"");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public record DataItem(String file, URI uri, long milli) {
        @Override
        public String toString() {
            return TimeUtils.format(milli) + "  " + file;
        }
    }

    public interface ReceiverItems {
        void setPunchResponseItems(List<DataItem> list);
    }

    public interface ReceiverProducts {
        void setPunchResponseProducts(List<String> list);
    }

    public interface ReceiverCoverage {
        // Latest day with data, in UTC epoch ms; 0 if the archive has nothing for this product
        void setPunchResponseCoverage(long latestDayMilli);
    }

    public static void submitSearchTime(@Nonnull ReceiverItems receiver, @Nonnull String level, @Nonnull String product, long start, long end, long cadence) {
        Task.submit("punch", new QueryItems(level, product, start, end, cadence), receiver::setPunchResponseItems, "Error listing the PUNCH archive");
    }

    public static void submitGetProducts(@Nonnull ReceiverProducts receiver, @Nonnull String level) {
        Task.submit("punch", new QueryProducts(level), receiver::setPunchResponseProducts, "Error listing the PUNCH archive");
    }

    public static void submitGetCoverage(@Nonnull ReceiverCoverage receiver, @Nonnull String level, @Nonnull String product) {
        Task.submit("punch", new QueryCoverage(level, product), receiver::setPunchResponseCoverage, "Error listing the PUNCH archive");
    }

    public static void submitLoad(@Nonnull List<DataItem> items, @Nonnull String level, @Nonnull String product, long start, long end, long cadence) {
        List<URI> uris = items.stream().map(DataItem::uri).toList();
        Commands.loadImage(uris).thenAccept(layer -> {
            if (layer != null)
                rememberQuery(layer, level, product, start, end, cadence, uris);
        });
    }

    // ---- per-layer query state, for the "check missing frames" refresh action -----

    public record RefreshResult(int existingCount, int newCount, ImageLayer newLayer) {}

    public interface RefreshReceiver {
        void onRefreshComplete(RefreshResult result);
    }

    private record QueryState(String level, String product, long start, long end, long cadence, Set<URI> loadedUris) {}

    // Weak so we do not pin layers that the user has removed
    private static final Map<ImageLayer, QueryState> layerQueries = Collections.synchronizedMap(new WeakHashMap<>());

    static void rememberQuery(ImageLayer layer, String level, String product, long start, long end, long cadence, List<URI> uris) {
        layerQueries.put(layer, new QueryState(level, product, start, end, cadence, new HashSet<>(uris)));
    }

    public static boolean hasRememberedQuery(ImageLayer layer) {
        return layerQueries.containsKey(layer);
    }

    // Re-runs the original query, diffs against the URIs we already loaded for this layer,
    // and loads any new ones as a fresh layer (so the user can compare or replace)
    public static void submitRefresh(@Nonnull ImageLayer layer, @Nonnull RefreshReceiver receiver) {
        QueryState q = layerQueries.get(layer);
        if (q == null) {
            receiver.onRefreshComplete(new RefreshResult(0, 0, null));
            return;
        }
        Task.submit("punch-refresh", new QueryItems(q.level, q.product, q.start, q.end, q.cadence), items -> {
            List<URI> newUris = new ArrayList<>();
            for (DataItem it : items)
                if (!q.loadedUris.contains(it.uri))
                    newUris.add(it.uri);
            if (newUris.isEmpty()) {
                receiver.onRefreshComplete(new RefreshResult(q.loadedUris.size(), 0, null));
                return;
            }
            Commands.loadImage(newUris).thenAccept(newLayer -> {
                // Track the union on the original layer so a second refresh diffs correctly
                q.loadedUris.addAll(newUris);
                if (newLayer != null)
                    rememberQuery(newLayer, q.level, q.product, q.start, q.end, q.cadence, newUris);
                receiver.onRefreshComplete(new RefreshResult(q.loadedUris.size() - newUris.size(), newUris.size(), newLayer));
            });
        }, "Error refreshing PUNCH layer");
    }

    private static String readIndex(String url) throws Exception {
        try (NetClient nc = NetClient.of(new URI(url), true, NetClient.NetCache.NETWORK)) {
            boolean ok = nc.isSuccessful();
            String body = ok ? nc.getSource().readUtf8() : null;
            // Log.info("PUNCH " + url + " -> " + (ok ? body.length() + " bytes" : "not ok"));
            if (!ok)
                Log.info("PUNCH " + url + " -> not ok");

            return body;
        }
    }

    private record QueryProducts(String level) implements Callable<List<String>> {
        @Override
        public List<String> call() throws Exception {
            String html = readIndex(BASE_URL + '/' + level + '/');
            List<String> products = new ArrayList<>();
            if (html != null) {
                Matcher m = DIR_PATTERN.matcher(html);
                while (m.find())
                    products.add(m.group(1));
            }
            return products;
        }
    }

    private record QueryCoverage(String level, String product) implements Callable<Long> {
        @Override
        public Long call() throws Exception {
            // Walk year -> month -> day, picking the lexicographically largest at each level
            String latestYear = latestNumeric(BASE_URL + '/' + level + '/' + product + '/');
            if (latestYear == null)
                return 0L;
            String latestMonth = latestNumeric(BASE_URL + '/' + level + '/' + product + '/' + latestYear + '/');
            if (latestMonth == null)
                return 0L;
            String latestDay = latestNumeric(BASE_URL + '/' + level + '/' + product + '/' + latestYear + '/' + latestMonth + '/');
            if (latestDay == null)
                return 0L;
            return TimeUtils.parseDate(latestYear + "-" + latestMonth + "-" + latestDay);
        }

        private static String latestNumeric(String url) throws Exception {
            String html = readIndex(url);
            if (html == null)
                return null;
            String latest = null;
            Matcher m = NUM_DIR_PATTERN.matcher(html);
            while (m.find()) {
                String s = m.group(1);
                if (latest == null || s.compareTo(latest) > 0)
                    latest = s;
            }
            return latest;
        }
    }

    private record QueryItems(String level, String product, long start, long end,
                              long cadence) implements Callable<List<DataItem>> {
        @Override
        public List<DataItem> call() throws Exception {
            // Newer versions of the same timestamp overwrite older ones (index is name-sorted)
            TreeMap<Long, DataItem> found = new TreeMap<>();
            for (long day = TimeUtils.floorDay(start); day <= end; day += TimeUtils.DAY_IN_MILLIS)
                listDay(found, day);

            // lastKept is seeded just below the first possible in-range item so that any
            // item with milli >= start passes the cadence check on the first iteration.
            // Using Long.MIN_VALUE here would overflow the (item.milli - lastKept) check.
            List<DataItem> result = new ArrayList<>(found.size());
            long lastKept = start - Math.max(1, cadence) - 1;
            for (DataItem item : found.values()) {
                if (item.milli >= start && item.milli <= end && item.milli - lastKept >= cadence) {
                    result.add(item);
                    lastKept = item.milli;
                }
            }
            return result;
        }

        private void listDay(TreeMap<Long, DataItem> found, long day) throws Exception {
            LocalDateTime date = LocalDateTime.ofEpochSecond(day / 1000, 0, ZoneOffset.UTC);
            String dirUrl = String.format("%s/%s/%s/%04d/%02d/%02d/", BASE_URL, level, product, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
            String html = readIndex(dirUrl);
            if (html == null)
                return;

            int matched = 0;
            Matcher m = FILE_PATTERN.matcher(html);
            while (m.find()) {
                String file = m.group(1);
                long milli = TimeUtils.parse(FILE_TIME, m.group(2));
                found.put(milli, new DataItem(file, URI.create(dirUrl + file), milli));
                matched++;
            }
            Log.info("PUNCH parsed " + matched + " files from " + dirUrl);
        }
    }

    private PunchClient() {}
}
