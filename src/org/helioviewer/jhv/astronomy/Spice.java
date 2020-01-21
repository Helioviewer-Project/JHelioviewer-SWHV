package org.helioviewer.jhv.astronomy;

import java.util.concurrent.Callable;

import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.SingleExecutor;
import org.helioviewer.jhv.threads.JHVThread;

import spice.basic.KernelDatabase;
import spice.basic.SpiceErrorException;
import spice.basic.TDBTime;

public class Spice extends Thread {

    private static final SingleExecutor executor = new SingleExecutor(new JHVThread.NamedClassThreadFactory(Spice.class, "SPICE"));

    public Spice(Runnable r, String name) {
        super(r, name);
    }

    public static void loadKernel(String file) {
        executor.invokeLater(new LoadKernel(file));
    }

    private static class LoadKernel implements Runnable {

        private final String file;

        LoadKernel(String _file) {
            file = _file;
        }

        @Override
        public void run() {
            try {
                KernelDatabase.load(file);
            } catch (Exception e) {
                Log.error(e);
            }
        }

    }

    public static String dateParse2UTC(String date) {
        try {
            return executor.invokeAndWait(new DateParse2UTC(date));
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    private static class DateParse2UTC implements Callable<String> {

        private final String date;

        DateParse2UTC(String _date) {
            date = _date;
        }

        @Override
        public String call() throws SpiceErrorException {
            return new TDBTime(date).toUTCString("isoc", 0);
        }

    }


}
