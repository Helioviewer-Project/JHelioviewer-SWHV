package org.helioviewer.jhv.metadata;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.base.XMLUtils;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;

public class XMLMetaDataContainer implements MetaDataContainer {

    private final Element meta;

    public XMLMetaDataContainer(String xml) throws Exception {
        meta = (Element) XMLUtils.parse(xml).getElementsByTagName("meta").item(0);
        if (meta == null)
            throw new Exception("XML metadata without meta tag");
    }

    @Nullable
    private String getValueFromXML(String key) {
        Element line = (Element) meta.getElementsByTagName(key).item(0);
        if (line == null)
            return null;

        Node child = line.getFirstChild();
        if (child instanceof CharacterData)
            return ((CharacterData) child).getData();
        return null;
    }

    public HelioviewerMetaData getHVMetaData(int i, boolean normalizeResponse) {
        return new HelioviewerMetaData(this, i, normalizeResponse);
    }

    @Nonnull
    @Override
    public Optional<String> getString(String key) {
        return Optional.ofNullable(getValueFromXML(key));
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
