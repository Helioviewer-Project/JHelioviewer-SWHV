package org.helioviewer.jhv;

import kdu_jni.KduException;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message_formatter;

import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_Kdu_message;

/**
 * This class takes care of setting up the internal Kakadu messaging objects.
 * 
 * @author caplins
 */
public class KakaduEngine {

    /**
     * Static instances of KduSysMessage for both errors and warnings. Although
     * never explicitly used, the references must be maintained since the native
     * code calls back to this.
     */
    private static JHV_Kdu_message warnings, errors;

    /**
     * Static instances of Kdu_message_formatter for both errors and warnings.
     * Although never explicitly used, the references must be maintained since
     * the native code calls back to this.
     */
    private static Kdu_message_formatter warningsFormatter, errorsFormatter;

    /**
     * Attempts to setup the Kakadu message handlers. Honestly, if this fails
     * then there is probably a larger problem and it should error and exit.
     * 
     * @throws JHV_KduException
     */
    public void startKduMessageSystem() throws JHV_KduException {
        try {
            warnings = new JHV_Kdu_message(false);
            errors = new JHV_Kdu_message(true);
            warningsFormatter = new Kdu_message_formatter(warnings, 80);
            errorsFormatter = new Kdu_message_formatter(errors, 80);
            Kdu_global.Kdu_customize_warnings(warningsFormatter);
            Kdu_global.Kdu_customize_errors(errorsFormatter);
        } catch (KduException ex) {
            throw new JHV_KduException("Error initializing Kakadu error handler:\n" + ex.getMessage());
        }
    }

};