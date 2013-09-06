package org.helioviewer.viewmodel.view.jp2view.kakadu;

import kdu_jni.KduException;
import kdu_jni.Kdu_message;

/**
 * This class allows to print Kakadu error messages, throwing Java exceptions if
 * it is necessary.
 */
public class JHV_Kdu_message extends Kdu_message {

    private boolean raiseException;

    public JHV_Kdu_message(boolean raiseException) {
        this.raiseException = raiseException;
    }

    public void Flush(boolean endOfMessage) throws KduException {
        if (endOfMessage && raiseException)
            throw new KduException("Kakadu message error");
    }
}
