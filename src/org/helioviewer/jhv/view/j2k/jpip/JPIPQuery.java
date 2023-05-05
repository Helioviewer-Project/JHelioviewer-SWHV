package org.helioviewer.jhv.view.j2k.jpip;

public class JPIPQuery {

    public static String create(int len, String... values) {
        boolean isKey = true;
        StringBuilder buf = new StringBuilder();
        for (String val : values) {
            buf.append(val);
            buf.append(isKey ? '=' : '&');
            isKey = !isKey;
        }
        return buf + "len=" + len;
    }

}
