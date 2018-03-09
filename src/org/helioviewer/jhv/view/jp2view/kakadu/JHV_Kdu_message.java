package org.helioviewer.jhv.view.jp2view.kakadu;

import kdu_jni.KduException;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message_queue;

// This class allows to print Kakadu error messages, throwing Java exceptions if necessary.
class JHV_Kdu_message extends Kdu_message_queue {

    public JHV_Kdu_message(boolean throwExceptions) throws KduException {
        Configure(1, true, throwExceptions, Kdu_global.KDU_ERROR_EXCEPTION);
    }

}
