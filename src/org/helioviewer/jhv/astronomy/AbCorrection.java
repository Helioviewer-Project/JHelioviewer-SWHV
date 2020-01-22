package org.helioviewer.jhv.astronomy;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import spice.basic.AberrationCorrection;
import spice.basic.SpiceErrorException;

public enum AbCorrection {
    NONE("NONE"), LT("LT"), LTS("LT+S"), XLT("XLT"), XLTS("XLT+S");

    public final String code;
    public final AberrationCorrection correction;

    AbCorrection(String abcorr) {
        code = URLEncoder.encode(abcorr, StandardCharsets.UTF_8);
        try {
            correction = new AberrationCorrection(abcorr);
        } catch (SpiceErrorException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

}
