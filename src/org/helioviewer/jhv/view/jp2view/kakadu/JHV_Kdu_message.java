package org.helioviewer.jhv.view.jp2view.kakadu;

import kdu_jni.KduException;
import kdu_jni.Kdu_message;

/**
 * This class allows to print Kakadu error messages, throwing Java exceptions if
 * it is necessary.
 */
class JHV_Kdu_message extends Kdu_message {

    private final boolean raiseException;

    public JHV_Kdu_message(boolean _raiseException) {
        raiseException = _raiseException;
    }

/*  Only for debug, may crash
    @Override
    public void Put_text(String text) {
        System.out.print(text);
    }
*/

    @Override
    public void Flush(boolean endOfMessage) throws KduException {
        if (endOfMessage && raiseException)
            throw new KduException("Kakadu message error");
    }

}
