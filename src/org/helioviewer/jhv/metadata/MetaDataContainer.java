package org.helioviewer.jhv.metadata;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

interface MetaDataContainer {

    Set<String> keys();

    @Nonnull
    Optional<String> getString(String key);

    @Nonnull
    Optional<Long> getLong(String key);

    @Nonnull
    Optional<Double> getDouble(String key);

    @Nonnull
    String getRequiredString(String key);

    long getRequiredLong(String key);

    double getRequiredDouble(String key);

}
