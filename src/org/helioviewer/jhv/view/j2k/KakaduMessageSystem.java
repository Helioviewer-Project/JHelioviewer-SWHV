package org.helioviewer.jhv.view.j2k;

import java.util.concurrent.locks.ReentrantLock;

import org.helioviewer.jhv.app.Log;

import kdu_jni.KduException;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message;
import kdu_jni.Kdu_message_formatter;

// This class takes care of setting up the internal Kakadu messaging objects.
public class KakaduMessageSystem {

    // This class allows printing Kakadu error messages, throwing Java exceptions if necessary.
    private static class JHV_Kdu_message extends Kdu_message {
        private final boolean error;
        private final ReentrantLock lock = new ReentrantLock();
        private final StringBuilder message = new StringBuilder();

        JHV_Kdu_message(boolean _error) {
            error = _error;
        }

        @Override
        public void Start_message() {
            // Kakadu shares each formatter across its worker threads. Keep all
            // fragments of one message together until the terminal flush.
            lock.lock();
            message.setLength(0);
        }

        @Override
        public void Put_text(String text) {
            message.append(text);
        }

        @Override
        public void Flush(boolean endOfMessage) throws KduException {
            if (!endOfMessage)
                return;

            String text = message.toString().stripTrailing();
            try {
                if (error)
                    Log.error(text);
                else
                    Log.warn(text);
            } finally {
                lock.unlock();
            }

            if (error)
                throw new KduException(Kdu_global.KDU_ERROR_EXCEPTION, text);
        }
    }

    // Static instances of KduSysMessage for both errors and warnings. Although
    // never explicitly used, the references must be maintained since the native
    // code calls back to this.
    @SuppressWarnings("FieldCanBeLocal")
    private static JHV_Kdu_message warnings, errors;

    // Static instances of Kdu_message_formatter for both errors and warnings.
    // Although never explicitly used, the references must be maintained since
    // the native code calls back to this.
    @SuppressWarnings("FieldCanBeLocal")
    private static Kdu_message_formatter warningsFormatter, errorsFormatter;

    // Attempts to set up the Kakadu message handlers. Honestly, if this fails
    // then there is probably a larger problem, and it should error and exit.
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
