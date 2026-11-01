package org.helioviewer.jhv.io;

import java.time.Instant;
import java.util.List;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.thread.UiThread;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

public final class DataSourcesTest {

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        DataSources.initSources();
        Settings.setProperty("dataSources.defaultServer", "ROB");
        DataSourcesParser parser = new DataSourcesParser("ROB", catalog());
        checkParser(parser);
        UiThread.postAndWait(() -> {
            checkRegistration(parser);
        });
        System.out.println("DataSourcesTest passed");
    }

    private static JSONObject leaf(String name, int id, boolean selectedDefault) {
        return new JSONObject().put("name", name).put("description", "Description " + name).put("sourceId", id)
                .put("start", "2020-01-02 03:04:05").put("end", "2021-02-03 04:05:06").put("default", selectedDefault);
    }

    private static JSONObject group(String name, JSONObject children) {
        return new JSONObject().put("name", name).put("description", "Description " + name).put("children", children);
    }

    private static JSONObject catalog() {
        JSONObject missingStart = leaf("missing", 102, true);
        missingStart.remove("start");
        JSONObject children = new JSONObject()
                .put("channel10", leaf("304", 10, true))
                .put("channel2", leaf("171", 2, true))
                .put("repeat", group("Same", new JSONObject().put("x", group("Same",
                        new JSONObject().put("x", leaf("Same", 33, false))))))
                .put("space", leaf("X\u205fY_under", 34, false))
                .put("skip1", leaf("empty", 101, true).put("end", JSONObject.NULL))
                .put("skip2", missingStart);
        return new JSONObject().put("group10", group("Empty_10", new JSONObject()))
                .put("group2", group("Group_2\u205fA", children))
                .put("group20", group("Last", new JSONObject().put("x", leaf("Final", 50, true))));
    }

    private static void checkParser(DataSourcesParser parser) {
        List<DataSourcesParser.Group> groups = parser.getGroups();
        check(groups.size() == 3 && groups.get(0).item().name.equals("Group-2 A")
                && groups.get(1).item().name.equals("Empty-10") && groups.get(2).item().name.equals("Last"),
                "natural group order and normalized group names");
        check(groups.get(1).sources().isEmpty(), "empty groups are retained");
        List<DataSourcesParser.SourceItem> sources = groups.getFirst().sources();
        check(ids(sources).equals(List.of(2, 10, 33, 34)), "natural source order and skipped empty datasets");
        check(sources.get(2).name.equals("Same") && sources.get(3).name.equals("X Y_under"),
                "nested names merge without repetition and source underscores remain unchanged");
        DataSourcesParser.SourceItem first = sources.getFirst();
        check(first.server.equals("ROB") && first.description.equals("Description 171")
                && first.start == Instant.parse("2020-01-02T03:04:05Z").toEpochMilli()
                && first.end == Instant.parse("2021-02-03T04:05:06Z").toEpochMilli(), "source fields and UTC extent");
        check(parser.getDefault().sourceId == 50, "last default in traversal order wins");
        check(DataSources.selectDataset("ROB", "Group-2 A", "171") == -1, "parsing does not register datasets");

        DataSourcesParser empty = new DataSourcesParser("empty", new JSONObject());
        check(empty.getGroups().isEmpty() && empty.getDefault() == null, "empty catalog");
        DataSourcesParser skipped = new DataSourcesParser("skipped", new JSONObject().put("x", group("G",
                new JSONObject().put("x", leaf("skip", 1, true).put("start", JSONObject.NULL)))));
        check(skipped.getGroups().getFirst().sources().isEmpty() && skipped.getDefault() == null, "skipped default is ignored");
        try {
            new DataSourcesParser("invalid", new JSONObject()
                    .put("group2", group("G", new JSONObject().put("x", leaf("partial", 1, false))))
                    .put("group10", leaf("root", 2, false)));
            throw new AssertionError("top-level source was accepted as a group");
        } catch (JSONException expected) {
        }
        check(DataSources.selectDataset("invalid", "G", "partial") == -1, "failed parsing does not register partial datasets");
    }

    private static void checkRegistration(DataSourcesParser parser) {
        boolean[] notified = {false};
        DataSources.setListener(loaded -> {
            check(loaded == parser && DataSources.selectDataset("ROB", "Group-2 A", "171") == 2,
                    "datasets are registered before catalog notification");
            notified[0] = true;
        });
        try {
            DataSources.setupSources(null);
            check(!notified[0], "failed catalog delivery does not notify the listener");
            DataSources.setupSources(parser);
            check(notified[0], "successful catalog delivery notifies the listener");
        } finally {
            DataSources.setListener(null);
        }
        check(DataSources.selectDataset("ROB", "Group-2 A", "empty") == -1, "empty source is not registered");
        DataSourcesParser duplicate = new DataSourcesParser("duplicate", new JSONObject()
                .put("group2", group("Earlier", new JSONObject().put("x", leaf("old", 7, false))))
                .put("group10", group("Later", new JSONObject().put("x", leaf("new", 7, false)))));
        DataSources.setupSources(duplicate);
        check(duplicate.getGroups().size() == 2 && DataSources.selectDataset("duplicate", "Earlier", "old") == -1
                && DataSources.selectDataset("duplicate", "Later", "new") == 7,
                "registration without a listener preserves last duplicate ID wins");
    }

    private static List<Integer> ids(List<DataSourcesParser.SourceItem> sources) {
        return sources.stream().map(item -> item.sourceId).toList();
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
