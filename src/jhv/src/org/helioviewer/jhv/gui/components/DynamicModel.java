package org.helioviewer.jhv.gui.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.event.TreeModelListener;
import javax.swing.text.BadLocationException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.net.*;
import java.io.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.jhv.JHVGlobals;

/**
 * a TreeModel that organizes the structure of the nodes in the tree and creates
 * the children of the nodes dynamically
 * 
 * @author andreas
 */
public class DynamicModel implements TreeModel {

    private Node root;

    /**
     * constructor of a Dynamic Model
     * 
     * @param root
     *            name of the root of the Tree
     * @throws BadLocationException
     *             when the root-file cannot be read
     * @throws IOException
     *             IOException
     */
    public DynamicModel(String root) throws BadLocationException, IOException {

        if (root == null) {
            return;
        }
        this.root = new Node(root);
        this.root.toShow = root;
        if ((getSubdirectories(this.root.name)).size() == 0) {
            throw new BadLocationException(root, 0);
        }
    }

    /**
     * computes the URLs of subdirectories and of readable files (.jp2, .jpx) on
     * a http Server
     * 
     * @param urlString
     *            the URL of the server
     * @throws IOException
     * @return an Arraylist with the URLs of subdirectories and of readable
     *         files (.jp2, .jpx) on a http Server as String
     * @throws IOException
     *             IOException
     * @throws BadLocationException
     * @throws BadLocationException
     *             invalid URL
     */
    private static ArrayList<String> getSubdirectories(String urlString) throws IOException, BadLocationException {
        ArrayList<String> returnlist = new ArrayList<String>();

        URL url = new URL(urlString);

        BufferedReader br = new BufferedReader(new InputStreamReader(new DownloadStream(url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout()).getInput()));

        HTMLEditorKit editorKit = new HTMLEditorKit();
        HTMLDocument htmlDoc = new HTMLDocument();
        htmlDoc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        editorKit.read(br, htmlDoc, 0);

        HTMLDocument.Iterator iter = htmlDoc.getIterator(HTML.Tag.A);

        while (iter.isValid()) {

            String link = (String) iter.getAttributes().getAttribute(HTML.Attribute.HREF);

            if (link.charAt(link.length() - 1) == '/' && link.charAt(0) != '/') {
                returnlist.add(urlString + (String) iter.getAttributes().getAttribute(HTML.Attribute.HREF));
            }

            if (link.toLowerCase().endsWith(".jp2") || link.toLowerCase().endsWith(".jpx")) {
                returnlist.add(urlString + link);
            }

            iter.next();
        }
        return returnlist;
    }

    /**
     * return the root of the JTree
     */
    public Object getRoot() {
        return root;
    }

    /**
     * the child with the index index from the node parent
     * 
     * @param parent
     *            parent node
     * @param index
     *            index of the chile
     * @return the child with the index index from the node parent
     */
    public Object getChild(Object parent, int index) {
        Node node = (Node) parent;
        try {
            return node.getChild(index);
        } catch (Exception e) {

            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param node
     *            the node
     * @return the number of children of the node node
     */
    public int getChildCount(Object node) {
        if (isLeaf(node))
            return 0;

        ((Node) node).ensureChildren();
        return ((Node) node).children.size();

    }

    /**
     * *@param node the node
     * 
     * @return if the Node node is a leaf
     */
    public boolean isLeaf(Object node) {
        if (((Node) node).toString() == null)
            return true;

        String link = ((Node) node).toString().toLowerCase();
        if (link.toLowerCase().endsWith(".jp2") || link.toLowerCase().endsWith(".jpx")) {
            return true;
        }
        return false;
    }

    /***
     * @param parent
     *            the node
     * @return the index of the Node child with the parent-node parent
     */
    public int getIndexOfChild(Object parent, Object child) {
        Node node = (Node) parent;

        LinkedList<Node> children = node.getChildren();
        int counter = 0;
        for (Node kid : children) {
            counter++;

            if (kid == child)
                return counter;
        }

        return -1;
    }

    public void valueForPathChanged(TreePath path, Object node) {
    }

    public void addTreeModelListener(TreeModelListener listener) {

    }

    public void removeTreeModelListener(TreeModelListener listener) {

    }

    /**
     * represents a Node in the JTree
     * 
     * @author andreas
     * 
     */
    private class Node {
        private String name;
        private String toShow;
        LinkedList<Node> children = new LinkedList<Node>();

        /**
         * constructor of a node
         * 
         * @param name
         *            the name of the node
         */
        public Node(String name) {
            this.name = name;
        }

        /**
         * 
         * @param index
         *            index of the child to be returned
         * @return child with the index index
         */
        public Node getChild(int index) {
            ensureChildren();
            return children.get(index);
        }

        /**
         * 
         * @return the children of the node
         */

        public LinkedList<Node> getChildren() {
            ensureChildren();
            return children;
        }

        /**
         * makes sure that every node has the children it should have according
         * to the directory strucutre on the http server
         */
        private void ensureChildren() {
            if (children.size() == 0) {
                // reads the children of the node from the http server
                ArrayList<String> children;
                try {
                    children = getSubdirectories(name);

                    for (String child : children) {

                        Node n = new Node(child);
                        n.toShow = child.substring(name.length(), child.length());
                        this.children.add(n);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * @return the name of the node
         */
        public String toString() {
            return toShow;
        }
    }
}
