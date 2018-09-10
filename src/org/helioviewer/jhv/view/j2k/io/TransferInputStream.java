package org.helioviewer.jhv.view.j2k.io;

import java.io.InputStream;

public abstract class TransferInputStream extends InputStream {

    public abstract int getTotalLength();

}
