package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

public class JPIPQuery {

    public static String create(int len, String... values) {
        boolean isKey = true;
        StringBuilder buf = new StringBuilder();
        for (String val : values) {
            buf.append(val);
            if (isKey)
                buf.append('=');
            else
                buf.append('&');
            isKey = !isKey;
        }
        return buf.toString() + "len=" + len;
    }

}
