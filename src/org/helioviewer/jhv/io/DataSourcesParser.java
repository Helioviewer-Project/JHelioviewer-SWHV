package org.helioviewer.jhv.io;

import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONObject;

public class DataSourcesParser {

    private final String server;
    private final DefaultMutableTreeNode rootNode;
    private DefaultMutableTreeNode defaultNode;
    private long responseTime;

    DataSourcesParser(String _server) {
        server = _server;
        rootNode = new DefaultMutableTreeNode(server);
    }

    void parse(JSONObject json) {
        parse(rootNode, json, null);
    }

    void setResponseTime(long time) {
        responseTime = time;
    }

    DefaultMutableTreeNode getRoot() {
        return rootNode;
    }

    DefaultMutableTreeNode getDefault() {
        return defaultNode;
    }

    private static String mergeNames(String str1, String str2) {
        if (str1.equals(str2))
            return str1;
        if (str1.isEmpty())
            return str2;
        return str1 + ' ' + str2;
    }

    private void parse(DefaultMutableTreeNode parentNode, JSONObject root, String str) {
        TreeSet<String> sorted = new TreeSet<>(JHVGlobals.alphanumComparator);
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
                String description = json.getString("description") + " [" + TimeUtils.formatDate(start) + " : " + TimeUtils.formatDate(end) + ']';
                DataSourcesTree.SourceItem item = new DataSourcesTree.SourceItem(server, mergeNames(str, name),
                        description, sourceId, start, end,
                        json.optBoolean("default", false));
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(item, false);
                parentNode.add(treeNode);

                TreeNode[] path = treeNode.getPath();
                if (path.length == 3) {
                    DataSources.insert(sourceId, path[0].toString(), path[1].toString(), path[2].toString());
                }
                if (item.defaultItem)
                    defaultNode = treeNode;
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
