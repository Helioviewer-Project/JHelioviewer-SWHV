package org.helioviewer.jhv.metadata;

import java.util.Optional;

interface MetaDataContainer {

    Optional<String> getString(String key);

    Optional<Integer> getInteger(String key);

    Optional<Double> getDouble(String key);

    String getRequiredString(String key);

    int getRequiredInteger(String key);

    double getRequiredDouble(String key);

}
