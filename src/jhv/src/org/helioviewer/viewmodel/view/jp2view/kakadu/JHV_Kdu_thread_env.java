package org.helioviewer.viewmodel.view.jp2view.kakadu;

import kdu_jni.KduException;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_thread_env;

public class JHV_Kdu_thread_env {

    private static Kdu_thread_env threadEnv;

    public static Kdu_thread_env getThreadEnv() throws KduException {
        if (threadEnv == null) {
            int numThreads = Kdu_global.Kdu_get_num_processors();
            threadEnv = new Kdu_thread_env();
            threadEnv.Create();
            for (int i = 1; i < numThreads; i++)
                threadEnv.Add_thread();
        }
        return threadEnv;
    }

}
