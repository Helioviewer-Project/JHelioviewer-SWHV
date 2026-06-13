package org.helioviewer.jhv.app;

public class Message {

    public interface Handler {
        void err(String title, Object msg);

        void warn(String title, Object msg);

        void fatalErr(String msg);
    }

    private static Handler handler = new ConsoleHandler();

    public static void setHandler(Handler _handler) {
        handler = _handler;
    }

    public static void err(String title, Object msg) {
        handler.err(title, msg);
    }

    public static void warn(String title, Object msg) {
        handler.warn(title, msg);
    }

    public static void fatalErr(String msg) {
        handler.fatalErr(msg);
        System.exit(-1);
    }

    public static String format(Object msg) {
        return msg == null || msg.toString().isEmpty() ? "No details available." : msg.toString();
    }

    private static final class ConsoleHandler implements Handler {
        @Override
        public void err(String title, Object msg) {
            System.err.println(title + ": " + format(msg));
        }

        @Override
        public void warn(String title, Object msg) {
            System.err.println(title + ": " + format(msg));
        }

        @Override
        public void fatalErr(String msg) {
            System.err.println("Fatal Error: " + format(msg));
        }
    }

}
