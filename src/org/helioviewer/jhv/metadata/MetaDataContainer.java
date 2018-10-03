package org.helioviewer.jhv.metadata;

import java.util.Optional;

interface MetaDataContainer {

    Optional<String> getString(String key);

    Optional<Long> getLong(String key);

    Optional<Double> getDouble(String key);

    String getRequiredString(String key);

    long getRequiredLong(String key);

    double getRequiredDouble(String key);

}
