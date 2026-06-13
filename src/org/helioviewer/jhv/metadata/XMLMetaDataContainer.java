package org.helioviewer.jhv.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.io.XMLUtils;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;

public class XMLMetaDataContainer implements MetaDataContainer {

    private final Map<String, String> values = new HashMap<>();

    public XMLMetaDataContainer(String xml) throws Exception {
        Element meta = (Element) XMLUtils.parse(xml).getElementsByTagName("meta").item(0);
        if (meta == null)
            throw new Exception("XML metadata without meta tag");

        NodeList nodes = meta.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            Node child = element.getFirstChild();
            if (child instanceof CharacterData cd)
                values.putIfAbsent(element.getNodeName(), cd.getData());
        }
    }

    @Nonnull
    @Override
    public Optional<String> getString(String key) {
        return Optional.ofNullable(values.get(key));
    }

    @Nonnull
    @Override
    public Optional<Long> getLong(String key) {
        return getString(key).map(Longs::tryParse);
    }

    @Nonnull
    @Override
    public Optional<Double> getDouble(String key) {
        return getString(key).map(Doubles::tryParse);
    }

    @Nonnull
    @Override
    public String getRequiredString(String key) {
        return getString(key).orElseThrow(() -> new RuntimeException(key + " not found in metadata"));
    }

    @Override
    public long getRequiredLong(String key) {
        return getLong(key).orElseThrow(() -> new RuntimeException(key + " not found in metadata"));
    }

    @Override
    public double getRequiredDouble(String key) {
        return getDouble(key).orElseThrow(() -> new RuntimeException(key + " not found in metadata"));
    }

}
