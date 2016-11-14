package org.helioviewer.jhv.io;

import java.text.ParseException;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.json.JSONObject;

public class DataSourcesParser {

    final DefaultMutableTreeNode rootNode;
    DefaultMutableTreeNode defaultNode;

    private final String server;

    DataSourcesParser(String server) {
        this.server = server;
        rootNode = new DefaultMutableTreeNode(server);
    }

    void parse(JSONObject json) throws ParseException {
        parse(rootNode, json, null);
    }

    private static String mergeNames(String str1, String str2) {
        if (str1.equals(str2))
            return str1;
        if (str1.isEmpty())
            return str2;
        return str1 + ' ' + str2;
    }

    private void parse(DefaultMutableTreeNode parentNode, JSONObject root, String str) throws ParseException {
        TreeSet<String> sorted = new TreeSet<>(JHVGlobals.alphanumComparator);
        sorted.addAll(root.keySet());

        for (String key : sorted) {
            JSONObject json = root.getJSONObject(key);
            String name = json.getString("name").replace((char) 8287, ' '); // for Windows
            if (str == null && !DataSources.SupportedObservatories.contains(name)) // filter top level
                continue;

            if (str != null /* can't happen */ && json.has("sourceId")) { // leaf
                long start = TimeUtils.sqlDateFormat.parse(json.getString("start")).getTime();
                long end = TimeUtils.sqlDateFormat.parse(json.getString("end")).getTime();
                String description = json.getString("description") + " [" + TimeUtils.dateFormat.format(start) + " : " + TimeUtils.dateFormat.format(end) + ']';
                DataSourcesTree.SourceItem item = new DataSourcesTree.SourceItem(server, mergeNames(str, name),
                                                                                 description, json.getInt("sourceId"), start, end,
                                                                                 json.optBoolean("default", false));
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(item, false);
                parentNode.add(treeNode);
                if (item.defaultItem)
                    defaultNode = treeNode;
            } else {
                if (str == null) { // show only top level, else flatten hierarchy
                    DataSourcesTree.Item item = new DataSourcesTree.Item(name.replace('_', ' '), json.getString("description"));
                    DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(item);
                    parentNode.add(treeNode);
                    parse(treeNode, json.getJSONObject("children"), "");
                } else
                    parse(parentNode, json.getJSONObject("children"), mergeNames(str, name));
            }
        }
    }

}
