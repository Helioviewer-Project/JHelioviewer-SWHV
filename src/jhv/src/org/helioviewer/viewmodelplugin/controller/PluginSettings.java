package org.helioviewer.viewmodelplugin.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class handles to write and read the settings made to plug-ins into a
 * specific file.
 *
 * @author Stephan Pagel
 */
public class PluginSettings {

    private static final String NODES_PLUGINS = "Plugins";
    private static final String NODES_PLUGIN = "Plugin";
    private static final String NODES_PLUGINLOCATION = "PluginLocation";
    private static final String NODES_PLUGINACTIVATED = "PluginActivated";

    private static final String PLUGIN_FILENAME = "PluginProperties.xml";

    private static PluginSettings singeltonInstance = new PluginSettings();

    private String settingsFileName;

    private Document xmlDocument;
    private Node pluginsRootNode;

    /**
     * The private constructor to support the singleton pattern.
     * */
    private PluginSettings() {
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            xmlDocument = docBuilder.newDocument();
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
        return singeltonInstance;
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

        if (new File(settingsFileName).exists()) {
            // load XML from file
            try {
                DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                xmlDocument = docBuilder.parse(settingsFileName);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // check root node of XML; if there is an unexpected root node clean up
        // the internal XML document
        if (xmlDocument != null) {
            NodeList list = xmlDocument.getElementsByTagName(NODES_PLUGINS);

            if (list.getLength() == 1) {
                pluginsRootNode = list.item(0);
                cleanUpXMLFile();
                savePluginSettings();
            } else {
                xmlDocument = null;
            }
        }

        // if there is no XML document loaded create a new blank one
        if (xmlDocument == null) {
            try {
                xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            pluginsRootNode = xmlDocument.createElement(NODES_PLUGINS);
            xmlDocument.appendChild(pluginsRootNode);
        }
    }

    /**
     * Checks if for every specified plug-in a corresponding object exists. If
     * there is a XML entry and no corresponding plug-in object exists the XML
     * entry will be removed.
     */
    private void cleanUpXMLFile() {
        NodeList pluginNodes = ((Element) pluginsRootNode).getElementsByTagName(NODES_PLUGIN);

        for (int i = 0; i < pluginNodes.getLength(); i++) {
            Node pluginNode = pluginNodes.item(i);
            NodeList pluginLocationNodes = ((Element) pluginNode).getElementsByTagName(NODES_PLUGINLOCATION);

            if (pluginLocationNodes.getLength() == 1) {
                Node textNode = pluginLocationNodes.item(0).getFirstChild();

                if (textNode != null) {
                    if (!(new File(textNode.getNodeValue()).exists()) && !textNode.getNodeValue().equals("internal"))
                        pluginsRootNode.removeChild(pluginNode);
                } else {
                    pluginsRootNode.removeChild(pluginNode);
                }
            } else {
                pluginsRootNode.removeChild(pluginNode);
            }
        }
    }

    /**
     * Removes a plugin from the XML settings file
     *
     * @param pluginContainer
     *            Container of the plugin which should be removed
     */
    public void removePluginFromXML(PluginContainer pluginContainer) {
        Node pluginNode = findNode(pluginsRootNode, "PluginLocation", pluginContainer.getPluginLocation().getPath());

        if (pluginNode != null) {
            pluginsRootNode.removeChild(pluginNode);
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
        Node pluginNode = findNode(pluginsRootNode, "PluginLocation", pluginContainer.getPluginLocation().getPath());

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

        locationNode.appendChild(xmlDocument.createTextNode(pluginContainer.getPluginLocation().getPath()));
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
    private void editPluginInXML(Node pluginNode, PluginContainer pluginContainer) {

        NodeList list = ((Element) pluginNode).getElementsByTagName(NODES_PLUGINACTIVATED);

        if (list.getLength() == 1) {
            Node textNode = list.item(0).getFirstChild();
            textNode.setNodeValue(Boolean.toString(pluginContainer.isActive()));
        }
    }

    /**
     * Saves the internal XML document to the settings file.
     */
    public void savePluginSettings() {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource source = new DOMSource(xmlDocument);
            FileOutputStream fos = new FileOutputStream(new File(settingsFileName));
            StreamResult result = new StreamResult(fos);

            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if for a given plug-in an entry can be found in the XML document.
     *
     * @param pluginLocation
     *            location of the file where the plug-in comes from.
     * @return true if an entry exists in the XML document; otherwise false.
     */
    public boolean isPluginInformationAvailable(URI pluginLocation) {
        return findNode(pluginsRootNode, NODES_PLUGINLOCATION, pluginLocation.getPath()) != null;
    }

    /**
     * Checks the activated value of a given plug-in in the XML document. If
     * there is no entry in the XML document the return value is false.
     *
     * @param pluginLocation
     *            location of the file where the plug-in comes from.
     * @return boolean value as it is in the XML document or false if no entry
     *         exists.
     */
    public boolean isPluginActivated(URI pluginLocation) {
        // This is to activate plugins automatically during the debugging phase
        // in eclipse
        if (pluginLocation == null) {
            return true;
        }

        Node pluginNode = findNode(pluginsRootNode, NODES_PLUGINLOCATION, pluginLocation.getPath());

        if (pluginNode != null)
            return isActivated(pluginNode, NODES_PLUGINACTIVATED);

        return true;
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
    private Node findNode(Node root, String nodeName, String compareValue) {
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
    private boolean isActivated(Node root, String nodeName) {
        NodeList list = ((Element) root).getElementsByTagName(nodeName);

        if (list.getLength() == 1) {
            Node child = list.item(0).getFirstChild();
            if (child != null && child.getNodeType() == Node.TEXT_NODE)
                return Boolean.parseBoolean(child.getNodeValue());
        }

        return false;
    }

}
