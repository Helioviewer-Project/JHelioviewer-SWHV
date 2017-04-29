package org.helioviewer.jhv.viewmodel.metadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.helioviewer.jhv.base.logging.Log;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

public class XMLMetaDataContainer implements MetaDataContainer {

    Element meta;

    public void parseXML(String xml) throws Exception {
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            meta = (Element) builder.parse(in).getElementsByTagName("meta").item(0);
        } catch (Exception e) {
            throw new Exception("XML metadata parse failure: ", e);
        }

        if (meta == null)
            throw new Exception("XML metadata without meta tag");
    }

    public void destroyXML() {
        meta = null;
    }

    private String getValueFromXML(String key) {
        Element line = (Element) meta.getElementsByTagName(key).item(0);
        if (line == null)
            return null;

        Node child = line.getFirstChild();
        if (child instanceof CharacterData)
            return ((CharacterData) child).getData();
        return null;
    }

    @Override
    public String get(String key) {
        return getValueFromXML(key);
    }

    @Override
    public double tryGetDouble(String key) {
        String string = get(key);
        if (string != null) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException e) {
                Log.warn("NumberFormatException while trying to parse value \"" + string + "\" of key " + key);
                return Double.NaN;
            }
        }
        return 0.0;
    }

    @Override
    public int tryGetInt(String key) {
        String string = get(key);
        if (string != null) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                Log.warn("NumberFormatException while trying to parse value \"" + string + "\" of key " + key);
                return Integer.MIN_VALUE;
            }
        }
        return 0;
    }

    @Override
    public Optional<String> getString(String key) {
        return Optional.ofNullable(getValueFromXML(key));
    }

    @Override
    public Optional<Integer> getInteger(String key) {
        return getString(key).map(Ints::tryParse);
    }

    @Override
    public Optional<Double> getDouble(String key) {
        return getString(key).map(Doubles::tryParse);
    }

}
