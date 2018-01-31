package org.helioviewer.jhv.metadata;

import java.util.Optional;

import org.helioviewer.jhv.base.XMLUtils;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

public class XMLMetaDataContainer implements MetaDataContainer {

    private Element meta;

    public void parseXML(String xml) throws Exception {
        meta = (Element) XMLUtils.parse(xml).getElementsByTagName("meta").item(0);
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

    @Override
    public String getRequiredString(String key) {
        return getString(key).orElseThrow(() -> new MetaDataException(key));
    }

    @Override
    public int getRequiredInteger(String key) {
        return getInteger(key).orElseThrow(() -> new MetaDataException(key));
    }

    @Override
    public double getRequiredDouble(String key) {
        return getDouble(key).orElseThrow(() -> new MetaDataException(key));
    }

}
