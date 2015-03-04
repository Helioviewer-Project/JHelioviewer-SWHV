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

import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodelplugin.filter.FilterContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
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

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final String NODES_PLUGINS = "Plugins";
    private static final String NODES_PLUGIN = "Plugin";
    private static final String NODES_PLUGINLOCATION = "PluginLocation";
    private static final String NODES_PLUGINACTIVATED = "PluginActivated";
    private static final String NODES_FILTERS = "Filters";
    private static final String NODES_FILTER = "Filter";
    private static final String NODES_FILTERNAME = "FilterName";
    private static final String NODES_FILTERACTIVATED = "FilterActivated";
    private static final String NODES_FILTERPOSITION = "FilterPosition";
    private static final String NODES_OVERLAYS = "Overlays";
    private static final String NODES_OVERLAY = "Overlay";
    private static final String NODES_OVERLAYNAME = "OverlayName";
    private static final String NODES_OVERLAYACTIVATED = "OverlayActivated";
    private static final String NODES_OVERLAYPOSITION = "OverlayPosition";

    private static final String PLUGIN_FILENAME = "PluginProperties.xml";

    private static PluginSettings singeltonInstance = new PluginSettings();

    private String settingsFileName;

    private Document xmlDocument;
    private Node pluginsRootNode;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

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
     * Adds or updates the information of a passed filter to the internal XML
     * document. This method does not save the XML document to the corresponding
     * settings file!
     * 
     * @param pluginLocation
     *            location of the plug-in where the filter comes from.
     * @param filterContainer
     *            Filter container whose information have to be updated in the
     *            internal XML document.
     * @see #savePluginSettings()
     */
    public void filterSettingsToXML(URI pluginLocation, FilterContainer filterContainer) {

        if (pluginLocation == null)
            return;

        Node pluginNode = findNode(pluginsRootNode, "PluginLocation", pluginLocation.getPath());

        if (pluginNode == null)
            return;

        Node filterNode = findNode(pluginNode, NODES_FILTERNAME, filterContainer.getFilterClass().getName());

        if (filterNode == null) {
            addFilterToXML(pluginNode, filterContainer);
        } else {
            editFilterInXML(filterNode, filterContainer);
        }
    }

    /**
     * Adds the information of a filter to the XML document.
     * 
     * @param pluginNode
     *            Corresponding plug-in node of the given filter.
     * @param filterContainer
     *            Filter container whose information have to be added to the
     *            internal XML document.
     */
    private void addFilterToXML(Node pluginNode, FilterContainer filterContainer) {

        NodeList filters = ((Element) pluginNode).getElementsByTagName(NODES_FILTERS);
        Node filtersNode;

        if (filters.getLength() == 1) {
            filtersNode = filters.item(0);
        } else {
            filtersNode = xmlDocument.createElement(NODES_FILTERS);
            pluginNode.appendChild(filtersNode);
        }

        Node filterNode = xmlDocument.createElement(NODES_FILTER);
        Node nameNode = xmlDocument.createElement(NODES_FILTERNAME);
        Node activatedNode = xmlDocument.createElement(NODES_FILTERACTIVATED);
        Node positionNode = xmlDocument.createElement(NODES_FILTERPOSITION);

        filtersNode.appendChild(filterNode);
        filterNode.appendChild(nameNode);
        filterNode.appendChild(activatedNode);
        filterNode.appendChild(positionNode);

        nameNode.appendChild(xmlDocument.createTextNode(filterContainer.getFilterClass().getName()));
        activatedNode.appendChild(xmlDocument.createTextNode(Boolean.toString(filterContainer.isActive())));
        positionNode.appendChild(xmlDocument.createTextNode(Integer.toString(filterContainer.getPosition())));
    }

    /**
     * Updates the information of a passed filter to the internal XML document.
     * 
     * @param filterNode
     *            Corresponding filter node of the given filter.
     * @param filterContainer
     *            Filter container whose information have to be updated in the
     *            internal XML document.
     */
    private void editFilterInXML(Node filterNode, FilterContainer filterContainer) {

        NodeList list = ((Element) filterNode).getElementsByTagName(NODES_FILTERACTIVATED);

        if (list.getLength() == 1) {
            Node textNode = list.item(0).getFirstChild();
            textNode.setNodeValue(Boolean.toString(filterContainer.isActive()));
        }

        list = ((Element) filterNode).getElementsByTagName(NODES_FILTERPOSITION);

        if (list.getLength() == 1) {
            Node textNode = list.item(0).getFirstChild();
            textNode.setNodeValue(Integer.toString(filterContainer.getPosition()));
        }
    }

    /**
     * Adds or updates the information of a passed overlay to the internal XML
     * document. This method does not save the XML document to the corresponding
     * settings file!
     * 
     * @param pluginLocation
     *            location of the plug-in where the filter comes from.
     * @param overlayContainer
     *            Filter container whose information have to be updated in the
     *            internal XML document.
     * @see #savePluginSettings()
     */
    public void overlaySettingsToXML(URI pluginLocation, OverlayContainer overlayContainer) {

        if (pluginLocation == null)
            return;

        Node pluginNode = findNode(pluginsRootNode, "PluginLocation", pluginLocation.getPath());

        if (pluginNode == null)
            return;

        Node overlayNode = findNode(pluginNode, NODES_OVERLAYNAME, overlayContainer.getOverlayClass().getName());

        if (overlayNode == null) {
            addOverlayToXML(pluginNode, overlayContainer);
        } else {
            editOverlayInXML(overlayNode, overlayContainer);
        }
    }

    /**
     * Adds the information of a overlay to the XML document.
     * 
     * @param pluginNode
     *            Corresponding plug-in node of the given overlay.
     * @param overlayContainer
     *            Overlay container whose information have to be added to the
     *            internal XML document.
     */
    private void addOverlayToXML(Node pluginNode, OverlayContainer overlayContainer) {

        NodeList overlays = ((Element) pluginNode).getElementsByTagName(NODES_OVERLAYS);
        Node overlaysNode;

        if (overlays.getLength() == 1) {
            overlaysNode = overlays.item(0);
        } else {
            overlaysNode = xmlDocument.createElement(NODES_OVERLAYS);
            pluginNode.appendChild(overlaysNode);
        }

        Node overlayNode = xmlDocument.createElement(NODES_OVERLAY);
        Node nameNode = xmlDocument.createElement(NODES_OVERLAYNAME);
        Node activatedNode = xmlDocument.createElement(NODES_OVERLAYACTIVATED);
        Node positionNode = xmlDocument.createElement(NODES_OVERLAYPOSITION);

        overlaysNode.appendChild(overlayNode);
        overlayNode.appendChild(nameNode);
        overlayNode.appendChild(activatedNode);
        overlayNode.appendChild(positionNode);

        nameNode.appendChild(xmlDocument.createTextNode(overlayContainer.getOverlayClass().getName()));
        activatedNode.appendChild(xmlDocument.createTextNode(Boolean.toString(overlayContainer.isActive())));
        positionNode.appendChild(xmlDocument.createTextNode(Integer.toString(overlayContainer.getPosition())));
    }

    /**
     * Updates the information of a passed overlay to the internal XML document.
     * 
     * @param overlayNode
     *            Corresponding overlay node of the given overlay.
     * @param overlayContainer
     *            Overlay container whose information have to be updated in the
     *            internal XML document.
     */
    private void editOverlayInXML(Node overlayNode, OverlayContainer overlayContainer) {

        NodeList list = ((Element) overlayNode).getElementsByTagName(NODES_OVERLAYACTIVATED);

        if (list.getLength() == 1) {
            Node textNode = list.item(0).getFirstChild();
            textNode.setNodeValue(Boolean.toString(overlayContainer.isActive()));
        }

        list = ((Element) overlayNode).getElementsByTagName(NODES_OVERLAYPOSITION);

        if (list.getLength() == 1) {
            Node textNode = list.item(0).getFirstChild();
            textNode.setNodeValue(Integer.toString(overlayContainer.getPosition()));
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
     * Checks the activated value of a given filter in the XML document. If
     * there is no entry in the XML document the return value is the passed
     * default value.
     * 
     * @param pluginLocation
     *            location of the file where the plug-in comes from.
     * @param filterClass
     *            Class of the filter which has to be checked in the XML
     *            document.
     * @param defaultValue
     *            value which has to be returned if there is no entry for the
     *            given filter in the XML document.
     * @return boolean value as it is in the XML document or the passed default
     *         value if no entry exists.
     */
    public boolean isFilterInPluginActivated(URI pluginLocation, Class<? extends Filter> filterClass, boolean defaultValue) {

        Node pluginNode = findNode(pluginsRootNode, NODES_PLUGINLOCATION, pluginLocation.getPath());

        if (pluginNode == null)
            return defaultValue;

        if (isActivated(pluginNode, NODES_PLUGINACTIVATED)) {

            Node filterNode = findNode(pluginNode, NODES_FILTERNAME, filterClass.getName());
            if (filterNode != null)
                return isActivated(filterNode, NODES_FILTERACTIVATED);
        }

        return defaultValue;
    }

    /**
     * Checks the activated value of a given overlay in the XML document. If
     * there is no entry in the XML document the return value is the passed
     * default value.
     * 
     * @param pluginLocation
     *            location of the file where the plug-in comes from.
     * @param overlayClass
     *            Class of the overlay which has to be checked in the XML
     *            document.
     * @param defaultValue
     *            value which has to be returned if there is no entry for the
     *            given overlay in the XML document.
     * @return boolean value as it is in the XML document or the passed default
     *         value if no entry exists.
     */
    public boolean isOverlayInPluginActivated(URI pluginLocation, Class<? extends PhysicalRenderer> overlayClass, boolean defaultValue) {

        Node pluginNode = findNode(pluginsRootNode, NODES_PLUGINLOCATION, pluginLocation.getPath());

        if (pluginNode == null)
            return defaultValue;

        if (isActivated(pluginNode, NODES_PLUGINACTIVATED)) {

            Node overlayNode = findNode(pluginNode, NODES_OVERLAYNAME, overlayClass.getName());
            if (overlayNode != null)
                return isActivated(overlayNode, NODES_OVERLAYACTIVATED);
        }

        return defaultValue;
    }

    /**
     * Reads the saved position of a given filter from the XML document.
     * 
     * @param pluginLocation
     *            location of the file where the plug-in comes from.
     * @param filterClass
     *            Class of the filter which has to be checked in the XML
     *            document.
     * @return The position of the filter or -1 if there is no entry for the
     *         given filter.
     */
    public int getFilterPosition(URI pluginLocation, Class<? extends Filter> filterClass) {

        Node pluginNode = findNode(pluginsRootNode, NODES_PLUGINLOCATION, pluginLocation.getPath());

        if (pluginNode == null)
            return -1;

        if (isActivated(pluginNode, NODES_PLUGINACTIVATED)) {

            Node filterNode = findNode(pluginNode, NODES_FILTERNAME, filterClass.getName());
            if (filterNode != null) {
                NodeList list = ((Element) filterNode).getElementsByTagName(NODES_FILTERPOSITION);

                if (list.getLength() == 1) {
                    Node child = list.item(0).getFirstChild();
                    if (child != null && child.getNodeType() == Node.TEXT_NODE) {
                        try {
                            return Integer.parseInt(child.getNodeValue());
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Reads the saved position of a given overlay from the XML document.
     * 
     * @param pluginLocation
     *            location of the file where the plug-in comes from.
     * @param overlayClass
     *            Class of the overlay which has to be checked in the XML
     *            document.
     * @return The position of the overlay or -1 if there is no entry for the
     *         given overlay.
     */
    public int getOverlayPosition(URI pluginLocation, Class<? extends PhysicalRenderer> overlayClass) {

        Node pluginNode = findNode(pluginsRootNode, NODES_PLUGINLOCATION, pluginLocation.getPath());

        if (pluginNode == null)
            return -1;

        if (isActivated(pluginNode, NODES_PLUGINACTIVATED)) {

            Node overlayNode = findNode(pluginNode, NODES_OVERLAYNAME, overlayClass.getName());
            if (overlayNode != null) {
                NodeList list = ((Element) overlayNode).getElementsByTagName(NODES_OVERLAYPOSITION);

                if (list.getLength() == 1) {
                    Node child = list.item(0).getFirstChild();
                    if (child != null && child.getNodeType() == Node.TEXT_NODE) {
                        try {
                            return Integer.parseInt(child.getNodeValue());
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
        }

        return -1;
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
