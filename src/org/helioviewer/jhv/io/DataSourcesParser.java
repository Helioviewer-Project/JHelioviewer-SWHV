package org.helioviewer.jhv.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.NaturalSort;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONObject;

// Datasets of one server as a two-level tree: groups with their datasets flattened beneath.
public class DataSourcesParser {

    public static class Item {

        public final String name;
        public final String description;

        public Item(String _name, String _description) {
            name = _name;
            description = _description;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static class SourceItem extends Item {

        public final String server;
        public final int sourceId;
        public final long start;
        public final long end;

        public SourceItem(String _server, String _name, String _description, int _sourceId, long _start, long _end) {
            super(_name, _description);
            server = _server;
            sourceId = _sourceId;
            start = _start;
            end = _end;
        }

    }

    public record Group(Item item, List<SourceItem> sources) {}

    private final String server;
    private final ArrayList<Group> groups = new ArrayList<>();
    private SourceItem defaultItem;

    DataSourcesParser(String _server, JSONObject json) {
        server = _server;
        TreeSet<String> sorted = new TreeSet<>(NaturalSort.comparator);
        sorted.addAll(json.keySet());
        for (String key : sorted) {
            JSONObject group = json.getJSONObject(key);
            String name = group.getString("name").replace((char) 8287, ' ').replace('_', '-');
            Item item = new Item(name, group.getString("description"));
            ArrayList<SourceItem> sources = new ArrayList<>();
            groups.add(new Group(item, sources));
            parse(sources, group.getJSONObject("children"), "");
        }
    }

    public String getServer() {
        return server;
    }

    public List<Group> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    @Nullable
    public SourceItem getDefault() {
        return defaultItem;
    }

    private static String mergeNames(String str1, String str2) {
        if (str1.equals(str2))
            return str1;
        if (str1.isEmpty())
            return str2;
        return str1 + ' ' + str2;
    }

    private void parse(List<SourceItem> sources, JSONObject root, String prefix) {
        TreeSet<String> sorted = new TreeSet<>(NaturalSort.comparator);
        sorted.addAll(root.keySet());

        for (String key : sorted) {
            JSONObject json = root.getJSONObject(key);
            String name = json.getString("name").replace((char) 8287, ' '); // for Windows

            if (json.has("sourceId")) { // leaf
                if (json.isNull("start") || json.isNull("end")) // skip empty datasets
                    continue;

                int sourceId = json.getInt("sourceId");
                long start = TimeUtils.parse(TimeUtils.sqlTimeFormatter, json.getString("start"));
                long end = TimeUtils.parse(TimeUtils.sqlTimeFormatter, json.getString("end"));
                SourceItem item = new SourceItem(server, mergeNames(prefix, name), json.getString("description"), sourceId, start, end);
                sources.add(item);

                if (json.optBoolean("default", false))
                    defaultItem = item;
            } else
                parse(sources, json.getJSONObject("children"), mergeNames(prefix, name));
        }
    }

}
