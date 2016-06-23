package org.helioviewer.jhv.io;

import java.text.ParseException;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class DataSourcesParser {

    DefaultMutableTreeNode defaultNode;
    DefaultMutableTreeNode rootNode;

    private final String server;

    DataSourcesParser(String server) {
        this.server = server;
        rootNode = new DefaultMutableTreeNode(server);
    }

    void parse(JSONObject json) throws ParseException, JSONException {
        parse(rootNode, json, null);
    }

    private String mergeNames(String str1, String str2) {
        if (str1.equals(str2))
            return str1;
        else if (str1.equals(""))
            return str2;
        return str1 + " " + str2;
    }

    private void parse(DefaultMutableTreeNode parentNode, JSONObject root, String str) throws ParseException, JSONException {
        TreeSet<String> sorted = new TreeSet<String>(JHVGlobals.alphanumComparator);
        sorted.addAll(root.keySet());

        for (String key : sorted) {
            JSONObject json = root.getJSONObject(key);
            String name = json.getString("name").replace((char) 8287, ' '); // for Windows
            if (str == null && !DataSources.SupportedObservatories.contains(name)) // filter top level
                continue;

            if (json.has("sourceId")) { // leaf
                DataSourcesTree.SourceItem item = new DataSourcesTree.SourceItem(server, key, mergeNames(str, name),
                                                                                 json.getString("description"),
                                                                                 json.getInt("sourceId"),
                                                                                 TimeUtils.sqlDateFormat.parse(json.getString("start")).getTime(),
                                                                                 TimeUtils.sqlDateFormat.parse(json.getString("end")).getTime(),
                                                                                 json.optBoolean("default", false));
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(item, false);
                parentNode.add(treeNode);
                if (item.defaultItem)
                    defaultNode = treeNode;
            } else {
                if (str == null) { // show only top level, else flatten hierarchy
                    DataSourcesTree.Item item = new DataSourcesTree.Item(key, name.replace('_', ' '), json.getString("description"));
                    DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(item);
                    parentNode.add(treeNode);
                    parse(treeNode, json.getJSONObject("children"), "");
                } else
                    parse(parentNode, json.getJSONObject("children"), mergeNames(str, name));
            }
        }
    }

}
