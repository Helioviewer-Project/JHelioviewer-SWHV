package org.helioviewer.jhv.astronomy;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

enum AbCorrection {
    NONE("NONE"), LT("LT"), LTS("LT+S"), XLT("XLT"), XLTS("XLT+S");

    private final String corr;
    public final String code;

    AbCorrection(String _corr) {
        corr = _corr;
        code = URLEncoder.encode(corr, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return corr;
    }

}
