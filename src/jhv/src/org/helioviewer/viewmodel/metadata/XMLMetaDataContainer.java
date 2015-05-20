package org.helioviewer.viewmodel.metadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.helioviewer.base.logging.Log;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLMetaDataContainer implements MetaDataContainer {

    private NodeList nodeList;

    public void parseXML(String xml) throws Exception {
        if (xml == null)
            throw new Exception("No XML data present");
        else if (!xml.contains("</meta>")) {
            throw new Exception("XML data incomplete");
        }

        try {
            InputStream in = new ByteArrayInputStream(xml.trim().replace("&", "&amp;").getBytes("UTF-8"));
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            nodeList = builder.parse(in).getElementsByTagName("meta");
        } catch (Exception e) {
            throw new Exception("Failed parsing XML data", e);
        }
    }

    public void destroyXML() {
        nodeList = null;
    }

    private String getValueFromXML(String _keyword) throws Exception {
        try {
            NodeList value = ((Element) nodeList.item(0)).getElementsByTagName(_keyword);
            Element line = (Element) value.item(0);

            if (line == null)
                return null;

            Node child = line.getFirstChild();
            if (child instanceof CharacterData) {
                CharacterData cd = (CharacterData) child;
                return cd.getData();
            }
            return null;
        } catch (Exception e) {
            throw new Exception("Failed parsing XML data", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String get(String key) {
        try {
            return getValueFromXML(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double tryGetDouble(String key) {
        String string = get(key);
        if (string != null) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException e) {
                Log.warn("NumberFormatException while trying to parse value \"" + string + "\" of key " + key);
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int tryGetInt(String key) {
        String string = get(key);
        if (string != null) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                Log.warn("NumberFormatException while trying to parse value \"" + string + "\" of key " + key);
                return 0;
            }
        }
        return 0;
    }

}
