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

    public static class Item {

        // Key as needed to send to the API for this item
        public final String key;

        // Display name for a dropdown list
        public final String name;

        // Tooltip description
        public final String description;

        // Flag if this should take as default item
        public final boolean defaultItem;

        public Item(String key, String name, String description, boolean defaultItem) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.defaultItem = defaultItem;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static class SourceItem extends Item {

        public final int sourceId;
        public final long start;
        public final long end;

        public SourceItem(String key, String name, String description, boolean defaultItem, int sourceId, long start, long end) {
            super(key, name, description, defaultItem);
            this.sourceId = sourceId;
            this.start = start;
            this.end = end;
        }

    }

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

            if (json.has("sourceId")) { // leaf
                SourceItem item = new SourceItem(key, mergeNames(str, json.getString("name").replace((char) 8287, ' ')), // for Windows
                                                      json.getString("description"),
                                                      json.optBoolean("default", false),
                                                      json.getInt("sourceId"),
                                                      TimeUtils.sqlDateFormat.parse(json.getString("start")).getTime(),
                                                      TimeUtils.sqlDateFormat.parse(json.getString("end")).getTime());
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(item, false);
                parentNode.add(treeNode);
                if (item.defaultItem)
                    defaultPath = treeNode.getPath();
            } else {
                String name = json.getString("name").replace((char) 8287, ' '); // for Windows
                if (str == null) { // show only first level, else flatten hierarchy
                    Item item = new Item(key, name,
                                              json.getString("description"),
                                              json.optBoolean("default", false));
                    DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(item);
                    parentNode.add(treeNode);
                    parse(treeNode, json.getJSONObject("children"), "");
                } else
                    parse(parentNode, json.getJSONObject("children"), mergeNames(str, name));
            }
        }
    }

}
