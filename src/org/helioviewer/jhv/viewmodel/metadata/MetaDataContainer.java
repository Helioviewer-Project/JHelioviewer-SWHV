package org.helioviewer.jhv.viewmodel.metadata;

import java.util.Optional;

public interface MetaDataContainer {

    String get(String key);

    double tryGetDouble(String key);

    Optional<String> getString(String key);

    Optional<Integer> getInteger(String key);

    Optional<Double> getDouble(String key);

}
