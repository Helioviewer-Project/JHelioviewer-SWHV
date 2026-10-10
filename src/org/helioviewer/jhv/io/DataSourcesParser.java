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

    public record Node(Item item, List<Node> children) {}

    private final String server;
    private final ArrayList<Node> nodes = new ArrayList<>();
    private SourceItem defaultItem;

    DataSourcesParser(String _server, JSONObject json) {
        server = _server;
        parse(nodes, "", json, null);
    }

    public String getServer() {
        return server;
    }

    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
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

    private void parse(List<Node> siblings, String group, JSONObject root, @Nullable String str) {
        TreeSet<String> sorted = new TreeSet<>(NaturalSort.comparator);
        sorted.addAll(root.keySet());

        for (String key : sorted) {
            JSONObject json = root.getJSONObject(key);
            String name = json.getString("name").replace((char) 8287, ' '); // for Windows

            if (str != null /* can't happen */ && json.has("sourceId")) { // leaf
                if (json.isNull("start") || json.isNull("end")) // skip empty datasets
                    continue;

                int sourceId = json.getInt("sourceId");
                long start = TimeUtils.parse(TimeUtils.sqlTimeFormatter, json.getString("start"));
                long end = TimeUtils.parse(TimeUtils.sqlTimeFormatter, json.getString("end"));
                SourceItem item = new SourceItem(server, mergeNames(str, name), json.getString("description"), sourceId, start, end);
                siblings.add(new Node(item, List.of()));

                DataSources.insertDataset(sourceId, server, group, item.toString());
                if (json.optBoolean("default", false))
                    defaultItem = item;
            } else {
                if (str == null) { // show only top level, else flatten hierarchy
                    Item item = new Item(name.replace('_', '-'), json.getString("description"));
                    ArrayList<Node> children = new ArrayList<>();
                    siblings.add(new Node(item, children));
                    parse(children, item.name, json.getJSONObject("children"), "");
                } else
                    parse(siblings, group, json.getJSONObject("children"), mergeNames(str, name));
            }
        }
    }

}
