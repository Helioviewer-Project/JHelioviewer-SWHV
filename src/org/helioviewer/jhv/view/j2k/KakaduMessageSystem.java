package org.helioviewer.jhv.view.j2k;

import kdu_jni.KduException;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message_formatter;
import kdu_jni.Kdu_message_queue;

// This class takes care of setting up the internal Kakadu messaging objects
public class KakaduMessageSystem {

    // This class allows printing Kakadu error messages, throwing Java exceptions if necessary
    private static class JHV_Kdu_message extends Kdu_message_queue {

        JHV_Kdu_message(boolean throwExceptions) throws KduException {
            Configure(1, true, throwExceptions, Kdu_global.KDU_ERROR_EXCEPTION);
        }

    }

    /*
     * Static instances of KduSysMessage for both errors and warnings. Although
     * never explicitly used, the references must be maintained since the native
     * code calls back to this.
     */
    private static JHV_Kdu_message warnings, errors;

    /*
     * Static instances of Kdu_message_formatter for both errors and warnings.
     * Although never explicitly used, the references must be maintained since
     * the native code calls back to this.
     */
    private static Kdu_message_formatter warningsFormatter, errorsFormatter;

    /*
     * Attempts to set up the Kakadu message handlers. Honestly, if this fails
     * then there is probably a larger problem, and it should error and exit.
     */
    public static void startKduMessageSystem() throws Exception {
        try {
            warnings = new JHV_Kdu_message(false);
            errors = new JHV_Kdu_message(true);
            warningsFormatter = new Kdu_message_formatter(warnings, 80);
            errorsFormatter = new Kdu_message_formatter(errors, 80);
            Kdu_global.Kdu_customize_warnings(warningsFormatter);
            Kdu_global.Kdu_customize_errors(errorsFormatter);
        } catch (KduException e) {
            throw new Exception("Error initializing Kakadu error handler: " + e.getMessage(), e);
        }
    }

}
