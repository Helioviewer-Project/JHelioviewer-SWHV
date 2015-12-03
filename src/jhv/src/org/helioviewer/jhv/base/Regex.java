package org.helioviewer.jhv.base;

import java.util.regex.Pattern;

public class Regex {

    public static final Pattern HTTPpattern = Pattern.compile("^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$");

}
