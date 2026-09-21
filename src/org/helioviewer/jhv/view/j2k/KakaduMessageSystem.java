package org.helioviewer.jhv.view.j2k;

import org.helioviewer.jhv.app.Log;

import kdu_jni.KduException;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message;

// This class takes care of setting up the internal Kakadu messaging objects.
public class KakaduMessageSystem {

    private static class MessageHandler extends Kdu_message {
        private final boolean error;
        private final ThreadLocal<StringBuilder> message = ThreadLocal.withInitial(StringBuilder::new);

        MessageHandler(boolean _error) {
            error = _error;
        }

        @Override
        public void Start_message() {
            message.get().setLength(0);
        }

        @Override
        public void Put_text(String text) {
            message.get().append(text);
        }

        @Override
        public void Flush(boolean endOfMessage) throws KduException {
            if (!endOfMessage)
                return;

            String text = message.get().toString().stripTrailing();
            message.remove();
            if (error)
                Log.error(text);
            else
                Log.warn(text);

            if (error)
                throw new KduException(Kdu_global.KDU_ERROR_EXCEPTION, text);
        }
    }

    // Kakadu retains native references to these callback objects.
    @SuppressWarnings("FieldCanBeLocal")
    private static MessageHandler warnings, errors;

    // Attempts to set up the Kakadu message handlers. Honestly, if this fails
    // then there is probably a larger problem, and it should error and exit.
    public static void startKduMessageSystem() throws Exception {
        try {
            warnings = new MessageHandler(false);
            errors = new MessageHandler(true);
            Kdu_global.Kdu_customize_warnings(warnings);
            Kdu_global.Kdu_customize_errors(errors);
        } catch (KduException e) {
            throw new Exception("Error initializing Kakadu error handler: " + e.getMessage(), e);
        }
    }

}
