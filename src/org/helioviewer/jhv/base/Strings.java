package org.helioviewer.jhv.base;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

public class Strings {

    private static final Interner<String> interner = Interners.newWeakInterner();

    public static String intern(String str) {
        return interner.intern(str);
    }

}
