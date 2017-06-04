package org.helioviewer.jhv.base.plugin;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.helioviewer.jhv.base.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// This class handles to write and read the settings made to plug-ins into a specific file
public class PluginSettings {

    private static final String NODES_PLUGINS = "JHVPlugins";
    private static final String NODES_PLUGIN = "Plugin";
    private static final String NODES_PLUGINLOCATION = "Name";
    private static final String NODES_PLUGINACTIVATED = "Activated";

    private static final String PLUGIN_FILENAME = "PluginProperties.xml";

    private static final PluginSettings singletonInstance = new PluginSettings();

    private String settingsFileName;

    private Document xmlDocument;
    private Node pluginsRootNode;

    /**
     * The private constructor to support the singleton pattern.
     * */
    private PluginSettings() {
        try {
            xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method returns the sole instance of this class.
     *
     * @return the only instance of this class.
     * */
    public static PluginSettings getSingletonInstance() {
        return singletonInstance;
    }

    /**
     * Loads the saved settings from the corresponding file.
     *
     * @param settingsFilePath
     *            Path of the directory where the plug-in settings file is
     *            saved.
     */
    public void loadPluginSettings(String settingsFilePath) {
        settingsFileName = settingsFilePath + PLUGIN_FILENAME;
        if (new File(settingsFileName).canRead()) {
            // load XML from file
            try {
                xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(settingsFileName);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }
        }

        // check root node of XML; if there is an unexpected root node clean up
        // the internal XML document
        if (xmlDocument != null) {
            NodeList list = xmlDocument.getElementsByTagName(NODES_PLUGINS);
            if (list.getLength() == 1) {
                pluginsRootNode = list.item(0);
            } else {
                xmlDocument = null;
            }
        }

        if (xmlDocument == null || pluginsRootNode == null) {
            try {
                xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            if (xmlDocument != null) {
                pluginsRootNode = xmlDocument.createElement(NODES_PLUGINS);
                xmlDocument.appendChild(pluginsRootNode);
            }
        }
    }

    /**
     * Adds or updates the information of a passed plug-in to the internal XML
     * document. This method does not save the XML document to the corresponding
     * settings file!
     *
     * @param pluginContainer
     *            Plug-in container whose information have to be updated in the
     *            internal XML document.
     * @see #savePluginSettings()
     */
    public void pluginSettingsToXML(PluginContainer pluginContainer) {
        Node pluginNode = findNode(pluginsRootNode, NODES_PLUGINLOCATION, pluginContainer.toString());
        if (pluginNode == null) {
            addPluginToXML(pluginContainer);
        } else {
            editPluginInXML(pluginNode, pluginContainer);
        }
    }

    /**
     * Adds the information of a plug-in to the XML document.
     *
     * @param pluginContainer
     *            Plug-in container whose information have to be added to the
     *            internal XML document.
     */
    private void addPluginToXML(PluginContainer pluginContainer) {
        Node pluginNode = xmlDocument.createElement(NODES_PLUGIN);
        Node locationNode = xmlDocument.createElement(NODES_PLUGINLOCATION);
        Node activatedNode = xmlDocument.createElement(NODES_PLUGINACTIVATED);

        pluginNode.appendChild(locationNode);
        pluginNode.appendChild(activatedNode);

        locationNode.appendChild(xmlDocument.createTextNode(pluginContainer.toString()));
        activatedNode.appendChild(xmlDocument.createTextNode(Boolean.toString(pluginContainer.isActive())));

        pluginsRootNode.appendChild(pluginNode);
    }

    /**
     * Updates the information of a passed plug-in to the internal XML document.
     *
     * @param pluginNode
     *            Corresponding plug-in node of the given plug-in.
     * @param pluginContainer
     *            Plug-in container whose information have to be updated in the
     *            internal XML document.
     */
    private static void editPluginInXML(Node pluginNode, PluginContainer pluginContainer) {
        NodeList list = ((Element) pluginNode).getElementsByTagName(NODES_PLUGINACTIVATED);
        if (list.getLength() == 1) {
            Node textNode = list.item(0).getFirstChild();
            textNode.setNodeValue(Boolean.toString(pluginContainer.isActive()));
        }
    }

    // Saves the internal XML document to the settings file
    public void savePluginSettings() {
        try {
            DOMSource source = new DOMSource(xmlDocument);
            StreamResult result = new StreamResult(FileUtils.newBufferedOutputStream(new File(settingsFileName)));
            TransformerFactory.newInstance().newTransformer().transform(source, result);
        } catch (TransformerFactoryConfigurationError | TransformerException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks the activated value of a given plug-in in the XML document. If
     * there is no entry in the XML document the return value is false.
     */
    public boolean isPluginActivated(String jarName) {
        Node pluginNode = findNode(pluginsRootNode, NODES_PLUGINLOCATION, jarName);
        return pluginNode == null || isActivated(pluginNode, NODES_PLUGINACTIVATED);
    }

    /**
     * Method checks all child nodes of a given node recursively if there text
     * node has a specific value.
     *
     * @param root
     *            Check all child nodes of this node.
     * @param nodeName
     *            Name of the nodes where to check the text node.
     * @param compareValue
     *            Value of the text node to search for.
     * @return The first found node whose text node has the given value or null
     *         if no node could be found with the given values.
     */
    private static Node findNode(Node root, String nodeName, String compareValue) {
        NodeList list = ((Element) root).getElementsByTagName(nodeName);
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i).getFirstChild();
            if (child != null && child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().equals(compareValue)) {
                return list.item(i).getParentNode();
            }
        }
        return null;
    }

    /**
     * Method checks all child nodes of a given node recursively for there text
     * node and returns the corresponding boolean value.
     *
     * @param root
     *            Check all child nodes of this node.
     * @param nodeName
     *            Name of the nodes where to check the text node.
     * @return The boolean value of the first found given node. If no entry
     *         could be found the return value is false.
     */
    private static boolean isActivated(Node root, String nodeName) {
        NodeList list = ((Element) root).getElementsByTagName(nodeName);
        if (list.getLength() == 1) {
            Node child = list.item(0).getFirstChild();
            if (child != null && child.getNodeType() == Node.TEXT_NODE)
                return Boolean.parseBoolean(child.getNodeValue());
        }
        return false;
    }

}
