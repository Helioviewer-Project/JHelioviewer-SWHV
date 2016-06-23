package org.helioviewer.jhv.io;

import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.json.JSONObject;

public class DataSourcesParser {

    TreeNode[] defaultPath;
    TreeModel model;

    void parse(String root, JSONObject json) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(root);

        try {
            parse(treeNode, json, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        model = new DefaultTreeModel(treeNode);
    }

    private String mergeNames(String str1, String str2) {
        if (str1.equals(str2))
            return str1;
        else if (str1.equals(""))
            return str2;
        return str1 + " " + str2;
    }

    private void parse(DefaultMutableTreeNode parentNode, JSONObject root, String str) throws Exception {
        TreeSet<String> sorted = new TreeSet<String>(JHVGlobals.alphanumComparator);
        sorted.addAll(root.keySet());

        for (String key : sorted) {
            JSONObject json = root.getJSONObject(key);
            String name = json.getString("name").replace((char) 8287, ' '); // for Windows
            if (str == null && !DataSources.SupportedObservatories.contains(name)) // filter top level
                continue;

            if (json.has("sourceId")) { // leaf
                DataSourcesTree.SourceItem item = new DataSourcesTree.SourceItem(key, mergeNames(str, name),
                                                                               json.getString("description"),
                                                                               json.getInt("sourceId"),
                                                                               TimeUtils.sqlDateFormat.parse(json.getString("start")).getTime(),
                                                                               TimeUtils.sqlDateFormat.parse(json.getString("end")).getTime(),
                                                                               json.optBoolean("default", false));
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(item, false);
                parentNode.add(treeNode);
                if (item.defaultItem)
                    defaultPath = treeNode.getPath();
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
