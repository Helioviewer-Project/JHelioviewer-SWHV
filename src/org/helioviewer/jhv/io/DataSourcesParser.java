package org.helioviewer.jhv.io;

import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.base.NaturalSort;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONObject;

public class DataSourcesParser {

    private final String server;
    private final DefaultMutableTreeNode rootNode;
    private DataSourcesTree.SourceItem defaultItem;

    DataSourcesParser(String _server, JSONObject json) {
        server = _server;
        rootNode = new DefaultMutableTreeNode(server);
        parse(rootNode, json, null);
    }

    DefaultMutableTreeNode getRoot() {
        return rootNode;
    }

    DataSourcesTree.SourceItem getDefault() {
        return defaultItem;
    }

    private static String mergeNames(String str1, String str2) {
        if (str1.equals(str2))
            return str1;
        if (str1.isEmpty())
            return str2;
        return str1 + ' ' + str2;
    }

    private void parse(DefaultMutableTreeNode parentNode, JSONObject root, String str) {
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
                DataSourcesTree.SourceItem item = new DataSourcesTree.SourceItem(server, mergeNames(str, name), json.getString("description"), sourceId, start, end);
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(item, false);
                parentNode.add(treeNode);

                DataSources.insertDataset(sourceId, server, parentNode.toString(), item.toString());
                if (json.optBoolean("default", false))
                    defaultItem = item;
            } else {
                if (str == null) { // show only top level, else flatten hierarchy
                    DataSourcesTree.Item item = new DataSourcesTree.Item(name.replace('_', '-'), json.getString("description"));
                    DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(item);
                    parentNode.add(treeNode);
                    parse(treeNode, json.getJSONObject("children"), "");
                } else
                    parse(parentNode, json.getJSONObject("children"), mergeNames(str, name));
            }
        }
    }

}
